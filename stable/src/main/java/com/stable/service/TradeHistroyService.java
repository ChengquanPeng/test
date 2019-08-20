package com.stable.service;

import java.util.List;

import javax.annotation.PostConstruct;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.stereotype.Service;

import com.stable.constant.Constant;
import com.stable.constant.RedisConstant;
import com.stable.es.dao.EsTradeHistInfoDaliyDao;
import com.stable.spider.sina.SinaSpider;
import com.stable.spider.tushare.TushareSpider;
import com.stable.utils.RedisUtil;
import com.stable.vo.bus.TradeHistInfoDaliy;

import lombok.extern.log4j.Log4j2;

@Service
@Log4j2
public class TradeHistroyService {
	@Autowired
	private ElasticsearchTemplate elasticsearchTemplate;
	@Autowired
	private TushareSpider tushareSpider;
	@Autowired
	private RedisUtil redisUtil;
	@Autowired
	private SinaSpider sinaSpider;
	@Autowired
	private EsTradeHistInfoDaliyDao tradeHistDaliy;

	//全量获取历史记录（定时任务）-根据缓存是否需要重新获取，（除权得时候会重新获取）
	public boolean spiderDaliyTradeHistoryInfoFullJob(String code) {
		String rv = redisUtil.get(RedisConstant.RDS_TRADE_HIST_DAY_ + code);
		if (StringUtils.isNotBlank(rv)) {
			return true;
		}
		spiderDaliyTradeHistoryInfoFullDirect(code);
		redisUtil.set(RedisConstant.RDS_TRADE_HIST_DAY_ + code, "1");
		return true;
	}

	//直接全量获取历史记录，不需要根据缓存来判断
	public boolean spiderDaliyTradeHistoryInfoFullDirect(String code) {
		List<String> data = sinaSpider.getDaliyTradyHistory(code);
		if (data == null) {
			return false;
		}
		data.forEach(line -> {
			if (line.startsWith(Constant.NUM_ER)) {
				TradeHistInfoDaliy d = new TradeHistInfoDaliy(code, line);
				tradeHistDaliy.save(d);
			} else {
				log.debug(line);
			}
		});
		data = null;
		return true;
	}
	//每日更新-job
	//收到更新-手工
	
	@PostConstruct
	public void test() {
		spiderDaliyTradeHistoryInfoFullDirect("688009");
	}
}
