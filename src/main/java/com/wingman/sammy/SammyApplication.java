package com.wingman.sammy;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(MediaProperties.class)
public class SammyApplication {

	public static void main(String[] args) {
		SpringApplication.run(SammyApplication.class, args);
	}

}
