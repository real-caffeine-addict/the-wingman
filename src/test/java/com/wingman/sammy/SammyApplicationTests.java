package com.wingman.sammy;

import com.wingman.sammy.utilities.ConnectionManager;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
class SammyApplicationTests {

	@MockitoBean
	private ConnectionManager connectionManager;

	@Test
	void contextLoads() {
	}

}
