package com.stable.service.model.v1;

import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.sort.FieldSortBuilder;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.data.elasticsearch.core.query.SearchQuery;
import org.springframework.stereotype.Service;

import com.stable.es.dao.base.EsStockAvgDao;
import com.stable.spider.tushare.TushareSpider;
import com.stable.utils.CurrencyUitl;
import com.stable.utils.ErrorLogFileUitl;
import com.stable.utils.PythonCallUtil;
import com.stable.vo.ModelV1context;
import com.stable.vo.bus.DaliyBasicInfo;
import com.stable.vo.bus.StockAvg;
import com.stable.vo.spi.req.EsQueryPageReq;
import com.stable.vo.up.strategy.ModelV1;

import lombok.extern.log4j.Log4j2;

@Service
@Log4j2
public class AvgService {

	@Value("${python.file.daily.avg}")
	private String pythonFileName;

	@Autowired
	private EsStockAvgDao stockAvgDao;

	public void saveStockAvg(List<StockAvg> avgList) {
		if (avgList.size() > 0) {
			stockAvgDao.saveAll(avgList);
		}
	}

	public void checkAvg(ModelV1 mv1, int startDate, List<StockAvg> avgList,
			List<DaliyBasicInfo> dailyList, ModelV1context cxt) {
		try {
			StockAvg av = null;//TODO
			String code = mv1.getCode();
			int endDate = mv1.getDate();
			StockAvg r = getAvg(av, code, startDate, endDate, avgList, true);
			if (r != null) {
				mv1.setAvgIndex(0);
				getAvgPriceIndex(mv1, av, cxt);
				getAvgPriceType(mv1, startDate, av, avgList, dailyList, cxt);
			} else {
				cxt.setDropOutMsg("未获取到均价");
			}
		} catch (Exception e) {
			cxt.setDropOutMsg("获取到均价异常");
			e.printStackTrace();
			ErrorLogFileUitl.writeError(e, "均线执行异常", "", "");
		}
	}

	private StockAvg getAvg(StockAvg av, String code, int startDate, int endDate, List<StockAvg> avgList,
			boolean isFirstLevel) {
		String params = TushareSpider.formatCode(code) + " " + startDate + " " + endDate + " qfq D";
		List<String> lines = PythonCallUtil.callPythonScript(pythonFileName, params);
		if (lines == null || lines.isEmpty() || lines.get(0).startsWith(PythonCallUtil.EXCEPT)) {
			log.warn("pythonFileName：{}，未获取到数据 params：{}", pythonFileName, code, params);
			if (lines != null && !lines.isEmpty()) {
				log.error("Python 错误：code：{}，PythonCallUtil.EXCEPT：{}", code, lines.get(0));
			}
			ErrorLogFileUitl.writeError(new RuntimeException(), code, "未获取到均价信息", startDate + " " + endDate);
			return null;
		}
		try {
			String[] strs = lines.get(0).replaceAll("nan", "0").split(",");
			if (strs[1].equals(String.valueOf(endDate))) {
				// code,date,3,5,10,20,30,120,250
				// 600408.SH,20200403,2.2933,2.302,2.282,2.3255,2.297,2.2712,2.4559
				if (!isFirstLevel) {
					av = new StockAvg();
				}
				av.setCode(code);
				av.setDate(endDate);
				av.setId();
				av.setAvgPriceIndex3(Double.valueOf(strs[2]));
				av.setAvgPriceIndex5(Double.valueOf(strs[3]));
				av.setAvgPriceIndex10(Double.valueOf(strs[4]));
				av.setAvgPriceIndex20(Double.valueOf(strs[5]));
				av.setAvgPriceIndex30(Double.valueOf(strs[6]));
				if (isFirstLevel) {
					av.setAvgPriceIndex120(Double.valueOf(strs[7]));
					av.setAvgPriceIndex250(Double.valueOf(strs[8]));
				}
				avgList.add(av);
				return av;
			}
		} catch (Exception e) {
			String msg = "获取到的数据:" + lines.get(0);
//			log.error(msg);
			throw new RuntimeException(msg, e);
		}
		return null;
	}

	private final EsQueryPageReq queryPage = new EsQueryPageReq(30);

	// 计算均线排列类型，1.V型反转,2.横盘突破，3.波浪上涨
	private void getAvgPriceType(ModelV1 mv1, int startDate, StockAvg av, List<StockAvg> avgList,
			List<DaliyBasicInfo> dailyList, ModelV1context cxt) {
		if (mv1.getAvgIndex() >= 10) {
			cxt.setBase20Avg(true);// 至少20日均线
			List<DaliyBasicInfo> day20 = new LinkedList<DaliyBasicInfo>();
			for (int i = 0; i < 20; i++) {
				day20.add(dailyList.get(i));
			}
			// 20天涨幅
			double max20 = day20.stream().max(Comparator.comparingDouble(DaliyBasicInfo::getHigh)).get().getHigh();
			double min20 = day20.stream().min(Comparator.comparingDouble(DaliyBasicInfo::getLow)).get().getLow();
//			log.info("20 days,max={},min={}", max20, min20);
			if (max20 > CurrencyUitl.topPrice20(min20)) {
				cxt.setDropOutMsg("20天涨幅超过20%");
				mv1.setAvgIndex(-100);
				return;
			}
			String code = av.getCode();
			List<StockAvg> avglist = queryListByCodeForModel(code, av.getDate(), queryPage);
			// 已有的map
			Map<Integer, StockAvg> map = new HashMap<Integer, StockAvg>();
			if (avglist != null && avglist.size() > 0) {
				avglist.stream().forEach(item -> {
					map.put(item.getDate(), item);
				});
			}
			map.put(av.getDate(), av);
			int end = 30;
			if (dailyList.size() < 30) {
				end = dailyList.size();
			}
			// 补全30天
			List<StockAvg> clist = new LinkedList<StockAvg>();
			for (int i = 0; i < end; i++) {
				DaliyBasicInfo d = dailyList.get(i);
				if (map.containsKey(d.getTrade_date())) {
					clist.add(map.get(d.getTrade_date()));
				} else {
					StockAvg r = getAvg(null, code, startDate, d.getTrade_date(), avgList, false);
					if (r == null) {
						log.warn("数据不全code={},startDate={},enddate={}", code, startDate, d.getTrade_date());
						cxt.setDropOutMsg("均线数据不全，补充不到完整均线");
						return;
					}
					clist.add(r);
				}
			}

			double maxAvg30 = clist.stream().max(Comparator.comparingDouble(StockAvg::getAvgPriceIndex30)).get()
					.getAvgPriceIndex30();
			double minAvg30 = clist.stream().min(Comparator.comparingDouble(StockAvg::getAvgPriceIndex30)).get()
					.getAvgPriceIndex30();

			StockAvg firstDay = clist.get(clist.size() - 1);
			StockAvg endDay = clist.get(0);
			int avgPrice30 = 0;

			if (CurrencyUitl.topPrice(minAvg30, true) <= maxAvg30) {// 1.振幅在5%以内
				// 往上走
				if (firstDay.getAvgPriceIndex30() < endDay.getAvgPriceIndex30()) {
					avgPrice30 += 20;
					cxt.addDetailDesc("30日均线突破往上-20");
				} else {
					// 均线排列往上
					avgPrice30 += 10;
					cxt.addDetailDesc("30日均线粘合-10");
				}
			} else if (CurrencyUitl.topPrice(minAvg30, false) <= maxAvg30) {// 2.振幅在10%以内
				// 往上走
				if (firstDay.getAvgPriceIndex30() < endDay.getAvgPriceIndex30()) {
					avgPrice30 += 15;
					cxt.addDetailDesc("30日均线突破往上-15");
				} else if (CurrencyUitl.topPrice(minAvg30, true) <= endDay.getAvgPriceIndex30()) {
					avgPrice30 += 10;
					cxt.addDetailDesc("30日均线粘合-10");
				}
			} else {
				// 往上走
				if (firstDay.getAvgPriceIndex30() < endDay.getAvgPriceIndex30()) {
					// 白马
					avgPrice30 = 1;
					mv1.setWhiteHorse(1);
					cxt.addDetailDesc("白马？");
				} else {
					// 剔除往下走或者振幅较大
					cxt.setDropOutMsg("均线往下走或者振幅超过10%");
					mv1.setAvgIndex(-100);
					return;
				}
			}

			// 排除下跌周期中，刚开始反转的均线
			long count = clist.stream().filter(x -> {
				return x.getAvgPriceIndex30() >= x.getAvgPriceIndex5();
			}).count();

			if (count > 15) {
				mv1.setAvgIndex(-100);
				cxt.setDropOutMsg("30个交易日中，超过15天30日均线大于5日均线");
				return;
			}
			mv1.setAvgIndex(mv1.getAvgIndex() + avgPrice30);
		}
	}

	// 计算AvgPriceIndex-排列
	private void getAvgPriceIndex(ModelV1 mv1, StockAvg av, ModelV1context cxt) {
		if (av.getAvgPriceIndex250() > 0) {
			if (av.getAvgPriceIndex3() >= av.getAvgPriceIndex5() && av.getAvgPriceIndex5() >= av.getAvgPriceIndex10()
					&& av.getAvgPriceIndex10() >= av.getAvgPriceIndex20()
					&& av.getAvgPriceIndex20() >= av.getAvgPriceIndex30()
					&& av.getAvgPriceIndex30() >= av.getAvgPriceIndex120()
					&& av.getAvgPriceIndex120() >= av.getAvgPriceIndex250()) {
				mv1.setAvgIndex(15);
				cxt.addDetailDesc("各均线排列");
				return;
			}
		}
		if (av.getAvgPriceIndex3() >= av.getAvgPriceIndex5() && av.getAvgPriceIndex5() >= av.getAvgPriceIndex10()
				&& av.getAvgPriceIndex10() >= av.getAvgPriceIndex20()
				&& av.getAvgPriceIndex20() >= av.getAvgPriceIndex30()) {
			mv1.setAvgIndex(12);
			cxt.addDetailDesc("30日均线排列");
			return;
		}
		if (av.getAvgPriceIndex3() >= av.getAvgPriceIndex5() && av.getAvgPriceIndex5() >= av.getAvgPriceIndex10()
				&& av.getAvgPriceIndex10() >= av.getAvgPriceIndex20()) {
			mv1.setAvgIndex(10);
			cxt.addDetailDesc("20日均线排列");
			return;
		}

		double max = Stream.of(av.getAvgPriceIndex3(), av.getAvgPriceIndex5(), av.getAvgPriceIndex10(),
				av.getAvgPriceIndex20(), av.getAvgPriceIndex30()).max(Double::compare).get();
		double min = Stream.of(av.getAvgPriceIndex3(), av.getAvgPriceIndex5(), av.getAvgPriceIndex10(),
				av.getAvgPriceIndex20(), av.getAvgPriceIndex30()).min(Double::compare).get();
		if (min >= CurrencyUitl.lowestPrice(max, true)) {// 最高价和最低价在5%以内的
			if (av.getAvgPriceIndex3() >= av.getAvgPriceIndex5() && av.getAvgPriceIndex30() >= av.getAvgPriceIndex3()
					&& av.getAvgPriceIndex30() >= av.getAvgPriceIndex5()
					&& av.getAvgPriceIndex30() >= av.getAvgPriceIndex10()
					&& av.getAvgPriceIndex30() >= av.getAvgPriceIndex20()) {
				mv1.setAvgIndex(5);
				cxt.addDetailDesc("30日均线5%振幅");
			}
			return;
		}
		if (min >= CurrencyUitl.lowestPrice(max, false)) {// 最高价和最低价在10%以内的
			if (av.getAvgPriceIndex3() >= av.getAvgPriceIndex5() && av.getAvgPriceIndex30() >= av.getAvgPriceIndex3()
					&& av.getAvgPriceIndex30() >= av.getAvgPriceIndex5()
					&& av.getAvgPriceIndex30() >= av.getAvgPriceIndex10()
					&& av.getAvgPriceIndex30() >= av.getAvgPriceIndex20()) {
				mv1.setAvgIndex(4);
				cxt.addDetailDesc("30日均线10%振幅");
			}
			return;
		}
		cxt.setDropOutMsg("均线不满要求");
	}

	public List<StockAvg> queryListByCodeForModel(String code, int date, EsQueryPageReq queryPage) {
		int pageNum = queryPage.getPageNum();
		int size = queryPage.getPageSize();
//		log.info("queryPage code={},trade_date={},pageNum={},size={}", code, date, pageNum, size);
		Pageable pageable = PageRequest.of(pageNum, size);
		BoolQueryBuilder bqb = QueryBuilders.boolQuery();
		bqb.must(QueryBuilders.matchPhraseQuery("code", code));
		bqb.must(QueryBuilders.rangeQuery("date").lte(date));
		FieldSortBuilder sort = SortBuilders.fieldSort("date").unmappedType("integer").order(SortOrder.DESC);

		NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder();
		SearchQuery sq = queryBuilder.withQuery(bqb).withSort(sort).withPageable(pageable).build();
		return stockAvgDao.search(sq).getContent();
	}
}
