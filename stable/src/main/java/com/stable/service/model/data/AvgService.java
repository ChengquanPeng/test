package com.stable.service.model.data;

import java.util.List;

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
import com.stable.utils.ErrorLogFileUitl;
import com.stable.utils.PythonCallUtil;
import com.stable.utils.ThreadsUtil;
import com.stable.vo.bus.StockAvg;
import com.stable.vo.spi.req.EsQueryPageReq;

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

	public StockAvg getDPriceAvg(String code, int startDate, int endDate, List<StockAvg> avgList) {
		String params = TushareSpider.formatCode(code) + " " + startDate + " " + endDate + " qfq D";
		boolean gotData = false;
		List<String> lines = null;
		int i = 0;
		do {
			lines = PythonCallUtil.callPythonScript(pythonFileName, params);
			if (lines == null || lines.isEmpty() || lines.get(0).startsWith(PythonCallUtil.EXCEPT)) {
				if (i >= 3) {
					log.warn("pythonFileName：{}，未获取到数据 params：{}", pythonFileName, code, params);
					if (lines != null && !lines.isEmpty()) {
						log.error("Python 错误：code：{}，PythonCallUtil.EXCEPT：{}", code, lines.get(0));
					}
					ErrorLogFileUitl.writeError(new RuntimeException(), code, "未获取到均价信息", startDate + " " + endDate);
					return null;
				}
				ThreadsUtil.sleepRandomSecBetween5And15();
			} else {
				gotData = true;
			}
			i++;
		} while (!gotData);

		try {
			String[] strs = lines.get(0).replaceAll("nan", "0").split(",");
			if (strs[1].equals(String.valueOf(endDate))) {
				// code,date,3,5,10,20,30,120,250
				// 600408.SH,20200403,2.2933,2.302,2.282,2.3255,2.297,2.2712,2.4559
				StockAvg av = new StockAvg();
				av.setCode(code);
				av.setDate(endDate);
				av.setId();
				av.setAvgPriceIndex3(Double.valueOf(strs[2]));
				av.setAvgPriceIndex5(Double.valueOf(strs[3]));
				av.setAvgPriceIndex10(Double.valueOf(strs[4]));
				av.setAvgPriceIndex20(Double.valueOf(strs[5]));
				av.setAvgPriceIndex30(Double.valueOf(strs[6]));
				av.setAvgPriceIndex120(Double.valueOf(strs[7]));
				av.setAvgPriceIndex250(Double.valueOf(strs[8]));
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
