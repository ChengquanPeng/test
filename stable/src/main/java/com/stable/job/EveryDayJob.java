package com.stable.job;

import java.util.Calendar;
import java.util.Date;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.dangdang.ddframe.job.api.ShardingContext;
import com.stable.service.ChipsZfService;
import com.stable.service.DeleteDataService;
import com.stable.service.FinanceService;
import com.stable.service.ZhiYaService;
import com.stable.service.monitor.MonitorPoolService;
import com.stable.spider.eastmoney.EastmoneySpider;
import com.stable.spider.ths.ThsEventSpider;
import com.stable.utils.DateUtil;

import lombok.extern.log4j.Log4j2;

/**
 * 周一到周五执行的任务 18:00
 * 
 */
@Component
@Log4j2
public class EveryDayJob extends MySimpleJob {

//	@Autowired
//	private RunModelService runModelService;
	@Autowired
	private MonitorPoolService monitorPoolService;
	@Autowired
	private ChipsZfService chipsZfService;
	@Autowired
	private DeleteDataService deleteDataService;
	@Autowired
	private ZhiYaService zhiYaService;
	@Autowired
	private EastmoneySpider eastmoneySpider;
//	@Autowired
//	private TradeCalService tradeCalService;
	@Autowired
	private ThsEventSpider thsEventSpider;
	@Autowired
	private FinanceService financeService;

	@Override
	public void myexecute(ShardingContext sc) {
		log.info("1.服务到期");
		monitorPoolService.userExpired();
		Calendar cal = Calendar.getInstance();
		cal.setTime(new Date());
		int date = Integer.valueOf(DateUtil.getTodayYYYYMMDD());
		log.info("2.快预报");
		financeService.jobSpiderKuaiYuBao();
		log.info("定增完成预警公告");
		monitorPoolService.jobZfDoneWarning();
		// 删除退市监听
		monitorPoolService.deleteTsMoni();
		log.info("定增扩展属性");
		chipsZfService.jobZengFaExt(true);
		log.info("无效概念无效数据");
		deleteDataService.deleteData();
//		log.info("过期文件的删除");
//		SpringConfig efc = SpringUtil.getBean(SpringConfig.class);
//		FileDeleteUitl.deletePastDateFile(efc.getModelImageFloder());
//		FileDeleteUitl.deletePastDateFile(efc.getModelV1SortFloder());
//		FileDeleteUitl.deletePastDateFile(efc.getModelV1SortFloderDesc());
		log.info("周五晚上，质押");
		int calweek = cal.get(Calendar.DAY_OF_WEEK);
		if (calweek == Calendar.FRIDAY) {
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
		if (calweek == Calendar.WEDNESDAY) {// 每周3
			thsEventSpider.byWeb();
		}
		log.info("最新公告");
		monitorPoolService.listenerGg(date);

//		if (!tradeCalService.isOpen(date)) {
//			return;
//		}
		// 周一周4执行，每周末抓完财报后运行
		// if (calweek != Calendar.SUNDAY && calweek != Calendar.SATURDAY && calweek !=
		// Calendar.FRIDAY) {
		// runModelService.runModel(date, false);
		// WxPushUtil.pushSystem1("周五，周六，周日每晚23点不在运行定时运行 code model,周日下午在继续运行！");
//		}
	}
}
