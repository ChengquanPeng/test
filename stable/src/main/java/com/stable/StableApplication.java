package com.stable;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.ImportResource;

@ImportResource(locations={"classpath:jobs.xml"})
@SpringBootApplication
public class StableApplication {

	public static void main(String[] args) {
		System.setProperty("es.set.netty.runtime.available.processors", "false");
	    SpringApplicationBuilder builder = new SpringApplicationBuilder(StableApplication.class);
	    builder.headless(false).run(args);
		//SpringApplication.run(StableApplication.class, args);
	}

}
