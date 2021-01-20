package com.stable.job;

import java.util.Calendar;
import java.util.Date;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.stable.spider.ths.ThsBonusSpider;
import com.stable.spider.ths.ThsCompanySpider;
import com.stable.spider.ths.ThsHolderSpider;
import com.stable.spider.ths.ThsJiejinSpider;
import com.stable.spider.ths.ThsPlateSpider;

import lombok.extern.log4j.Log4j2;

@Component
@Log4j2
public class ThsJobLine {
	@Autowired
	private ThsJiejinSpider thsJiejinSpider;
	@Autowired
	private ThsHolderSpider thsHolderSpider;
	@Autowired
	private ThsPlateSpider thsPlateSpider;
	@Autowired
	private ThsBonusSpider thsBonusSpider;
	@Autowired
	private ThsCompanySpider thsCompanySpider;

	public void start() {
		log.info("每日股东人数任务开始执行");
		// 交易日(周一到周五)
		thsHolderSpider.dofetchHolder();

		Calendar cal = Calendar.getInstance();
		cal.setTime(new Date());

		if (cal.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) {
			log.info("周日，同花顺-公司资料");
			thsCompanySpider.byJob();// 周日，同花顺-公司资料
		}

		if (cal.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY) {
			log.info("周六");
			thsBonusSpider.byJob();// 同花顺, 增发
			thsJiejinSpider.byJob();// 同花顺, 解禁&分紅
			log.info("同花顺-亮点，主营 fetchAll=true");
			thsPlateSpider.fetchAll(true);// 同花顺-亮点，主营 多线程
		} else {
			log.info("同花顺-亮点，主营 fetchAll=false");
			thsPlateSpider.fetchAll(false);// 同花顺-亮点，主营 多线程
		}
	}
}
