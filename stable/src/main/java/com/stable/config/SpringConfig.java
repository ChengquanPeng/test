package com.stable.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

@Configuration
@Order(1)
public class SpringConfig {

	// @Value("${python.script.concurrency.num}")
	// private int pythonconcurrencynum;

	@Value("${error.log.file}")
	private String filepath = "/my/free/error.log";

	@Value("${no.tickdata.log.folder}")
	private String notickdata = "/my/free/notickdata/";

	@Value("${wx.push.app.token.system}")
	private String appToken;

	@Value("${wx.push.myuid}")
	private String myUid;

	@Value("${worker2.num}")
	private int worker2Num = 10;

	@Value("${model.v1.sort.floder}")
	private String modelV1SortFloder;
	
	@Value("${model.image.floder}")
	private String modelImageFloder;

	public String getFilepath() {
		return filepath;
	}

	public void setFilepath(String filepath) {
		this.filepath = filepath;
	}

	public String getNotickdata() {
		return notickdata;
	}

	public void setNotickdata(String notickdata) {
		this.notickdata = notickdata;
	}

	public String getAppToken() {
		return appToken;
	}

	public void setAppToken(String appToken) {
		this.appToken = appToken;
	}

	public String getMyUid() {
		return myUid;
	}

	public void setMyUid(String myUid) {
		this.myUid = myUid;
	}

	public int getWorker2Num() {
		return worker2Num;
	}

	public void setWorker2Num(int worker2Num) {
		this.worker2Num = worker2Num;
	}

	public String getModelV1SortFloder() {
		return modelV1SortFloder;
	}

	public void setModelV1SortFloder(String modelV1SortFloder) {
		this.modelV1SortFloder = modelV1SortFloder;
	}

	public String getModelImageFloder() {
		return modelImageFloder;
	}

	public void setModelImageFloder(String modelImageFloder) {
		this.modelImageFloder = modelImageFloder;
	}

}
