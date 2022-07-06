package com.stable.service;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
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
import com.stable.msg.WxPushUtil;
import com.stable.spider.eastmoney.EastmoneyZytjSpider2;
import com.stable.utils.CurrencyUitl;
import com.stable.utils.DateUtil;
import com.stable.utils.ErrorLogFileUitl;
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
		int update = DateUtil.getTodayIntYYYYMMDD();
		int pre2Year = DateUtil.getPreYear(update, 2);
		List<StockBaseInfo> list = stockBasicService.getAllOnStatusListWithSort();
		int total = list.size();
		log.info("总数：" + total);
		List<ZhiYa> rl = new LinkedList<ZhiYa>();
		for (StockBaseInfo s : list) {
			String code = s.getCode();
			try {
				if (!stockBasicService.onlinePreYearChk(code, pre2Year)) {
					continue;
				}

				ZhiYa zy = eastmoneyZytjSpider.getZyT(code);// 中登数据
				if (zy == null) {
					zy = new ZhiYa();
				}
				zy.setUpdate(update);
				zy.setCode(code);
				if (zy.getTotalRatio() >= 10.0) {// 高质押风险
					zy.setHasRisk(1);
				}
				rl.add(zy);
				if (zy.getTotalRatio() <= 1.0) {// 小于跳过，不抓明细，明细来自公告，可能不准
					continue;
				}
				// 明细
				StringBuffer sb = new StringBuffer("中登数据->总质押比例:");
				sb.append(zy.getTotalRatio()).append("%").append(Constant.HTML_LINE);
				List<ZhiYaDetail> l = eastmoneyZytjSpider.getZy(code);
				List<Zya> m = this.split(l);
				if (m != null) {
					double warningLine = 0.0;
					double openLine = 0.0;
					for (int i = 0; i < m.size(); i++) {
						if (i >= 5) {// 最多5个
							continue;
						}
						Zya gd = m.get(i);
						sb.append(gd.getName()).append("->").append(Constant.HTML_LINE);
						sb.append(" 次数:" + gd.getC() + " 比例:" + CurrencyUitl.roundHalfUp(gd.getBi()) + "%")
								.append(Constant.HTML_LINE);
						if (zy.getTotalRatio() > 10.0) {
							if (gd.getBi() >= 80.0 && gd.getTbi() >= 10.0) {// 高质押机会
								if (gd.getTopWarningLine() > warningLine) {// 按质押分组中早最高的预警线（超过质押比例）
									warningLine = gd.getTopWarningLine();
									openLine = gd.getTopOpenLine();
								}
							}
						}
					}
					zy.setDetail(sb.toString());
					zy.setOpenLine(openLine);
					zy.setWarningLine(warningLine);
				}

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

	private List<Zya> split(List<ZhiYaDetail> l) {
		if (l != null && l.size() > 0) {
			Map<String, Zya> m = new HashMap<String, Zya>();
			for (ZhiYaDetail detail : l) {// --同一个股东多次质押
				if (detail.getState() != 1) {// 不含-已解押的
					Zya tmp = m.get(detail.getHolderName());
					if (tmp == null) {
						tmp = new Zya();
						tmp.setName(detail.getHolderName());
						m.put(detail.getHolderName(), tmp);
					}
					tmp.setC(tmp.getC() + 1);
					tmp.setBi(tmp.getBi() + detail.getSelfRatio());// 自己所持
					tmp.setTbi(tmp.getTbi() + detail.getTotalRatio());// 总股本
					if (detail.getWarningLine() > tmp.getTopWarningLine()) {// 按质押分组中早最高的预警线（可能质押比例不够）
						tmp.setTopWarningLine(detail.getWarningLine());
						tmp.setTopOpenLine(detail.getOpenline());
					}
				}
			}
			Collection<Zya> list = m.values();
			List<Zya> rl = new LinkedList<Zya>();
			for (Zya z : list) {
				rl.add(z);
			}
			Collections.sort(rl, new Comparator<Zya>() {
				@Override
				public int compare(Zya o1, Zya o2) {
					if (o1.getTbi() == o2.getTbi()) {
						return 0;
					}
					return o2.getTbi() - o1.getTbi() > 0 ? 1 : -1;
				}
			});
			return rl;
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
