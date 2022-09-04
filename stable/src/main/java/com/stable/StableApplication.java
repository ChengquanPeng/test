package com.stable;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.annotation.ImportResource;

import com.stable.utils.OSystemUtil;

import lombok.extern.log4j.Log4j2;

@ImportResource(locations = { "classpath:jobs.xml" })
@SpringBootApplication
@Log4j2
public class StableApplication extends SpringBootServletInitializer {
	private static void init() {
		System.setProperty("es.set.netty.runtime.available.processors", "false");
		if (OSystemUtil.isWindows()) {
			log.info("OS System is Windows");
		} else {
			log.info("OS System is Linux");
		}
	}

	@Override
	protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
		init();
		return application.sources(StableApplication.class);
	}

	public static void main(String[] args) {
		init();
		new SpringApplicationBuilder(StableApplication.class).headless(false).run(args);
		// SpringApplication.run(StableApplication.class, args);
	}

}
