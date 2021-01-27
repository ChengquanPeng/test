package com.stable.service;

import java.util.Comparator;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.sort.FieldSortBuilder;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.data.elasticsearch.core.query.SearchQuery;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.stable.constant.EsQueryPageUtil;
import com.stable.enums.ZfStatus;
import com.stable.es.dao.base.ZengFaDao;
import com.stable.es.dao.base.ZengFaDetailDao;
import com.stable.es.dao.base.ZengFaExtDao;
import com.stable.es.dao.base.ZengFaSummaryDao;
import com.stable.spider.ths.ThsAnnSpider;
import com.stable.spider.tushare.TushareSpider;
import com.stable.utils.CurrencyUitl;
import com.stable.utils.DateUtil;
import com.stable.utils.ErrorLogFileUitl;
import com.stable.utils.ThreadsUtil;
import com.stable.utils.WxPushUtil;
import com.stable.vo.bus.DaliyBasicInfo2;
import com.stable.vo.bus.TradeHistInfoDaliy;
import com.stable.vo.bus.ZengFa;
import com.stable.vo.bus.ZengFaDetail;
import com.stable.vo.bus.ZengFaExt;
import com.stable.vo.bus.ZengFaSummary;
import com.stable.vo.http.resp.ZengFaResp;
import com.stable.vo.spi.req.EsQueryPageReq;

/**
 * 筹码-增发
 */
@Service
//@Log4j2
public class ChipsZfService {
	@Autowired
	private ZengFaDao zengFaDao;
	@Autowired
	private ZengFaDetailDao zengFaDetailDao;
	@Autowired
	private ZengFaSummaryDao zengFaSummaryDao;
	@Autowired
	private ZengFaExtDao zengFaExtDao;
	@Autowired
	private StockBasicService stockBasicService;
	@Autowired
	private DaliyTradeHistroyService daliyTradeHistroyService;
	@Autowired
	private DaliyBasicHistroyService daliyBasicHistroyService;
	@Autowired
	private TushareSpider tushareSpider;

	/**
	 * 增发-概况
	 */
	public ZengFaSummary getZengFaSummary(String code) {
		BoolQueryBuilder bqb = QueryBuilders.boolQuery();
		bqb.must(QueryBuilders.matchPhraseQuery("code", code));
		NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder();
		SearchQuery sq = queryBuilder.withQuery(bqb).build();
		Page<ZengFaSummary> page = zengFaSummaryDao.search(sq);
		if (page != null && !page.isEmpty()) {
			return page.getContent().get(0);
		}
		return new ZengFaSummary();
	}

	/**
	 * 最后的增发记录
	 */
	public ZengFa getLastZengFa(String code) {
		BoolQueryBuilder bqb = QueryBuilders.boolQuery();
		bqb.must(QueryBuilders.matchPhraseQuery("code", code));
		FieldSortBuilder sort = SortBuilders.fieldSort("startDate").unmappedType("integer").order(SortOrder.DESC);
		NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder();
		SearchQuery sq = queryBuilder.withQuery(bqb).withSort(sort).build();
		Page<ZengFa> page = zengFaDao.search(sq);
		if (page != null && !page.isEmpty()) {
			return page.getContent().get(0);
		}
		return new ZengFa();
	}

	/**
	 * 最后的增发详情记录
	 */
	public ZengFaDetail getLastZengFaDetail(String code) {
		BoolQueryBuilder bqb = QueryBuilders.boolQuery();
		bqb.must(QueryBuilders.matchPhraseQuery("code", code));
		FieldSortBuilder sort = SortBuilders.fieldSort("date").unmappedType("integer").order(SortOrder.DESC);
		NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder();
		SearchQuery sq = queryBuilder.withQuery(bqb).withSort(sort).build();
		Page<ZengFaDetail> page = zengFaDetailDao.search(sq);
		if (page != null && !page.isEmpty()) {
			return page.getContent().get(0);
		}
		return new ZengFaDetail();
	}

	/**
	 * -增发记录
	 */
	public List<ZengFaResp> getZengFaListForWeb(String code, String status, EsQueryPageReq querypage) {
		List<ZengFa> list = getZengFaList(code, status, querypage);
		if (list != null) {
			List<ZengFaResp> l = new LinkedList<ZengFaResp>();
			for (ZengFa zf : list) {
				ZengFaResp r = new ZengFaResp();
				BeanUtils.copyProperties(zf, r);
				r.setCodeName(stockBasicService.getCodeName(zf.getCode()));
				if (zf.getStatus() == 2) {
					ZengFaExt ext = this.getZengFaExtById(zf.getId());
					if (ext != null) {
						if (ext.getBuy() == 1) {
							r.setBuy("购买");
						} else {
							r.setBuy("-");
						}
						if (ext.getSelfzf() == 1) {
							r.setSelfzf("自己人");
						} else if (ext.getSelfzf() == 2) {
							r.setSelfzf("正常增发");
						} else {
							r.setSelfzf("-");
						}
						if (ext.getCompType() == 1) {
							r.setCompType("国企");
						} else if (ext.getCompType() == 2) {
							r.setCompType("民企");
						} else {
							r.setCompType("-");
						}
						r.setMarketVal(ext.getCircMarketVal() + "<br/>" + ext.getTotalMarketVal());// 总市值
					}
				}

				l.add(r);
			}
			return l;
		}
		return null;
	}

	/**
	 * -增发记录
	 */
	public List<ZengFa> getZengFaList(String code, String status, EsQueryPageReq querypage) {
		return getZengFaList(code, status, 0, querypage);
	}

	public synchronized void jobZengFaExt(boolean isJob) {
		int endDate = 0; // 全部
		if (isJob) {
			endDate = DateUtil.formatYYYYMMDDReturnInt(DateUtil.addDate(new Date(), -90));
		}
		StringBuffer sb = new StringBuffer();
		List<ZengFa> l = getZengFaList("", ZfStatus.DONE.getCode() + "", endDate, EsQueryPageUtil.queryPage9999);
		for (ZengFa zf : l) {
			try {
				if (zf.getEndDate() <= 20180101) {
					continue;
				}
				ZengFaExt zfe = getZengFaExtById(zf.getId());
				if (zfe == null) {
					zfe = new ZengFaExt();
					zfe.setId(zf.getId());
					zfe.setCode(zf.getCode());
					zfe.setDate(zf.getEndDate());
					String s = ThsAnnSpider.dofetch(zf.getCode(), zf.getStartDate());
					if (StringUtils.isNotBlank(s)) {
						zfe.setBuy(1);
						zfe.setTitle(s);
						sb.append(zfe.getCode()).append(",");
					}
					ws(zfe, zf.getEndDate());
					zengFaExtDao.save(zfe);
				}
			} catch (Exception e) {
				ErrorLogFileUitl.writeError(e, "ZengFaExt 增发是否购买资产出错", "", "");
			}
		}
		if (sb.length() > 0) {
			WxPushUtil.pushSystem1("增发完成且是购买资产：" + sb.toString());
		}

		ThreadsUtil.sleep(10, TimeUnit.MINUTES);

		List<ZengFaExt> exts = getZengFaExtListWithChk();
		if (exts != null) {
			for (ZengFaExt z : exts) {
				try {
					ZengFa orig = this.getZengFaById(z.getId());
					ws(z, orig.getEndDate());
				} catch (Exception e) {
					ErrorLogFileUitl.writeError(e, "ZengFaExt 是否自己人在增发出错", "", "");
				}
			}
			zengFaExtDao.saveAll(exts);
		}
	}

	private void ws(ZengFaExt z, int zfEndDate) {
		String code = z.getCode();
		// 20个交易日股价
		List<TradeHistInfoDaliy> befor45 = daliyTradeHistroyService.queryListByCodeWithLastQfq(code, 0, zfEndDate,
				EsQueryPageUtil.queryPage60, SortOrder.DESC);
		double end = befor45.get(0).getClosed();
		double max = befor45.stream().max(Comparator.comparingDouble(TradeHistInfoDaliy::getClosed)).get().getClosed();
		z.setSelfzf(2);
		if (max > end) {
			if (CurrencyUitl.cutProfit(end, max) >= 15.0) {
				z.setSelfzf(1);// 增发前跌幅在20%以上
			}
		}
		z.setCompType(stockBasicService.getCode(code).getCompnayType());
		if (zfEndDate >= 20210125) {
			DaliyBasicInfo2 d = daliyBasicHistroyService.queryLastest(code, zfEndDate);
			z.setCircMarketVal(d.getCircMarketVal());
			z.setTotalMarketVal(d.getTotalMarketVal());
		} else {
			try {
				JSONObject js = tushareSpider.getStockDaliyBasic(TushareSpider.formatCode(code), zfEndDate + "");
				JSONArray arr = js.getJSONObject("data").getJSONArray("items");
				int length = arr.size();
				double c = arr.getDoubleValue(length - 1);// 流通市值
				double t = arr.getDoubleValue(length - 2);// 总市值
				String s1 = CurrencyUitl.covertToString(c).replace(CurrencyUitl.YI, "").replace(CurrencyUitl.WAN, "");
				z.setCircMarketVal(Double.valueOf(s1));
				String s2 = CurrencyUitl.covertToString(t).replace(CurrencyUitl.YI, "").replace(CurrencyUitl.WAN, "");
				z.setTotalMarketVal(Double.valueOf(s2));
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

	}

	public List<ZengFaExt> getZengFaExtListWithChk() {
		int pageNum = EsQueryPageUtil.queryPage9999.getPageNum();
		int size = EsQueryPageUtil.queryPage9999.getPageSize();
		Pageable pageable = PageRequest.of(pageNum, size);
		BoolQueryBuilder bqb = QueryBuilders.boolQuery();
		bqb.must(QueryBuilders.matchPhraseQuery("selfzf", 0));
		FieldSortBuilder sort = SortBuilders.fieldSort("startDate").unmappedType("integer").order(SortOrder.DESC);
		NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder();
		SearchQuery sq = queryBuilder.withQuery(bqb).withSort(sort).withPageable(pageable).build();
		Page<ZengFaExt> page = zengFaExtDao.search(sq);
		if (page != null && !page.isEmpty()) {
			return page.getContent();
		}
		return null;
	}

	public ZengFaExt getZengFaExtById(String id) {
		BoolQueryBuilder bqb = QueryBuilders.boolQuery();
		bqb.must(QueryBuilders.matchPhraseQuery("id", id));
		NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder();
		SearchQuery sq = queryBuilder.withQuery(bqb).build();
		Page<ZengFaExt> page = zengFaExtDao.search(sq);
		if (page != null && !page.isEmpty()) {
			return page.getContent().get(0);
		}
		return null;
	}

	public ZengFa getZengFaById(String id) {
		BoolQueryBuilder bqb = QueryBuilders.boolQuery();
		bqb.must(QueryBuilders.matchPhraseQuery("id", id));
		NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder();
		SearchQuery sq = queryBuilder.withQuery(bqb).build();
		Page<ZengFa> page = zengFaDao.search(sq);
		if (page != null && !page.isEmpty()) {
			return page.getContent().get(0);
		}
		return null;
	}

	public List<ZengFa> getZengFaList(String code, String status, int endDate, EsQueryPageReq querypage) {
		int pageNum = querypage.getPageNum();
		int size = querypage.getPageSize();
		Pageable pageable = PageRequest.of(pageNum, size);
		BoolQueryBuilder bqb = QueryBuilders.boolQuery();
		if (StringUtils.isNotBlank(code)) {
			bqb.must(QueryBuilders.matchPhraseQuery("code", code));
		}
		if (endDate > 0) {
			bqb.must(QueryBuilders.rangeQuery("endDate").from(endDate));
		}
		FieldSortBuilder sort = SortBuilders.fieldSort("startDate").unmappedType("integer").order(SortOrder.DESC);
		if (StringUtils.isNotBlank(status)) {
			bqb.must(QueryBuilders.matchPhraseQuery("status", Integer.valueOf(status)));
			sort = SortBuilders.fieldSort("endDate").unmappedType("integer").order(SortOrder.DESC);
		}
		NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder();
		SearchQuery sq = queryBuilder.withQuery(bqb).withSort(sort).withPageable(pageable).build();
		Page<ZengFa> page = zengFaDao.search(sq);
		if (page != null && !page.isEmpty()) {
			return page.getContent();
		}
		return null;
	}

}
