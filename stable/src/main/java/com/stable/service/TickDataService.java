package com.stable.service;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.sort.FieldSortBuilder;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.data.elasticsearch.core.query.SearchQuery;
import org.springframework.stereotype.Service;

import com.stable.enums.RunCycleEnum;
import com.stable.enums.RunLogBizTypeEnum;
import com.stable.es.dao.base.EsDaliyBasicInfoDao;
import com.stable.es.dao.base.EsTickDataBuySellInfoDao;
import com.stable.job.MyCallable;
import com.stable.utils.CurrencyUitl;
import com.stable.utils.DateUtil;
import com.stable.utils.LogFileUitl;
import com.stable.utils.PythonCallUtil;
import com.stable.utils.TasksWorker;
import com.stable.utils.TheadUtil;
import com.stable.vo.bus.DaliyBasicInfo;
import com.stable.vo.bus.TickDataBuySellInfo;
import com.stable.vo.http.resp.TickDataBuySellInfoResp;
import com.stable.vo.spi.req.EsQueryPageReq;

import lombok.Data;
import lombok.extern.log4j.Log4j2;

@Service
@Log4j2
public class TickDataService {

	@Value("${python.file.market.date.tick}")
	private String pythonFileName;

	@Value("${program.html.folder}")
	private String programHtmlFolder;
	@Autowired
	private EsDaliyBasicInfoDao esDaliyBasicInfoDao;
	@Autowired
	private EsTickDataBuySellInfoDao esTickDataBuySellInfoDao;
	@Autowired
	private DaliyBasicHistroyService daliyBasicHistroyService;
	@Autowired
	private StockBasicService stockBasicService;

	public void fetch(String code, String date, String all) {
		if (StringUtils.isBlank(code) && StringUtils.isBlank(date) && StringUtils.isBlank(all)) {
			log.warn("参数为空");
			return;
		}

		TasksWorker.getInstance().getService().submit(
				new MyCallable(RunLogBizTypeEnum.TICK_DATA, RunCycleEnum.MANUAL, code + " " + date + " " + all) {
					public Object mycall() {
						boolean condition = true;
						EsQueryPageReq queryPage = new EsQueryPageReq();
						queryPage.setPageNum(0);
						queryPage.setPageSize(1000);
						String fetchTickData = null;
						if (StringUtils.isNotBlank(all) && "1".equals(all)) {

						} else {
							fetchTickData = "-1";// 剩余
						}
						int fetchResult = -1;
						do {
							Page<DaliyBasicInfo> page = daliyBasicHistroyService.queryListByCode(code, date,
									fetchTickData, queryPage);
							if (page != null && !page.isEmpty()) {
								log.info("剩余数量:{}，次数:{}", page.getTotalElements(), (page.getTotalPages() - 1));
								List<DaliyBasicInfo> list = page.getContent();
								int index = 0;
								for (DaliyBasicInfo d : list) {
									if (sumTickData(d) != null) {
										fetchResult = 1;
									} else {
										fetchResult = 0;
									}
									if (d.getFetchTickData() != fetchResult) {
										d.setFetchTickData(fetchResult);
										esDaliyBasicInfoDao.save(d);
									}
									index++;
									log.info("esDaliyBasicInfoDao update,index:{},data:{}", index, d.toString());
								}
							} else {
								log.info("page isEmpty ");
								condition = false;
							}
							log.info("PageSize=1000,condition={},fetchTickData={}", condition, fetchTickData);
						} while (condition);
						return null;
					}
				});
	}

	public List<TickDataBuySellInfoResp> listForWebPage(String code, String date, String programRate,
			EsQueryPageReq queryPage) {
		List<TickDataBuySellInfoResp> res = new LinkedList<TickDataBuySellInfoResp>();
		List<TickDataBuySellInfo> list = this.list(code, date, programRate, queryPage);
		if (list != null) {
			for (TickDataBuySellInfo dh : list) {
				TickDataBuySellInfoResp resp = new TickDataBuySellInfoResp();
				BeanUtils.copyProperties(dh, resp);
				resp.setCodeName(stockBasicService.getCodeName(dh.getCode()));
				resp.setBuyTotalAmt1(CurrencyUitl.covertToString(dh.getBuyTotalAmt()));
				resp.setSellTotalAmt1(CurrencyUitl.covertToString(dh.getSellTotalAmt()));
				resp.setTotalAmt1(CurrencyUitl.covertToString(dh.getTotalAmt()));
				res.add(resp);
			}
		}
		return res;
	}

	public List<TickDataBuySellInfo> list(String code, String date, String programRate, EsQueryPageReq queryPage) {
		int pageNum = queryPage.getPageNum();
		int size = queryPage.getPageSize();
		log.info("queryPage code={},date={},programRate={},pageNum={},size={}", code, date, programRate, pageNum, size);
		Pageable pageable = PageRequest.of(pageNum, size);
		FieldSortBuilder sort = null;
		BoolQueryBuilder bqb = QueryBuilders.boolQuery();
		if (StringUtils.isNotBlank(code)) {
			bqb.must(QueryBuilders.matchPhraseQuery("code", code));
			sort = SortBuilders.fieldSort("date").unmappedType("integer").order(SortOrder.DESC);
		}
		if (StringUtils.isNotBlank(date)) {
			bqb.must(QueryBuilders.matchPhraseQuery("date", Integer.valueOf(date)));
			sort = SortBuilders.fieldSort("programRate").unmappedType("integer").order(SortOrder.DESC);
		}

		if (StringUtils.isNotBlank(programRate)) {
			bqb.must(QueryBuilders.rangeQuery("programRate").gt(Integer.valueOf(0)));
		}

		if (sort != null) {
			NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder();
			SearchQuery sq = queryBuilder.withQuery(bqb).withSort(sort).withPageable(pageable).build();

			Page<TickDataBuySellInfo> page = esTickDataBuySellInfoDao.search(sq);
			if (page != null && !page.isEmpty()) {
				return page.getContent();
			}
		}
		return null;
	}

	/**
	 * 统计每天
	 */
	public TickDataBuySellInfo sumTickData(DaliyBasicInfo base) {
		String code = base.getCode();
		int date = base.getTrade_date();
		List<String> lines = this.getTickData(code, DateUtil.convertDate(date + ""));
		if (lines != null && lines.size() > 0) {
			TickDataBuySellInfo tickdatasum = this.sumTickData(base, lines);
			esTickDataBuySellInfoDao.save(tickdatasum);
			log.info(tickdatasum.toString());
			return tickdatasum;
		} else {
			// ErrorLogFileUitl.writeError(null, "没用找到分笔数据", "base 信息:", base.toString());
			// NoTickDataLogFileUitl.writeLog(date + "", code + " " + date);
		}
		return null;
	}

	private List<String> getTickData(String code, String date) {
		TheadUtil.sleepRandomSecBetween1And5();
		String params = code + " " + date;
		List<String> lines = PythonCallUtil.callPythonScript(pythonFileName, params);
		if (lines == null || lines.isEmpty() || lines.get(0).startsWith(PythonCallUtil.EXCEPT)) {
			log.warn("getTickData：{}，未获取到数据 params：{}", code, params);
			if (lines != null && !lines.isEmpty()) {
				log.error("Python 错误：code：{}，PythonCallUtil.EXCEPT：{}", code, lines.get(0));
			}
			return null;
		}
		log.info("getTickData：{}，获取到数据 date：{},数据条数:{}", code, date, lines.size());
		return lines;
	}

	public List<String> test1() {
		this.pythonFileName = "E:\\pythonworkspace\\tushareTickDataFb.py";
		List<String> list = this.getTickData("002587", "2020-01-10");
		return list;
	}

	@Data
	class BatchList {
		private int sec;
		private List<TickData> list;
	}

	@Data
	class TickData {
		private String time;
		private double price;
		private double change;
		private long volume;
		private long amount;
		private String type;

		public String getSecV1() {
			return time.split(":")[2];
		}
	}

	private TickData getDataObject(String line) {
		String str = line.trim().substring(1);
		String[] fv = str.split(",");
		TickData td = new TickData();
		td.setTime(fv[0]);
		td.setPrice(Double.valueOf(fv[1]));
		td.setChange(Double.valueOf(fv[2]));
		td.setVolume(Double.valueOf(fv[3]).longValue());
		td.setAmount(Double.valueOf(fv[4]).longValue());
		td.setType(fv[5]);
		return td;
	}

	private String getRateInt(int size) {
		if (size < 10) {
			return "0" + size;
		}
		if (size > 100) {
			return "99";
		}
		return size + "";
	}

	// 流通市值小于500亿
	private boolean needChkProgam(DaliyBasicInfo base) {
		if (base.getCirc_mv() < 5000000) {
			return true;
		}
		return false;
	}

	private TickDataBuySellInfo sumTickData(DaliyBasicInfo base, List<String> lines) {
		String code = base.getCode();
		int date = base.getTrade_date();
		TickDataBuySellInfo result = new TickDataBuySellInfo();

		// 程序单check
		boolean needChkProgam = needChkProgam(base);

		Map<String, TickData> bm = new HashMap<String, TickData>();
		Map<String, TickData> sm = new HashMap<String, TickData>();
		Map<String, TickData> am = new HashMap<String, TickData>();
		List<TickData> bl = new LinkedList<TickData>();
		List<TickData> sl = new LinkedList<TickData>();
		List<TickData> al = new LinkedList<TickData>();

		long sv = 0;
		long bv = 0;
		long nv = 0;
		long sa = 0;
		long ba = 0;
		long na = 0;
		for (String line : lines) {
			TickData td = getDataObject(line);
			if ("S".equals(td.getType())) {
				sm.put(td.getTime(), td);
				sl.add(td);

				sv += Long.valueOf(td.getVolume());
				sa += Long.valueOf(td.getAmount());
			} else if ("B".equals(td.getType())) {
				bm.put(td.getTime(), td);
				bl.add(td);

				bv += Long.valueOf(td.getVolume());
				ba += Long.valueOf(td.getAmount());
			} else {
				sm.put(td.getTime(), td);
				bm.put(td.getTime(), td);

				sl.add(td);
				bl.add(td);

				nv += Long.valueOf(td.getVolume());
				na += Long.valueOf(td.getAmount());
			}
			am.put(td.getTime(), td);
			al.add(td);
		}
		result.setBuyTotalAmt(ba);
		result.setBuyTotalVol(bv);
		result.setCode(code);
		result.setDate(date);
		result.setOtherTotalAmt(na);
		result.setOtherTotalVol(nv);
		result.setSellTotalAmt(sa);
		result.setSellTotalVol(sv);
		result.setTotalAmt(ba + sa + na);
		result.setTotalVol(bv + sv + nv);
		result.setKey();
		// log.info("买入量:{},卖出量:{},中性量:{},总量:{}", bv, sv, nv, result.getTotalVol());

		int rate = 0;// -1:不需要检查，0：未检查到，>0可信度
		if (needChkProgam) {
			// all
			Set<BatchList> setListsa = new HashSet<BatchList>();
			for (TickData td : al) {
				SecFrom2to60(td, am, al, setListsa);
			}
			if (setListsa.size() > 0) {
				rate = rate + Integer.valueOf("1" + getRateInt(setListsa.size()));
			}

			// SELL
			Set<BatchList> setListss = new HashSet<BatchList>();
			for (TickData td : sl) {
				SecFrom2to60(td, sm, sl, setListss);
			}
			if (setListss.size() > 0) {
				rate = rate + Integer.valueOf("2" + getRateInt(setListss.size()));
			}

			// Buy
			Set<BatchList> setListsb = new HashSet<BatchList>();
			for (TickData td : bl) {
				SecFrom2to60(td, bm, bl, setListsb);
			}
			if (setListsb.size() > 0) {
				rate = rate + Integer.valueOf("2" + getRateInt(setListsb.size()));
			}

			if (rate > 0) {
				printDetailToHtml(code, date, setListsa, setListss, setListsb);
			} else {
				rate = 0;
			}
		} else {
			rate = -1;
		}
		result.setProgramRate(rate);
		return result;
	}

	private final String systemNextLine = System.getProperty("line.separator");
	private final String htmlNextLine = "<br/>";

	private void printDetailToHtml(String code, int date, Set<BatchList> setListsa, Set<BatchList> setListss,
			Set<BatchList> setListsb) {
		StringBuffer sb = new StringBuffer();
		sb.append("=================================").append(htmlNextLine).append(systemNextLine);
		sb.append("===========    ALL  =============").append(htmlNextLine).append(systemNextLine);
		sb.append("=================================").append(htmlNextLine).append(systemNextLine);
		for (BatchList l : setListsa) {
			sb.append("=================================").append(l.getSec()).append(htmlNextLine)
					.append(systemNextLine);
			for (TickData t : l.getList()) {
				sb.append(t).append(htmlNextLine).append(systemNextLine);
			}
		}
		sb.append("=================================").append(htmlNextLine).append(systemNextLine);
		sb.append("===========    SELL  ============").append(htmlNextLine).append(systemNextLine);
		sb.append("=================================").append(htmlNextLine).append(systemNextLine);
		for (BatchList l : setListss) {
			sb.append("=================================").append(l.getSec()).append(htmlNextLine)
					.append(systemNextLine);
			for (TickData t : l.getList()) {
				sb.append(t).append(htmlNextLine).append(systemNextLine);
			}
		}
		sb.append("=================================").append(htmlNextLine).append(systemNextLine);
		sb.append("===========    Buy   ============").append(htmlNextLine).append(systemNextLine);
		sb.append("=================================").append(htmlNextLine).append(systemNextLine);
		for (BatchList l : setListsb) {
			sb.append("=================================").append(l.getSec()).append(htmlNextLine)
					.append(systemNextLine);
			for (TickData t : l.getList()) {
				sb.append(t).append(htmlNextLine).append(systemNextLine);
			}
		}
		LogFileUitl.writeLog(programHtmlFolder + "/" + code + "/" + date + ".html", sb.toString());
	}

	/**
	 * 每1秒，每2秒，每3秒，每4秒，每5秒****每60秒的的连续间隔 <br />
	 * checkline：最多间断次数
	 */
	private void SecFrom2to60(TickData td, Map<String, TickData> m, List<TickData> all, Set<BatchList> setLists) {
		int checkline = 0;
		for (int i = 1; i <= 60; i++) {
			LinkedList<TickData> subl = new LinkedList<TickData>();

			int hasNoRecords = 0;
			TimeFormat timef = new TimeFormat(td.getTime());
			subl.add(td);
			String time;
			if (i > 30) {
				checkline = 2;
			} else {
				checkline = 3;
			}
			do {
				time = timef.add(i);
				if (m.containsKey(time)) {
					subl.add(m.get(time));
					hasNoRecords = 0;// 重置
				} else {
					hasNoRecords++;
				}
			} while (hasNoRecords < checkline);

			if (isMatchCondition(subl)) {
				if (!isRepeating(setLists, subl)) {
					BatchList b = new BatchList();
					b.setSec(i);
					b.setList(subl);
					setLists.add(b);
				}
			}
		}
	}

	/**
	 * 符合筛选条件 </br>
	 * 宽松条件: 10条以上，切相同笔数有50%</br>
	 * 进一步条件: 10条以上，切相同笔数有50%，交易量大于相同笔数的量在80%以上：TODO未做</br>
	 */

	private boolean isMatchCondition(List<TickData> subl) {
		if (subl.size() >= 10 && persentCheck(subl)) {
			return true;
		}
		return false;
	}

	/**
	 * 去重
	 */
	@SuppressWarnings("unchecked")
	private boolean isRepeating(Set<BatchList> setLists, LinkedList<TickData> subl) {
		for (BatchList b : setLists) {
			List<TickData> slist = b.getList();
			LinkedList<TickData> clone = (LinkedList<TickData>) subl.clone();
			for (TickData td : slist) {
				clone.remove(td);
			}
			if (clone.size() <= 3) {
				return true;
			}
		}
		return false;
	}

	/**
	 * 相同的单超过50%
	 */
	private boolean persentCheck(List<TickData> subl) {
		Map<Long, Integer> m = new HashMap<Long, Integer>();
		for (TickData td : subl) {
			Integer cnt = m.get(td.getVolume());
			if (cnt == null) {
				m.put(td.getVolume(), Integer.valueOf(1));
			} else {
				cnt += 1;
				m.put(td.getVolume(), cnt);
			}
		}
		double persentLine = 0.5;
//		double persentLine = 0.6;
		for (Long key : m.keySet()) {
			double persent = Double.valueOf(m.get(key)) / subl.size();
			if (persent >= persentLine) {
				return true;
			}
		}
		return false;
	}

	@Data
	public class TimeFormat {
		private int hour;
		private int min;
		private int sec;

		public TimeFormat(int time) {
			String stime = String.valueOf(time);
			hour = Integer.valueOf(stime.substring(0, 2));
			min = Integer.valueOf(stime.substring(2, 4));
			sec = Integer.valueOf(stime.substring(4, 6));
		}

		public TimeFormat(String timeformat) {
			String[] ss = timeformat.split(":");
			hour = Integer.valueOf(ss[0]);
			min = Integer.valueOf(ss[1]);
			sec = Integer.valueOf(ss[2]);
		}

		public String add(int add) {
			sec += add;
			if (sec >= 60) {
				min += 1;
				sec = sec - 60;
				if (min >= 60) {
					min = 0;
					hour += 1;
				}
			}
			return getTimeDecimalFormat(hour) + ":" + getTimeDecimalFormat(min) + ":" + getTimeDecimalFormat(sec);
		}

		private String getTimeDecimalFormat(int s) {
			if (s < 10) {
				return "0" + s;
			}
			return s + "";
		}
	}

}
