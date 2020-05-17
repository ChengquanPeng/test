package com.stable;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;

@SpringBootApplication
public class StableUtilApplication {

	public static void main(String[] args) {
		System.setProperty("es.set.netty.runtime.available.processors", "false");
		new SpringApplicationBuilder(StableUtilApplication.class).headless(false).run(args);
		// SpringApplication.run(StableApplication.class, args);
	}

}
