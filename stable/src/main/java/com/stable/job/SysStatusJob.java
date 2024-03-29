package com.stable.job;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.dangdang.ddframe.job.api.ShardingContext;
import com.dangdang.ddframe.job.api.simple.SimpleJob;
import com.stable.service.model.WebModelService;
import com.stable.service.model.prd.msg.MsgPushServer;
import com.stable.utils.OSystemUtil;

/**
 * 这是我们的测试job类
 * 
 * @author
 * @date 2018/10/25.
 */
@Component
public class SysStatusJob implements SimpleJob {

	@Autowired
	private WebModelService modelWebService;

	@Override
	public void execute(ShardingContext sc) {
		try {
			modelWebService.getLastOneByCode2("000001");
		} catch (Exception e) {
			e.printStackTrace();
			MsgPushServer.pushToSystem("系统异常，正在重启...");
			OSystemUtil.restart();
		}
	}
}
