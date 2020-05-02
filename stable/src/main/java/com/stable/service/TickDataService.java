package com.stable.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.client.transport.NoNodeAvailableException;
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

import com.google.common.util.concurrent.ListenableFuture;
import com.stable.enums.RunCycleEnum;
import com.stable.enums.RunLogBizTypeEnum;
import com.stable.enums.StockAType;
import com.stable.es.dao.base.EsDaliyBasicInfoDao;
import com.stable.es.dao.base.EsTickDataBuySellInfoDao;
import com.stable.job.MyCallable;
import com.stable.service.model.v1.UpLevel1Service;
import com.stable.utils.CurrencyUitl;
import com.stable.utils.ErrorLogFileUitl;
import com.stable.utils.LogFileUitl;
import com.stable.utils.MyRunnable;
import com.stable.utils.OSystemUtil;
import com.stable.utils.PythonCallUtil;
import com.stable.utils.TasksWorker;
import com.stable.utils.TasksWorker2nd;
import com.stable.utils.WxPushUtil;
import com.stable.vo.TickDataV1Vo;
import com.stable.vo.bus.DaliyBasicInfo;
import com.stable.vo.bus.TickDataBuySellInfo;
import com.stable.vo.http.resp.TickDataBuySellInfoResp;
import com.stable.vo.spi.req.EsQueryPageReq;
import com.stable.vo.up.strategy.ModelV1;

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
	@Autowired
	private UpLevel1Service upLevel1Service;

	public static final Semaphore semp = new Semaphore(1);

	public String updateStartDateOnline(String startDate) {
		if (StringUtils.isNotBlank(startDate)) {
			String oldVal = daliyBasicHistroyService.startDate;
			daliyBasicHistroyService.startDate = startDate;
			log.info("startDate 从{}已经修改为:{}", oldVal, startDate);
		}
		return daliyBasicHistroyService.startDate;
	}

 	public void resetTickDataStatus() {
		EsQueryPageReq queryPage = new EsQueryPageReq();
		queryPage.setPageNum(1);
		queryPage.setPageSize(1000);
		boolean condition = true;
		int times = 1;
		do {
			Page<DaliyBasicInfo> page = daliyBasicHistroyService.queryListByCode("", "", "0", queryPage);
			if (page != null && !page.isEmpty()) {
				List<DaliyBasicInfo> wlist = page.getContent();
				List<DaliyBasicInfo> ulist = new LinkedList<DaliyBasicInfo>();
				for (DaliyBasicInfo d : wlist) {
					d.setFetchTickData(-1);
					ulist.add(d);
				}
				if (ulist.size() > 0) {
					esDaliyBasicInfoDao.saveAll(ulist);
				}
				log.info("Curr Page Num:{},ulist batch size:{}", times, ulist.size());
				times++;
			} else {
				log.info("page isEmpty ");
				condition = false;
			}
		} while (condition);
	}

	public void fetch(String code, String date, String all, boolean html, String startDate, boolean isJobSource) {
		if (StringUtils.isBlank(code) && StringUtils.isBlank(date) && StringUtils.isBlank(all)) {
			log.warn("参数为空");
			return;
		}
		try {
			boolean getLock = semp.tryAcquire(1, TimeUnit.HOURS);
			if (!getLock) {
				log.warn("No Locked");
				return;
			}
			log.info("Get Locked");
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		}

		updateStartDateOnline(startDate);

		ListenableFuture<Object> lis = TasksWorker.getInstance().getService().submit(
				new MyCallable(RunLogBizTypeEnum.TICK_DATA, RunCycleEnum.MANUAL, code + " " + date + " " + all) {
					public Object mycall() {
						boolean condition = true;
						EsQueryPageReq queryPage = new EsQueryPageReq();
						int currPage = 1;
						queryPage.setPageNum(currPage);
						queryPage.setPageSize(1000);
						String fetchTickData = null;

						// 查询模式：查询全部=需要翻页，查询剩余=就查询当前页
						boolean nextPage = false;
						if (StringUtils.isNotBlank(all) && "1".equals(all)) {
							nextPage = true;
							fetchTickData = null;
						} else {
							fetchTickData = "-1";// 剩余
						}
						do {
							Page<DaliyBasicInfo> page = daliyBasicHistroyService.queryListByCode(code, date,
									fetchTickData, queryPage);
							if (page != null && !page.isEmpty()) {
								log.info("剩余数量:{}，次数:{}", page.getTotalElements(), (page.getTotalPages() - 1));
								List<DaliyBasicInfo> list = page.getContent();
								CountDownLatch cnt = new CountDownLatch(list.size());
								int i = 0;
								for (DaliyBasicInfo d : list) {
									int index = i++;
									try {
										TasksWorker2nd.add(new MyRunnable() {
											public void running() {
												try {
													log.info("running index:{}", index);
													int fetchResult = -1;
													if (sumTickData(d, html) == 1) {
														fetchResult = 1;
													} else {
														fetchResult = 0;
													}
													if (d.getFetchTickData() != fetchResult) {
														d.setFetchTickData(fetchResult);
														batch.add(d);
													}
													saveData(false);
												} catch (Exception e) {
													e.printStackTrace();
													ErrorLogFileUitl.writeError(e, d.toString(), "", "");
													if (e instanceof NoNodeAvailableException) {
														WxPushUtil.pushSystem1("检测到ES系统异常，正在重启...");
														OSystemUtil.restart();
													}
												} finally {
													cnt.countDown();
												}
											}
										});
									} catch (Exception e) {
										e.printStackTrace();
										ErrorLogFileUitl.writeError(e, d.toString(), "", "");
									}
								}
								try {
									cnt.await();
								} catch (InterruptedException e) {
									e.printStackTrace();
								}
								if (nextPage) {
									currPage++;
									queryPage.setPageNum(currPage);
								}
							} else {
								log.info("page isEmpty ");
								condition = false;
							}
							try {
								TimeUnit.SECONDS.sleep(30);
							} catch (InterruptedException e) {
								e.printStackTrace();
							}
							log.info("PageSize=1000,condition={},fetchTickData={}", condition, fetchTickData);
							saveData(true);
						} while (condition);
						return null;
					}
				});
		try {
			log.info("等待任务执行完成");
			lis.get();
			if (isJobSource) {
				log.info("等待执行模型。。");
				upLevel1Service.runJob(0);
				log.info("等待执行模型完成");
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			semp.release();
		}
	}

	private List<DaliyBasicInfo> batch = Collections.synchronizedList(new ArrayList<DaliyBasicInfo>());
	private List<TickDataBuySellInfo> tickdataList = Collections.synchronizedList(new ArrayList<TickDataBuySellInfo>());

	private synchronized void saveData(boolean isEnd) {
		if (isEnd || (!isEnd && batch.size() >= 100)) {
			log.info("isEnd? {},saveData for update,DaliyBasicInfo.size:{}, TickDataBuySellInfo.size:{}", isEnd,
					batch.size(), tickdataList.size());
			if (tickdataList.size() > 0) {
				esTickDataBuySellInfoDao.saveAll(tickdataList);
				tickdataList = Collections.synchronizedList(new ArrayList<TickDataBuySellInfo>());
			}
			if (batch.size() > 0) {
				esDaliyBasicInfoDao.saveAll(batch);
				batch = Collections.synchronizedList(new ArrayList<DaliyBasicInfo>());
			}
		}
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

		// 程序评分>0
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
	public int sumTickData(DaliyBasicInfo base, boolean html) {
		String code = base.getCode();
		int date = base.getTrade_date();

		// ThreadsUtil.sleepRandomSecBetween1And2();
		String params = code + " " + date;
		List<String> lines = PythonCallUtil.callPythonScript(pythonFileName, params);
		// List<String> lines = PythonCallUtil.callPythonScriptByServerTickData(code,
		// date + "");
		if (lines == null || lines.isEmpty() || lines.get(0).startsWith(PythonCallUtil.EXCEPT)) {
			log.warn("getTickData：{}，未获取到数据 params：{}", code, code + " " + date);
			if (lines != null && !lines.isEmpty()) {
				log.error("Python 错误：code：{}，PythonCallUtil.EXCEPT：{}", code, lines.get(0));
				return -1;
			}
			return 0;
		}
//		ThreadsUtil.sleepRandomSecBetween1And5();

		// 获取日线交易数据
		daliyBasicHistroyService.getDailyData(base);
		lines.remove(lines.size() - 1);// 最后一条是空的
		log.info("getTickData：{}，获取到数据 date：{},数据条数:{}", code, date, lines.size());
		TickDataBuySellInfo tickdatasum = this.sumTickData(base, lines, html);
		if (tickdatasum != null) {
			tickdataList.add(tickdatasum);
		}
		// log.info(tickdatasum.toString());
		return 1;
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
		if (StringUtils.isBlank(line)) {
			return null;
		}
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

	private TickDataBuySellInfo sumTickData(DaliyBasicInfo base, List<String> lines, boolean html) {
		String code = base.getCode();
		int date = base.getTrade_date();
		TickDataBuySellInfo result = new TickDataBuySellInfo();

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

		long bt = 0;
		long st = 0;
		long ot = 0;
		double topPrice;
		double lowPrice;
		if (StockAType.KCB == StockAType.formatCode(code)) {// 科创板20%涨跌幅
			topPrice = CurrencyUitl.topPrice20(base.getYesterdayPrice());
			lowPrice = CurrencyUitl.lowestPrice20(base.getYesterdayPrice());
		} else {
			boolean isST = stockBasicService.getCodeName(code).contains("ST");
			topPrice = CurrencyUitl.topPrice(base.getYesterdayPrice(), isST);
			lowPrice = CurrencyUitl.lowestPrice(base.getYesterdayPrice(), isST);
		}

		// 涨停：买入盘多，卖出算中性
		// 跌停：卖出盘多，买入算中性
		for (String line : lines) {
			TickData td = getDataObject(line);
			if (td != null) {
				if ("S".equals(td.getType())) {
					sm.put(td.getTime(), td);
					sl.add(td);

					sv += Long.valueOf(td.getVolume());
					sa += Long.valueOf(td.getAmount());

					if (topPrice > 0 && td.getPrice() >= topPrice) {// 涨停：买入盘多，卖出算中性
						ot++;
					} else {
						st++;
					}
				} else if ("B".equals(td.getType())) {
					bm.put(td.getTime(), td);
					bl.add(td);

					bv += Long.valueOf(td.getVolume());
					ba += Long.valueOf(td.getAmount());

					if (lowPrice > 0 && td.getPrice() <= lowPrice) {// 跌停：卖出盘多，买入算中性
						ot++;
					} else {
						bt++;
					}
				} else {
					sm.put(td.getTime(), td);
					bm.put(td.getTime(), td);

					sl.add(td);
					bl.add(td);

					nv += Long.valueOf(td.getVolume());
					na += Long.valueOf(td.getAmount());
					ot++;
				}
				am.put(td.getTime(), td);
				al.add(td);
			}
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
		result.setSellTimes(st);
		result.setBuyTimes(bt);
		result.setOtherTimes(ot);
		result.setKey();
		// log.info("买入量:{},卖出量:{},中性量:{},总量:{}", bv, sv, nv, result.getTotalVol());

		// 程序单check
		boolean needChkProgam = needChkProgam(base);
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

			if (html && rate > 0) {
				printDetailToHtml(code, date, setListsa, setListss, setListsb);
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

	// 2交易方向:次数和差值:3/5/10/20/120/250天
	// 3程序单:次数:3/5/10/20/120/250天
	public void tickDataCheck(ModelV1 mv1, TickDataV1Vo wv, EsQueryPageReq queryPage) {
		String code = mv1.getCode();
		List<TickDataBuySellInfo> list = this.list(code, null, null, queryPage);

		// check-3
		int wayTimes3 = 0;
		int index = 3;
		long wayDef3 = 0;
		int pgmTimes3 = 0;
		for (int i = 0; i < index; i++) {
			TickDataBuySellInfo db = list.get(i);
			if (db.getBuyTimes() > db.getSellTimes()) {
				wayTimes3++;
			}
			wayDef3 += (db.getBuyTimes() - db.getSellTimes());
			if (db.getProgramRate() > 0) {
				pgmTimes3++;
			}
		}
		wv.setWayTimes3(wayTimes3);
		wv.setWayDef3(wayDef3);
		wv.setPgmTimes3(pgmTimes3);
		// check-5
		int wayTimes5 = 0;
		index = 5;
		long wayDef5 = 0;
		int pgmTimes5 = 0;
		for (int i = 0; i < index; i++) {
			TickDataBuySellInfo db = list.get(i);
			if (db.getBuyTimes() > db.getSellTimes()) {
				wayTimes5++;
			}
			wayDef5 += (db.getBuyTimes() - db.getSellTimes());
			if (db.getProgramRate() > 0) {
				pgmTimes5++;
			}
		}
		wv.setWayTimes5(wayTimes5);
		wv.setWayDef5(wayDef5);
		wv.setPgmTimes5(pgmTimes5);
		// check-10
		int wayTimes10 = 0;
		index = 10;
		long wayDef10 = 0;
		int pgmTimes10 = 0;
		for (int i = 0; i < index; i++) {
			TickDataBuySellInfo db = list.get(i);
			if (db.getBuyTimes() > db.getSellTimes()) {
				wayTimes10++;
			}
			wayDef10 += (db.getBuyTimes() - db.getSellTimes());
			if (db.getProgramRate() > 0) {
				pgmTimes10++;
			}
		}
		wv.setWayTimes10(wayTimes10);
		wv.setWayDef10(wayDef10);
		wv.setPgmTimes10(pgmTimes10);

		// check-20
		int wayTimes20 = 0;
		index = 20;
		long wayDef20 = 0;
		int pgmTimes20 = 0;
		for (int i = 0; i < index; i++) {
			TickDataBuySellInfo db = list.get(i);
			if (db.getBuyTimes() > db.getSellTimes()) {
				wayTimes20++;
			}
			wayDef20 += (db.getBuyTimes() - db.getSellTimes());
			if (db.getProgramRate() > 0) {
				pgmTimes20++;
			}
		}
		wv.setWayTimes20(wayTimes20);
		wv.setWayDef20(wayDef20);
		wv.setPgmTimes20(pgmTimes20);
		// check-120

		if (list.size() < 120) {
			return;
		}
		index = 120;
		int wayTimes120 = 0;
		long wayDef120 = 0;
		int pgmTimes120 = 0;
		for (int i = 0; i < index; i++) {
			TickDataBuySellInfo db = list.get(i);
			if (db.getBuyTimes() > db.getSellTimes()) {
				wayTimes120++;
			}
			wayDef120 += (db.getBuyTimes() - db.getSellTimes());
			if (db.getProgramRate() > 0) {
				pgmTimes120++;
			}
		}
		wv.setWayTimes120(wayTimes120);
		wv.setWayDef120(wayDef120);
		wv.setPgmTimes120(pgmTimes120);
		// check-250
		int wayTimes250 = 0;
		index = list.size();
		long wayDef250 = 0;
		int pgmTimes250 = 0;
		for (int i = 0; i < index; i++) {
			TickDataBuySellInfo db = list.get(i);
			if (db.getBuyTimes() > db.getSellTimes()) {
				wayTimes250++;
			}
			wayDef250 += (db.getBuyTimes() - db.getSellTimes());
			if (db.getProgramRate() > 0) {
				pgmTimes250++;
			}
		}
		wv.setWayTimes250(wayTimes250);
		wv.setWayDef250(wayDef250);
		wv.setPgmTimes250(pgmTimes250);

		getWayRes(mv1, wv);
	}

	private void getWayRes(ModelV1 mv1, TickDataV1Vo wv) {
		// 短期强势
		int sortWay = 0;
		boolean s3 = (wv.getWayDef3() > 0 && wv.getWayTimes3() > 0);// TODO way def
		boolean s5 = (wv.getWayDef5() > 0 && wv.getWayTimes5() > 0);
		if (s3 || s5) {
			sortWay = 1;
		}
		if (s3 && s5) {
			sortWay = 3;
		}
		// 中期强势
		int midWay = 0;
		boolean m10 = (wv.getWayDef10() > 0 && wv.getWayTimes10() > 0);// TODO way def
		boolean m20 = (wv.getWayDef20() > 0 && wv.getWayTimes20() > 0);
		if (m10 || m20) {
			midWay = 1;
		}
		if (m10 && m20) {
			midWay = 3;
		}
		// 长期强势
		int lngWay = 0;
		boolean m120 = (wv.getWayDef120() > 0 && wv.getWayTimes120() > 0);// TODO way def
		boolean m250 = (wv.getWayDef250() > 0 && wv.getWayTimes250() > 0);
		if (m120 || m250) {
			lngWay = 1;
		}
		if (m120 && m250) {
			lngWay = 3;
		}

		mv1.setSortWay(sortWay);
		mv1.setMidWay(midWay);
		mv1.setLngWay(lngWay);

		// 短期
		int sortPgm = 0;
		boolean p3 = (wv.getPgmTimes3() > 0);
		boolean p5 = (wv.getPgmTimes5() > 0);

		if (p3 || p5) {
			sortPgm = 1;
		}
		if (p3 && p5) {
			sortPgm = 3;
		}

		// 短期
		int midPgm = 0;
		boolean p10 = (wv.getPgmTimes10() > 0);
		boolean p20 = (wv.getPgmTimes20() > 0);

		if (p10 || p20) {
			midPgm = 1;
		}
		if (p10 && p20) {
			midPgm = 3;
		}

		// 短期
		int lngPgm = 0;
		boolean p120 = (wv.getPgmTimes120() > 0);
		boolean p250 = (wv.getPgmTimes250() > 0);

		if (p120 || p250) {
			lngPgm = 1;
		}
		if (p120 && p250) {
			lngPgm = 3;
		}

		mv1.setSortPgm(sortPgm);
		mv1.setMidPgm(midPgm);
		mv1.setLngPgm(lngPgm);
	}
}
