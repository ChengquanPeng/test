package com.stable.job;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.dangdang.ddframe.job.api.ShardingContext;
import com.stable.spider.eastmoney.EmAddIssueSpider;
import com.stable.spider.ths.ThsHolderSpider;
import com.stable.spider.ths.ThsJiejinSpider;

import lombok.extern.log4j.Log4j2;

/**
 * 周一到周五执行的任务 18:00
 * 
 */
@Component
@Log4j2
public class EveryDayMorningJob extends MySimpleJob {

	@Autowired
	private ThsJiejinSpider thsJiejinSpider;
	@Autowired
	private ThsHolderSpider thsHolderSpider;
	@Autowired
	private EmAddIssueSpider emAddIssueSpider;

	@Override
	public void myexecute(ShardingContext sc) {
		log.info("每日股东人数任务开始执行");
		thsHolderSpider.dofetchHolder();
		log.info("东方财富增发公告");
		emAddIssueSpider.dofetch(0);
		log.info("周六-同花顺解禁");
		thsJiejinSpider.dofetch();
	}
}
