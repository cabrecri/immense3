package com.example.peopleapi.common;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.example.peopleapi.person.InvalidPageRequestException;
import com.example.peopleapi.person.PersonNotFoundException;
import com.example.peopleapi.security.InvalidApiKeyException;
import com.example.peopleapi.security.MissingApiKeyException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(PersonNotFoundException.class)
    public ProblemDetail handlePersonNotFound(PersonNotFoundException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setTitle("Person not found");
        return problem;
    }

    @ExceptionHandler(InvalidPageRequestException.class)
    public ProblemDetail handleInvalidPageRequest(InvalidPageRequestException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problem.setTitle("Invalid page request");
        return problem;
    }

    @ExceptionHandler(MissingApiKeyException.class)
    public ResponseEntity<ProblemDetail> handleMissingApiKey(MissingApiKeyException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, ex.getMessage());
        problem.setTitle("API key missing");
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .header(HttpHeaders.WWW_AUTHENTICATE, "ApiKey realm=\"people-api\"")
                .body(problem);
    }

    @ExceptionHandler(InvalidApiKeyException.class)
    public ProblemDetail handleInvalidApiKey(InvalidApiKeyException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, ex.getMessage());
        problem.setTitle("Invalid API key");
        return problem;
    }
}
