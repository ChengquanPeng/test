package com.stable;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ImportResource;

@ImportResource(locations={"classpath:jobs.xml"})
@SpringBootApplication
public class StableApplication {

	public static void main(String[] args) {
		SpringApplication.run(StableApplication.class, args);
	}

}
