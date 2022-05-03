package com.stable.service;

import java.util.HashMap;
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
import com.stable.es.dao.base.ZhiYaDetailDao;
import com.stable.spider.eastmoney.EastmoneyZytjSpider;
import com.stable.spider.eastmoney.EastmoneyZytjSpider2;
import com.stable.utils.CurrencyUitl;
import com.stable.utils.DateUtil;
import com.stable.utils.ErrorLogFileUitl;
import com.stable.utils.WxPushUtil;
import com.stable.vo.Zya;
import com.stable.vo.bus.StockBaseInfo;
import com.stable.vo.bus.ZhiYa;
import com.stable.vo.bus.ZhiYaDetail;

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
	@Autowired
	private EastmoneyZytjSpider2 eastmoneyZytjSpider;
	@Autowired
	private ZhiYaDetailDao zhiYaDetailDao;

//	@PostConstruct
//	private void start() {
//		new Thread(new Runnable() {
//			@Override
//			public void run() {
//				try {
//					Thread.sleep(60 * 1000);
//					fetchBySun();
//				} catch (Exception e) {
//					e.printStackTrace();
//				}
//			}
//		}).start();
//	}

	public synchronized void fetchBySun() {
		EastmoneyZytjSpider.errorcnt = new LinkedList<String>();
		int update = DateUtil.getTodayIntYYYYMMDD();
//		int endDate = DateUtil.formatYYYYMMDDReturnInt(DateUtil.addDate(new Date(), (-365 * 5)));
		List<StockBaseInfo> list = stockBasicService.getAllOnStatusListWithSort();
		int total = list.size();
		log.info("总数：" + total);
		List<ZhiYa> rl = new LinkedList<ZhiYa>();
		for (StockBaseInfo s : list) {
			String code = s.getCode();
			try {
				ZhiYa zy = new ZhiYa();
				zy.setCode(code);
				zy.setUpdate(update);
				zy.setHasRisk(0);
				boolean r1 = false;
				StringBuffer sb = new StringBuffer("");
				List<ZhiYaDetail> l = eastmoneyZytjSpider.getZy(code);
				Map<String, Zya> m = this.split(l);
				if (m != null) {
					Zya tzy = m.get(EastmoneyZytjSpider.TOTAL_BI);
					double highRatio = 0.0;
					double warningLine = 0.0;
					double openLine = 0.0;
					for (String key : m.keySet()) {
						Zya gd = m.get(key);
//					System.err.println(key + "-> 次数:" + z.getC() + " 比例:" + z.getBi() + "%");
						sb.append(key).append("->").append(Constant.HTML_LINE);
						sb.append(" 次数:" + gd.getC() + " 比例:" + CurrencyUitl.roundHalfUp(gd.getBi()) + "%")
								.append(Constant.HTML_LINE);
						if (gd.getBi() >= 80.0) {// 股东高质押
							r1 = true;
						}
						if (gd.getBi() > highRatio) {
							highRatio = gd.getBi();
						}
						if (tzy.getTbi() > 10.0) {
							if (gd.getBi() >= 80.0 && gd.getTbi() >= 10.0) {// 高质押机会
								if (gd.getTopWarningLine() > warningLine) {// 按质押分组中早最高的预警线（超过质押比例）
									warningLine = gd.getTopWarningLine();
									openLine = gd.getTopOpenLine();
								}
							}
						}
					}
					zy.setDetail(sb.toString());
					zy.setHighRatio(CurrencyUitl.roundHalfUp(highRatio));
					zy.setTotalRatio(CurrencyUitl.roundHalfUp(tzy.getTbi()));
					zy.setOpenLine(openLine);
					zy.setWarningLine(warningLine);
					if (r1 && tzy.getTbi() >= 10.0) {// 股东自身超过80%的质押，总股本超过10%
						zy.setHasRisk(1);
					}
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
		log.info("质押抓包完成");
	}

	private Map<String, Zya> split(List<ZhiYaDetail> l) {
		if (l != null && l.size() > 0) {
			this.zhiYaDetailDao.saveAll(l);

			Map<String, Zya> m = new HashMap<String, Zya>();
			Zya tot = new Zya();
			tot.setC(0);
			tot.setBi(0.0);
			m.put(EastmoneyZytjSpider.TOTAL_BI, tot);

			for (ZhiYaDetail detail : l) {// --同一个股东多次质押
				if (detail.getState() != 1) {// 不含-已解押的

					Zya tmp = m.get(detail.getHolderName());
					if (tmp == null) {
						tmp = new Zya();
						m.put(detail.getHolderName(), tmp);
					}
					tmp.setC(tmp.getC() + 1);
					tmp.setBi(tmp.getBi() + detail.getSelfRatio());// 自己所持
					tmp.setTbi(tmp.getTbi() + detail.getTotalRatio());// 总股本
					if (detail.getWarningLine() > tmp.getTopWarningLine()) {// 按质押分组中早最高的预警线（可能质押比例不够）
						tmp.setTopWarningLine(detail.getWarningLine());
						tmp.setTopOpenLine(detail.getOpenline());
					}

					// 总体
					tot.setC(tot.getC() + 1);
					tot.setTbi(tot.getTbi() + detail.getTotalRatio());// 总股本
				}
			}
			return m;
		}
		return null;
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
