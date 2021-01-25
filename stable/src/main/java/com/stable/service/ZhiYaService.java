package com.stable.service;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.data.elasticsearch.core.query.SearchQuery;
import org.springframework.stereotype.Service;

import com.stable.constant.Constant;
import com.stable.es.dao.base.ZhiYaDao;
import com.stable.spider.eastmoney.EastmoneyZytjSpider;
import com.stable.utils.DateUtil;
import com.stable.utils.ErrorLogFileUitl;
import com.stable.utils.WxPushUtil;
import com.stable.vo.Zya;
import com.stable.vo.bus.StockBaseInfo;
import com.stable.vo.bus.ZhiYa;

import lombok.extern.log4j.Log4j2;

/**
 * 质押
 */
@Service
@Log4j2
public class ZhiYaService {
	@Autowired
	private ZhiYaDao zhiYaDao;
	@Autowired
	private StockBasicService stockBasicService;

	public void fetchBySun() {
		int update = DateUtil.getTodayIntYYYYMMDD();
		int endDate = DateUtil.formatYYYYMMDDReturnInt(DateUtil.addDate(new Date(), (-365 * 5)));
		List<StockBaseInfo> list = stockBasicService.getAllOnStatusList();
		int total = list.size();
		log.info("总数：" + total);
		List<ZhiYa> rl = new LinkedList<ZhiYa>();
		for (StockBaseInfo s : list) {
			String code = s.getCode();
			try {
				ZhiYa zy = new ZhiYa();
				zy.setCode(code);
				boolean r1 = false;
				StringBuffer sb = new StringBuffer("");
				Map<String, Zya> m = EastmoneyZytjSpider.getZy(code, endDate);
				Zya tzy = m.get(EastmoneyZytjSpider.TOTAL_BI);
				double highRatio = 0.0;
				for (String key : m.keySet()) {
					Zya z = m.get(key);
//					System.err.println(key + "-> 次数:" + z.getC() + " 比例:" + z.getBi() + "%");
					sb.append(key).append(Constant.HTML_LINE);
					sb.append("-> 次数:" + z.getC() + " 比例:" + z.getBi() + "%").append(Constant.HTML_LINE);
					if (z.getBi() > 50.0) {
						r1 = true;
					}
					if (z.getBi() > highRatio) {
						highRatio = z.getBi();
					}
				}
				zy.setDetail(sb.toString());
				zy.setHighRatio(highRatio);
				zy.setTotalRatio(tzy.getBi());
				zy.setUpdate(update);
				zy.setHasRisk(0);
				if (r1 && tzy.getBi() > 7.0) {// 股东超过50%的质押，总股本超过10%
					zy.setHasRisk(1);
				}
				rl.add(zy);
			} catch (Exception e) {
				WxPushUtil.pushSystem1("质押抓包异常:" + code);
				ErrorLogFileUitl.writeError(e, "质押", "", "");
			}
		}
		if (rl.size() > 0) {
			zhiYaDao.saveAll(rl);
		}
	}

	public ZhiYa getZhiYa(String code) {
		BoolQueryBuilder bqb = QueryBuilders.boolQuery();
		bqb.must(QueryBuilders.matchPhraseQuery("code", code));
		NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder();
		SearchQuery sq = queryBuilder.withQuery(bqb).build();

		Page<ZhiYa> page = zhiYaDao.search(sq);
		if (page != null && !page.isEmpty()) {
			return page.getContent().get(0);
		}
		return new ZhiYa();
	}
}
