package com.stable.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

import lombok.Data;

@Configuration
@Order(1)
@Data
public class SpringConfig {

	@Value("${wx.push.app.token.system}")
	private String appToken;

	@Value("${wx.push.myuid}")
	private String myUid;

	@Value("${putHost}")
	private String putHost;
}
