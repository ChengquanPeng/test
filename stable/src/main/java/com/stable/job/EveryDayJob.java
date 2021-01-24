package com.stable.job;

import java.util.Calendar;
import java.util.Date;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.dangdang.ddframe.job.api.ShardingContext;
import com.stable.config.SpringConfig;
import com.stable.service.BuyBackService;
import com.stable.service.model.CodeModelService;
import com.stable.spider.jys.JysSpider;
import com.stable.utils.DateUtil;
import com.stable.utils.FileDeleteUitl;
import com.stable.utils.SpringUtil;
import com.stable.utils.WxPushUtil;

import lombok.extern.log4j.Log4j2;

/**
 * 周一到周五执行的任务 18:00
 * 
 */
@Component
@Log4j2
public class EveryDayJob extends MySimpleJob {

	@Autowired
	private BuyBackService buyBackService;
	@Autowired
	private CodeModelService codeModelService;
	@Autowired
	private JysSpider jysSpider;

	@Override
	public void myexecute(ShardingContext sc) {
		log.info("回购公告");
		buyBackService.jobFetchHistEveryDay();

		log.info("过期文件的删除");
		SpringConfig efc = SpringUtil.getBean(SpringConfig.class);
		FileDeleteUitl.deletePastDateFile(efc.getModelImageFloder());
		FileDeleteUitl.deletePastDateFile(efc.getModelV1SortFloder());
		FileDeleteUitl.deletePastDateFile(efc.getModelV1SortFloderDesc());

		log.info("交易所公告");
		jysSpider.byJob();

		// codeAttentionService.fetchAll();
		Calendar cal = Calendar.getInstance();
		cal.setTime(new Date());
		// 周一周4执行，每周末抓完财报后运行
		if (cal.get(Calendar.DAY_OF_WEEK) != Calendar.SUNDAY && cal.get(Calendar.DAY_OF_WEEK) != Calendar.SATURDAY
				&& cal.get(Calendar.DAY_OF_WEEK) != Calendar.FRIDAY) {
			codeModelService.runJobv2(true, Integer.valueOf(DateUtil.getTodayYYYYMMDD()));
		} else {
			WxPushUtil.pushSystem1("周五，周六，周日每晚23点不在运行定时运行 code model,周日下午在继续运行！");
		}
	}
}
