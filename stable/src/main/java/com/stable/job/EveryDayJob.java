package com.stable.job;

import java.util.Calendar;
import java.util.Date;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.dangdang.ddframe.job.api.ShardingContext;
import com.stable.service.BuyBackService;
import com.stable.service.ChipsZfService;
import com.stable.service.ZhiYaService;
import com.stable.service.model.CodeModelService;
import com.stable.service.monitor.MonitorPoolService;
import com.stable.spider.eastmoney.EmDzjySpider;
import com.stable.spider.official.JysSpider;
import com.stable.spider.ths.ThsSpider;
import com.stable.utils.DateUtil;

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
	@Autowired
	private MonitorPoolService monitorPoolService;
	@Autowired
	private ChipsZfService chipsZfService;
	@Autowired
	private EmDzjySpider emDzjySpider;
	@Autowired
	private ThsSpider thsSpider;
	@Autowired
	private ZhiYaService zhiYaService;

	@Override
	public void myexecute(ShardingContext sc) {
		Calendar cal = Calendar.getInstance();
		cal.setTime(new Date());
		int date = Integer.valueOf(DateUtil.getTodayYYYYMMDD());
		log.info("大宗交易");
		emDzjySpider.byDaily(DateUtil.formatYYYYMMDD2(cal.getTime()), date);
//		emDzjySpider.byJob();
		monitorPoolService.jobDzjyWarning();
		log.info("回购公告");
		buyBackService.jobFetchHistEveryDay();

//		log.info("过期文件的删除");
//		SpringConfig efc = SpringUtil.getBean(SpringConfig.class);
//		FileDeleteUitl.deletePastDateFile(efc.getModelImageFloder());
//		FileDeleteUitl.deletePastDateFile(efc.getModelV1SortFloder());
//		FileDeleteUitl.deletePastDateFile(efc.getModelV1SortFloderDesc());

		log.info("定增完成预警公告");
		monitorPoolService.jobZfDoneWarning();
		log.info("定增扩展属性");
		chipsZfService.jobZengFaExt(true);

		log.info("交易所公告");
		jysSpider.byJob();

		// 周一周4执行，每周末抓完财报后运行
		if (cal.get(Calendar.DAY_OF_WEEK) != Calendar.SUNDAY && cal.get(Calendar.DAY_OF_WEEK) != Calendar.SATURDAY
				&& cal.get(Calendar.DAY_OF_WEEK) != Calendar.FRIDAY) {
//			financeService.fetchFinances();
			codeModelService.runJobv2(date, false);
		} else {
			// WxPushUtil.pushSystem1("周五，周六，周日每晚23点不在运行定时运行 code model,周日下午在继续运行！");
		}
		log.info("无效概念清除");
		thsSpider.deleteInvaildCodeConcept();

		log.info("周五晚上，质押");
		if (cal.get(Calendar.DAY_OF_WEEK) == Calendar.FRIDAY) {
			new Thread(new Runnable() {
				@Override
				public void run() {
					zhiYaService.fetchBySun();// 质押
				}
			}).start();
		}
	}
//
//	@Autowired
//	private FinanceService financeService;
}
