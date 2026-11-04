package com.example.peopleapi;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = "app.security.api-key=test-key")
class PeopleApiApplicationTests {

	@Test
	void contextLoads() {
	}

}
