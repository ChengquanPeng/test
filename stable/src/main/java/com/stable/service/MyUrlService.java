package com.stable.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class MyUrlService {

	@Value("${myurl.server.http}")
	private String serverhttp;
	@Value("${myurl.server.ip}")
	private String severip;
	@Value("${myurl.server.port}")
	private String serverport;

	private String httpserverurlheader = null;

	public String getHttpServer() {
		if (httpserverurlheader == null) {
			httpserverurlheader = serverhttp + "://" + severip + ":" + serverport + "/";
		}
		return httpserverurlheader;
	}

	// https://183.56.196.175:9999/html/code.jpg
	public String getUrl1_Pic(String code) {
		return getHttpServer() + "html/" + code + ".jpg";
	}

	// https://183.56.196.175:9999/web/admin/manual.html?code=002366
	public String getUrl2_manual(String code) {
		return getHttpServer() + "web/admin/manual.html?code=" + code;
	}
}
