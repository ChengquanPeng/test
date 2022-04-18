package com.stable.job;

import java.util.Calendar;
import java.util.Date;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.dangdang.ddframe.job.api.ShardingContext;
import com.stable.service.ChipsZfService;
import com.stable.service.TradeCalService;
import com.stable.service.ZhiYaService;
import com.stable.service.model.CodeModelService;
import com.stable.service.monitor.MonitorPoolService;
import com.stable.spider.eastmoney.DzjySpider;
import com.stable.spider.eastmoney.EastmoneySpider;
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
	private CodeModelService codeModelService;
	@Autowired
	private MonitorPoolService monitorPoolService;
	@Autowired
	private ChipsZfService chipsZfService;
	@Autowired
	private DzjySpider emDzjySpider;
	@Autowired
	private ThsSpider thsSpider;
	@Autowired
	private ZhiYaService zhiYaService;
	@Autowired
	private EastmoneySpider eastmoneySpider;
	@Autowired
	private TradeCalService tradeCalService;

	@Override
	public void myexecute(ShardingContext sc) {
		Calendar cal = Calendar.getInstance();
		cal.setTime(new Date());
		int date = Integer.valueOf(DateUtil.getTodayYYYYMMDD());
		log.info("定增完成预警公告");
		monitorPoolService.jobZfDoneWarning();
		log.info("定增扩展属性");
		chipsZfService.jobZengFaExt(true);
		log.info("无效概念清除");
		thsSpider.deleteInvaildCodeConcept();
		log.info("交易所公告");
//		jysSpider.byJob();
//		log.info("过期文件的删除");
//		SpringConfig efc = SpringUtil.getBean(SpringConfig.class);
//		FileDeleteUitl.deletePastDateFile(efc.getModelImageFloder());
//		FileDeleteUitl.deletePastDateFile(efc.getModelV1SortFloder());
//		FileDeleteUitl.deletePastDateFile(efc.getModelV1SortFloderDesc());
		log.info("周五晚上，质押");
		if (cal.get(Calendar.DAY_OF_WEEK) == Calendar.FRIDAY) {
			new Thread(new Runnable() {
				@Override
				public void run() {
					eastmoneySpider.getDwcfCompanyTypeByJob();// 周五，东方财富更新企业类型
				}
			}).start();
			new Thread(new Runnable() {
				public void run() {
					zhiYaService.fetchBySun();// 质押
				}
			}).start();
		}
		if (!tradeCalService.isOpen(date)) {
			return;
		}

		String dateYYYY_ = DateUtil.formatYYYYMMDD2(cal.getTime());
		log.info("大宗交易");
		emDzjySpider.byDaily(dateYYYY_);
		log.info("大宗交易-预警");
		monitorPoolService.jobDzjyWarning();
		// 周一周4执行，每周末抓完财报后运行
		if (cal.get(Calendar.DAY_OF_WEEK) != Calendar.SUNDAY && cal.get(Calendar.DAY_OF_WEEK) != Calendar.SATURDAY
				&& cal.get(Calendar.DAY_OF_WEEK) != Calendar.FRIDAY) {
			codeModelService.runJobv2(date, false);
			// WxPushUtil.pushSystem1("周五，周六，周日每晚23点不在运行定时运行 code model,周日下午在继续运行！");
		}
	}
}
