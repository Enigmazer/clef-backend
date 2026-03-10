package com.enigmazer.clef;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ClefApplication {

	public static void main(String[] args) {
		SpringApplication.run(ClefApplication.class, args);
	}

}
