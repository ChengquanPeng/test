package com.stable.service;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;

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
import com.stable.service.model.CodeModelService;
import com.stable.spider.ths.ThsAnnSpider;
import com.stable.spider.tushare.TushareSpider;
import com.stable.utils.CurrencyUitl;
import com.stable.utils.DateUtil;
import com.stable.utils.ErrorLogFileUitl;
import com.stable.utils.WxPushUtil;
import com.stable.vo.bus.DaliyBasicInfo2;
import com.stable.vo.bus.PriceLife;
import com.stable.vo.bus.ZengFa;
import com.stable.vo.bus.ZengFaDetail;
import com.stable.vo.bus.ZengFaExt;
import com.stable.vo.bus.ZengFaSummary;
import com.stable.vo.http.resp.ZengFaResp;
import com.stable.vo.spi.req.EsQueryPageReq;

import lombok.extern.log4j.Log4j2;

/**
 * 筹码-增发
 */
@Service
@Log4j2
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
	private DaliyBasicHistroyService daliyBasicHistroyService;
	@Autowired
	private TushareSpider tushareSpider;
	@Autowired
	private CodeModelService codeModelService;
	@Autowired
	private PriceLifeService priceLifeService;

	public boolean isZfDateOk(ZengFa zengfa, int agoDate) {
		if (zengfa != null && (zengfa.getEndDate() > agoDate || zengfa.getZjhDate() > agoDate)
				|| zengfa.getStartDate() > agoDate) {
			return true;
		}
		return false;
	}

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
		return getLastZengFa(code, 0);
	}

	/**
	 * 最后已实施的增发记录
	 */
	public ZengFa getLastZengFa(String code, int status) {
		BoolQueryBuilder bqb = QueryBuilders.boolQuery();
		bqb.must(QueryBuilders.matchPhraseQuery("code", code));
		if (status > 0) {
			bqb.must(QueryBuilders.matchPhraseQuery("status", status));
		}
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
	public ZengFaDetail getLastZengFaDetail(String code, int date) {
		BoolQueryBuilder bqb = QueryBuilders.boolQuery();
		bqb.must(QueryBuilders.matchPhraseQuery("code", code));
		if (date > 0) {
			bqb.must(QueryBuilders.matchPhraseQuery("date", date));
		}
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
		jobZengFaExt(isJob, -90);
	}

	public synchronized void jobZengFaExt(boolean isJob, int date) {
		int endDate = DateUtil.formatYYYYMMDDReturnInt(DateUtil.addDate(new Date(), date)); // 全部
		StringBuffer sb = new StringBuffer();
		List<ZengFa> l = getZengFaList("", ZfStatus.DONE.getCode() + "", endDate, EsQueryPageUtil.queryPage9999);
		List<ZengFaExt> zfxl = new LinkedList<ZengFaExt>();
		if (l != null) {
			log.info("List<ZengFa> size:{}", l.size());
			for (ZengFa zf : l) {
				try {
					log.info("zf code:{}", zf.getCode());
					ZengFaExt zfe = getZengFaExtById(zf.getId());
					if (!isJob) {// 手动全部执行
						zfe = null;
					}
					if (zfe == null) {
						zfe = new ZengFaExt();
						zfe.setId(zf.getId());
						zfe.setCode(zf.getCode());
						zfe.setDate(zf.getEndDate());
						String s = ThsAnnSpider.isBuyAsset(zf.getCode(), zf.getStartDate());
						if (StringUtils.isNotBlank(s)) {
							zfe.setBuy(1);
							zfe.setTitle(s);
							sb.append(zfe.getCode()).append(",");
						}
						zfe.setSelfzf(0);
						ws(zfe, zf.getEndDate(), zf.getPrice());
//						zengFaExtDao.save(zfe);
						zfxl.add(zfe);
						log.info("done:{}", zf.getCode());
					}
				} catch (Exception e) {
					ErrorLogFileUitl.writeError(e, "ZengFaExt 增发ext出错", zf.getCode(), "");
				}
			}
		} else {
			log.info("List<ZengFa> size:ooo");
		}
		log.info("zengFaExtDao size:{}", zfxl.size());
		if (zfxl.size() > 0) {
			zengFaExtDao.saveAll(zfxl);
		}
		log.info("List<ZengFa> done");
		if (sb.length() > 0) {
			WxPushUtil.pushSystem1("增发完成且是购买资产：" + sb.toString());
		}

//		ThreadsUtil.sleep(3, TimeUnit.MINUTES);
//		// 以下代码一个月后去掉，2020-01-28
//		List<ZengFaExt> exts = getZengFaExtListWithChk();
//		if (exts != null) {
//			log.info("List<ZengFaExt> exts:{}", exts.size());
//			for (ZengFaExt z : exts) {
//				try {
//					log.info("ZengFaExt code:{}", z.getCode());
//					ZengFa orig = this.getZengFaById(z.getId());
//					ws(z, orig.getEndDate());
//				} catch (Exception e) {
//					ErrorLogFileUitl.writeError(e, "ZengFaExt 是否自己人在增发出错", "", "");
//				}
//			}
//			zengFaExtDao.saveAll(exts);
//		}
		log.info("List<ZengFaExt> done");
	}

	private void ws(ZengFaExt z, int zfEndDate, double zfprice) {
		String code = z.getCode();
		if (codeModelService.getLastOneByCode2(code).getZfjjup() >= 2) {// 起码2年未涨
			PriceLife pl = priceLifeService.getPriceLife(code, zfEndDate);
			if (pl != null) {
				if (priceLifeService.priceIndex(pl, zfprice) <= 15) {// 增发价的水位
					z.setSelfzf(1);// 底部增发
				}
			}
		}

		// 国企
		z.setCompType(stockBasicService.getCode(code).getCompnayType());

		// 增发时的市值
		if (zfEndDate >= 20210125) {
			DaliyBasicInfo2 d = daliyBasicHistroyService.queryLastest(code, zfEndDate);
			if (d == null) {
				d = daliyBasicHistroyService.queryLastest(code, 0);
			}
			z.setCircMarketVal(d.getCircMarketVal());
			z.setTotalMarketVal(d.getTotalMarketVal());
		} else {
			try {
				JSONObject js = tushareSpider.getStockDaliyBasic(TushareSpider.formatCode(code), zfEndDate + "");
				JSONArray arr = js.getJSONArray("items").getJSONArray(0);
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
		FieldSortBuilder sort = SortBuilders.fieldSort("date").unmappedType("integer").order(SortOrder.DESC);
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
