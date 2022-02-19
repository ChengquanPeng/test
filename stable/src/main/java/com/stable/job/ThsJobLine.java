package com.stable.job;

import java.util.Calendar;
import java.util.Date;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.stable.spider.igoodstock.IgoodstockSpider;
import com.stable.spider.ths.ThsBonusSpider;
import com.stable.spider.ths.ThsCompanySpider;
import com.stable.spider.ths.ThsHolderSpider;
import com.stable.spider.ths.ThsPlateSpider;

import lombok.extern.log4j.Log4j2;

@Component
@Log4j2
public class ThsJobLine {
	@Autowired
	private ThsHolderSpider thsHolderSpider;
	@Autowired
	private ThsPlateSpider thsPlateSpider;
	@Autowired
	private ThsBonusSpider thsBonusSpider;
	@Autowired
	private ThsCompanySpider thsCompanySpider;
	@Autowired
	private IgoodstockSpider igoodstockSpider;

	// 每日凌晨
	public void start() {
		Calendar cal = Calendar.getInstance();
		cal.setTime(new Date());
		int week = cal.get(Calendar.DAY_OF_WEEK);
		// 周二到周五凌晨（实际是抓取周1到周4的情况）
		if (week == Calendar.TUESDAY || week == Calendar.WEDNESDAY || week == Calendar.THURSDAY
				|| week == Calendar.FRIDAY) {
			log.info("每日股东人数任务开始执行");
			// 交易日(周一到周五)
			thsHolderSpider.dofetchHolder(false);// 放到周日运行？
		}

		if (week == Calendar.SUNDAY) {
			log.info("周日，同花顺-公司资料");
			thsCompanySpider.byJob();// 周日，同花顺-公司资料
			igoodstockSpider.byWeb();// 外资持股
		}

		if (week == Calendar.SATURDAY) {
			log.info("周六");
			log.info("同花顺, 增发&分紅");
			thsBonusSpider.byJob();// 同花顺, 增发&分紅
		}

		if (week == Calendar.MONDAY) {
			log.info("同花顺-亮点，主营 fetchAll=true");
			thsPlateSpider.fetchAll(true);// 同花顺-亮点，主营 多线程
		} else {
			log.info("同花顺-亮点，主营 fetchAll=false");
			thsPlateSpider.fetchAll(false);// 同花顺-亮点，主营 多线程
		}
	}
}
