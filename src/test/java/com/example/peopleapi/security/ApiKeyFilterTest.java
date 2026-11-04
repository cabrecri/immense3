package com.example.peopleapi.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.servlet.HandlerExceptionResolver;

@ExtendWith(MockitoExtension.class)
class ApiKeyFilterTest {

    private static final String KEY = "secret-key";

    @Mock
    private HandlerExceptionResolver resolver;

    private ApiKeyFilter filter;
    private MockHttpServletResponse response;
    private MockFilterChain chain;

    @BeforeEach
    void setUp() {
        filter = new ApiKeyFilter(KEY, resolver);
        response = new MockHttpServletResponse();
        chain = new MockFilterChain();
    }

    private MockHttpServletRequest get(String uri) {
        return new MockHttpServletRequest("GET", uri);
    }

    @Test
    void validKey_continuesTheChain() throws Exception {
        MockHttpServletRequest request = get("/api/people");
        request.addHeader(ApiKeyFilter.HEADER, KEY);

        filter.doFilter(request, response, chain);

        assertThat(chain.getRequest()).isSameAs(request);
        verifyNoInteractions(resolver);
    }

    @Test
    void missingHeader_delegatesMissingApiKeyAndStopsTheChain() throws Exception {
        MockHttpServletRequest request = get("/api/people");

        filter.doFilter(request, response, chain);

        ArgumentCaptor<Exception> ex = ArgumentCaptor.forClass(Exception.class);
        verify(resolver).resolveException(any(), any(), isNull(), ex.capture());
        assertThat(ex.getValue()).isInstanceOf(MissingApiKeyException.class);
        assertThat(chain.getRequest()).isNull();
    }

    @Test
    void blankHeader_isTreatedAsMissing() throws Exception {
        MockHttpServletRequest request = get("/api/people/1");
        request.addHeader(ApiKeyFilter.HEADER, "   ");

        filter.doFilter(request, response, chain);

        ArgumentCaptor<Exception> ex = ArgumentCaptor.forClass(Exception.class);
        verify(resolver).resolveException(any(), any(), isNull(), ex.capture());
        assertThat(ex.getValue()).isInstanceOf(MissingApiKeyException.class);
        assertThat(chain.getRequest()).isNull();
    }

    @Test
    void wrongKey_delegatesInvalidApiKeyAndStopsTheChain() throws Exception {
        MockHttpServletRequest request = get("/api/people/page");
        request.addHeader(ApiKeyFilter.HEADER, "not-the-key");

        filter.doFilter(request, response, chain);

        ArgumentCaptor<Exception> ex = ArgumentCaptor.forClass(Exception.class);
        verify(resolver).resolveException(any(), any(), isNull(), ex.capture());
        assertThat(ex.getValue()).isInstanceOf(InvalidApiKeyException.class);
        assertThat(chain.getRequest()).isNull();
    }

    @Test
    void wrongKeyOfDifferentLength_isStillJustInvalid() throws Exception {
        MockHttpServletRequest request = get("/api/people");
        request.addHeader(ApiKeyFilter.HEADER, "x");

        filter.doFilter(request, response, chain);

        ArgumentCaptor<Exception> ex = ArgumentCaptor.forClass(Exception.class);
        verify(resolver).resolveException(any(), any(), isNull(), ex.capture());
        assertThat(ex.getValue()).isInstanceOf(InvalidApiKeyException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "/h2-console",
            "/h2-console/",
            "/h2-console/login.do",
            "/h2-console/stylesheet.css"
    })
    void h2Console_isNotFiltered(String uri) throws Exception {
        MockHttpServletRequest request = get(uri);

        filter.doFilter(request, response, chain);

        assertThat(chain.getRequest()).isSameAs(request);
        verifyNoInteractions(resolver);
    }

    @Test
    void h2ConsoleUnderContextPath_isNotFiltered() throws Exception {
        MockHttpServletRequest request = get("/app/h2-console/login.do");
        request.setContextPath("/app");

        filter.doFilter(request, response, chain);

        assertThat(chain.getRequest()).isSameAs(request);
        verifyNoInteractions(resolver);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "/api/people",
            "/api",
            "/api;x=1/people",
            "/%61pi/people",
            "//api/people",
            "/h2-consolex",
            "/anything-else",
            "/h2-console/../api/people",
            "/h2-console/./../api/people",
            "/h2-console/%2e%2e/api/people",
            "/h2-console/..;/api/people",
            "/h2-console;x=1/../api/people"
    })
    void everythingElse_requiresTheKey(String uri) throws Exception {
        MockHttpServletRequest request = get(uri);

        filter.doFilter(request, response, chain);

        ArgumentCaptor<Exception> ex = ArgumentCaptor.forClass(Exception.class);
        verify(resolver).resolveException(any(), any(), isNull(), ex.capture());
        assertThat(ex.getValue()).isInstanceOf(MissingApiKeyException.class);
        assertThat(chain.getRequest()).isNull();
    }

    @Test
    void blankConfiguredKey_failsFast() {
        assertThatThrownBy(() -> new ApiKeyFilter("  ", resolver))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("PEOPLE_API_KEY");
    }
}
