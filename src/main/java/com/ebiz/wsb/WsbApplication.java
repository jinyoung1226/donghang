package com.ebiz.wsb;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class WsbApplication {

	public static void main(String[] args) {
		SpringApplication.run(WsbApplication.class, args);
	}

}
