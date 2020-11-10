package com.stable.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

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

	@Value("${no.tickdata.log.folder}")
	private String notickdata = "/my/free/notickdata/";

	@Value("${wx.push.app.token.system}")
	private String appToken;

	@Value("${wx.push.myuid}")
	private String myUid;

	@Value("${wx.push.env}")
	private String env;

	@Value("${worker2.num}")
	private int worker2Num = 10;

	@Value("${model.v1.sort.floder}")
	private String modelV1SortFloder;

	@Value("${model.v1.sort.floder.desc}")
	private String modelV1SortFloderDesc;

	@Value("${model.image.floder}")
	private String modelImageFloder;

	@Value("${program.html.folder}")
	private String pubFloder;

}
