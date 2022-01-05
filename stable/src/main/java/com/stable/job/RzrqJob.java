package com.stable.job;

import java.util.Calendar;
import java.util.Date;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.dangdang.ddframe.job.api.ShardingContext;
import com.stable.service.TradeCalService;
import com.stable.spider.eastmoney.RzrqSpider;
import com.stable.utils.DateUtil;

import lombok.extern.log4j.Log4j2;

/**
 * 融资融券任何（融资融券是第二天早上更新的），比较特殊
 * 
 */
@Component
@Log4j2
public class RzrqJob extends MySimpleJob {
	@Autowired
	private TradeCalService tradeCalService;
	@Autowired
	private RzrqSpider rzrqSpider;

	@Override
	public void myexecute(ShardingContext sc) {
		Date yes = DateUtil.addDate(new Date(), -1);
		Calendar cal = Calendar.getInstance();
		cal.setTime(yes);
		int date = DateUtil.formatYYYYMMDDReturnInt(yes);

		if (!tradeCalService.isOpen(date)) {
			log.info("融资融券-非交易日");
			return;
		}
		if (cal.get(Calendar.DAY_OF_WEEK) != Calendar.SUNDAY && cal.get(Calendar.DAY_OF_WEEK) != Calendar.SATURDAY) {
			String dateYYYY_ = DateUtil.formatYYYYMMDD2(yes);
			log.info("融资融券-交易日开始:{}", dateYYYY_);
			rzrqSpider.byDaily(dateYYYY_, date);
		} else {
			log.info("融资融券-周末");
		}
	}
}
