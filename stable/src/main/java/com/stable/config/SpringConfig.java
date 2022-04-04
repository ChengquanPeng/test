package com.stable.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

import com.stable.utils.OSystemUtil;

import lombok.Getter;
import lombok.Setter;

@Configuration
@Order(1)
@Getter
@Setter
public class SpringConfig {
	@Value("${python.script.concurrency.num}")
	private int pythonconcurrencynum;

	@Value("${error.log.file}")
	private String filepath = "/my/free/error.log";

	@Value("${wx.push.app.token.system}")
	private String appToken;

	@Value("${wx.push.myuid}")
	private String myUid;

	@Value("${worker2.num}")
	private int worker2Num = 10;

	public final static boolean isWindows = OSystemUtil.isWindows();
}
