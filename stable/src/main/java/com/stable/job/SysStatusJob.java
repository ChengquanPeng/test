package com.stable.job;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.dangdang.ddframe.job.api.ShardingContext;
import com.dangdang.ddframe.job.api.simple.SimpleJob;
import com.stable.service.RunLogService;
import com.stable.utils.OSystemUtil;
import com.stable.utils.WxPushUtil;
import com.stable.vo.spi.req.EsQueryPageReq;

/**
 * 这是我们的测试job类
 * 
 * @author
 * @date 2018/10/25.
 */
@Component
public class SysStatusJob implements SimpleJob {

	@Autowired
	private RunLogService runLogService;
	EsQueryPageReq page = new EsQueryPageReq(1);

	@Override
	public void execute(ShardingContext sc) {
		try {
			runLogService.queryRunlogs(null, null, page);
		} catch (Exception e) {
			WxPushUtil.pushSystem1("系统异常，正在重启...");
			OSystemUtil.restart();
			e.printStackTrace();
		}
	}
}
