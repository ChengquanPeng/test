package com.stable.job;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.dangdang.ddframe.job.api.ShardingContext;
import com.dangdang.ddframe.job.api.simple.SimpleJob;
import com.stable.service.model.CodeModelService;
import com.stable.utils.OSystemUtil;
import com.stable.utils.WxPushUtil;

/**
 * 这是我们的测试job类
 * 
 * @author
 * @date 2018/10/25.
 */
@Component
public class SysStatusJob implements SimpleJob {

	@Autowired
	private CodeModelService codeModelService;

	@Override
	public void execute(ShardingContext sc) {
		try {
			codeModelService.getLastOneByCode2("000001");
		} catch (Exception e) {
			e.printStackTrace();
			WxPushUtil.pushSystem1("系统异常，正在重启...");
			OSystemUtil.restart();
		}
	}
}
