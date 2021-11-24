package com.stable.service.model;

import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

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

import com.stable.constant.Constant;
import com.stable.constant.EsQueryPageUtil;
import com.stable.enums.MonitorType;
import com.stable.enums.SylType;
import com.stable.enums.ZfStatus;
import com.stable.es.dao.base.EsCodeBaseModel2Dao;
import com.stable.es.dao.base.EsCodeBaseModelHistDao;
import com.stable.es.dao.base.EsFinanceBaseInfoHyDao;
import com.stable.es.dao.base.MonitorPoolDao;
import com.stable.service.AnnouncementService;
import com.stable.service.BonusService;
import com.stable.service.ChipsService;
import com.stable.service.ChipsZfService;
import com.stable.service.ConceptService;
import com.stable.service.DaliyBasicHistroyService;
import com.stable.service.DaliyTradeHistroyService;
import com.stable.service.DzjyService;
import com.stable.service.FinanceService;
import com.stable.service.PlateService;
import com.stable.service.PriceLifeService;
import com.stable.service.StockBasicService;
import com.stable.service.TradeCalService;
import com.stable.service.ZhiYaService;
import com.stable.service.model.data.AvgService;
import com.stable.service.model.data.FinanceAnalyzer;
import com.stable.service.model.data.LineAvgPrice;
import com.stable.service.model.data.LinePrice;
import com.stable.service.monitor.MonitorPoolService;
import com.stable.spider.ths.ThsAnnSpider;
import com.stable.utils.BeanCopy;
import com.stable.utils.CurrencyUitl;
import com.stable.utils.DateUtil;
import com.stable.utils.ErrorLogFileUitl;
import com.stable.utils.WxPushUtil;
import com.stable.vo.AnnMentParamUtil;
import com.stable.vo.HolderAnalyse;
import com.stable.vo.bus.AnnouncementHist;
import com.stable.vo.bus.CodeBaseModel2;
import com.stable.vo.bus.CodeBaseModelHist;
import com.stable.vo.bus.DaliyBasicInfo2;
import com.stable.vo.bus.DzjyYiTime;
import com.stable.vo.bus.FenHong;
import com.stable.vo.bus.FinanceBaseInfo;
import com.stable.vo.bus.FinanceBaseInfoHangye;
import com.stable.vo.bus.HolderPercent;
import com.stable.vo.bus.Jiejin;
import com.stable.vo.bus.MonitorPool;
import com.stable.vo.bus.StockBaseInfo;
import com.stable.vo.bus.ZengFa;
import com.stable.vo.bus.ZengFaDetail;
import com.stable.vo.bus.ZengFaExt;
import com.stable.vo.bus.ZhiYa;
import com.stable.vo.http.req.ModelManulReq;
import com.stable.vo.http.req.ModelReq;
import com.stable.vo.http.resp.CodeBaseModelResp;
import com.stable.vo.spi.req.EsQueryPageReq;

import lombok.extern.log4j.Log4j2;

@Service
@Log4j2
public class CodeModelService {
	@Autowired
	private AnnouncementService announcementService;
	@Autowired
	private TradeCalService tradeCalService;
	@Autowired
	private StockBasicService stockBasicService;
	@Autowired
	private EsCodeBaseModel2Dao codeBaseModel2Dao;
	@Autowired
	private EsFinanceBaseInfoHyDao esFinanceBaseInfoHyDao;
	@Autowired
	private EsCodeBaseModelHistDao codeBaseModelHistDao;
	@Autowired
	private FinanceService financeService;
	@Autowired
	private ConceptService conceptService;
	@Autowired
	private PlateService plateService;
	@Autowired
	private ChipsService chipsService;
	@Autowired
	private ZhiYaService zhiYaService;
	@Autowired
	private MonitorPoolService monitorPoolService;
	@Autowired
	private DaliyTradeHistroyService daliyTradeHistroyService;
	@Autowired
	private PriceLifeService priceLifeService;
	@Autowired
	private AvgService avgService;
	@Autowired
	private SortV6Service sortV6Service;
	@Autowired
	private MonitorPoolDao monitorPoolDao;
	@Autowired
	private ChipsZfService chipsZfService;
	@Autowired
	private BonusService bonusService;
	@Autowired
	private DaliyBasicHistroyService daliyBasicHistroyService;
	@Autowired
	private DzjyService dzjyService;
	@Autowired
	private ChipsSortService chipsSortService;

	public synchronized void runJobv2(int date, boolean isweekend) {
		try {
			log.info("param date:{}", date);
			if (!tradeCalService.isOpen(date)) {
				date = tradeCalService.getPretradeDate(date);
			}
			log.info("final date:{}", date);
			runByJobv2(date, isweekend);
		} catch (Exception e) {
			e.printStackTrace();
			ErrorLogFileUitl.writeError(e, "CodeModel模型运行异常", "", "");
			WxPushUtil.pushSystem1("CodeModel模型运行异常..");
		}
	}

	private synchronized void runByJobv2(int tradeDate, boolean isweekend) {
		Date now = new Date();

		int dzdate = DateUtil.formatYYYYMMDDReturnInt(DateUtil.addDate(now, -370));// 最近1年大宗
		int currYear = DateUtil.getCurYYYY();
		int checkYear = currYear - 1;
		checkYear = currYear - 5;

		threeYearAgo = DateUtil.formatYYYYMMDDReturnInt(DateUtil.addDate(now, -1000));
		oneYearAgo = DateUtil.formatYYYYMMDDReturnInt(DateUtil.addDate(now, -370));
		halfYearAgo = DateUtil.formatYYYYMMDDReturnInt(DateUtil.addDate(now, -180));
		log.info("CodeModel processing request date={}", tradeDate);
		if (!tradeCalService.isOpen(tradeDate)) {
			tradeDate = tradeCalService.getPretradeDate(tradeDate);
		}
		log.info("Actually processing request date={}", tradeDate);
		// 基本面
		List<CodeBaseModel2> listLast = new LinkedList<CodeBaseModel2>();
		List<CodeBaseModelHist> listHist = new LinkedList<CodeBaseModelHist>();
		List<StockBaseInfo> codelist = stockBasicService.getAllOnStatusListWithSort();
		// 大牛
		Map<String, MonitorPool> poolMap = monitorPoolService.getMonitorPoolMap();
		List<MonitorPool> poolList = new LinkedList<MonitorPool>();

		// 到期提醒
		StringBuffer sbc = new StringBuffer();
		// 公告提醒
		StringBuffer annc = new StringBuffer();

		Map<String, CodeBaseModel2> histMap = getALLForMap();
		for (StockBaseInfo s : codelist) {
			try {
				String code = s.getCode();
				// 监听池
				MonitorPool pool = poolMap.get(code);
				if (pool == null) {
					pool = new MonitorPool();
					pool.setCode(code);
				}
				poolList.add(pool);

				boolean onlineYear = stockBasicService.online1YearChk(code, tradeDate);
				if (!onlineYear) {
					continue;
				}
				boolean online4Year = stockBasicService.online4YearChk(code, tradeDate);
				DaliyBasicInfo2 d = daliyBasicHistroyService.queryLastest(code, 0, 0);
				if (d == null) {
					d = new DaliyBasicInfo2();
				}
				if (d.getCircMarketVal() <= 0) {
					d = daliyBasicHistroyService.queryLastest(code, 0, 1);
				}
				// 高质押
				ZhiYa zy = zhiYaService.getZhiYa(code);
				double mkv = d.getCircMarketVal();
				CodeBaseModel2 newOne = getBaseAnalyse(s, tradeDate, histMap.get(s.getCode()), listHist, d, zy);
				listLast.add(newOne);

				newOne.setTagDzPriceLow(0);
				newOne.setTagHighZyChance(0);
				// 市值
				newOne.setMkv(mkv);
				if (mkv > 0 && s.getCircZb() > 0) {
					newOne.setActMkv(CurrencyUitl.roundHalfUp(Double.valueOf(mkv * (s.getCircZb() / 100.0))));
				}
				newOne.setHolderZb(s.getHolderZb());
				// 同步-备注
				newOne.setBuyRea(pool.getRemark());

				// 人工审核是否时间到期-重置
				if (newOne.getPlst() < tradeDate) {
					if (newOne.getPls() == 1) {
						sbc.append(stockBasicService.getCodeName2(code)).append(",");
					}
					newOne.setPls(0);
					newOne.setPlst(0);
				}

				// 增发自动监听-重置
				if (newOne.getPls() != 1 && (pool.getMonitor() == MonitorType.ZengFaAuto.getCode()// 增发
						|| pool.getMonitor() == MonitorType.SMALL_AND_BEAUTIFUL.getCode()// 小而美
						|| pool.getMonitor() == MonitorType.SORT_CHIPS.getCode()// 短线-收集
						|| pool.getMonitor() == MonitorType.NO.getCode())) {// 自动监听归0
					pool.setMonitor(MonitorType.NO.getCode());
					pool.setRealtime(0);
					pool.setOffline(0);
					pool.setUpTodayChange(0);
					newOne.setMonitor(MonitorType.NO.getCode());
				}
				newOne.setCompnayType(s.getCompnayType());

				List<FinanceBaseInfo> l = financeService.getFinacesReportByYearRpt(code, EsQueryPageUtil.queryPage5);
				int c = 0;

				// 周末计算-至少N年未大涨?
				if (isweekend) {
					newOne.setZfjjup(0);
					newOne.setZfjjupStable(0);
					// if (mkv <= 100.0) {
					if (online4Year) {
						int listdate = Integer.valueOf(s.getList_date());
						newOne.setZfjjup(priceLifeService.noupYear(code, listdate));
						if (newOne.getZfjjup() >= 2) {
							newOne.setZfjjupStable(priceLifeService.noupYearstable(code, listdate));
						}
					}
					// }

					newOne.setFinOK(0);
					if (l != null) {
						c = l.size();
						for (FinanceBaseInfo f : l) {
							if (f.getGsjlr() < 0 || f.getKfjlr() < 0) {
								c--;
							}
						}
						if (c == l.size()) {
							newOne.setFinOK(1);
						}
					}

					if (bonusService.isBousOk(code, checkYear)) {
						newOne.setBousOK(1);
					} else {
						newOne.setBousOK(0);
					}
				}
				// 大宗交易超1亿
				newOne.setDzjyRct(0);
				DzjyYiTime dz = null;
				if (mkv <= 75.0) {
					dz = dzjyService.dzjyF(code, dzdate);
					if (dz != null && dz.getTotalAmt() > 9999.0) {// 1亿
						newOne.setDzjyRct(1);
						newOne.setDzjyAvgPrice(dz.getAvgPrcie());
						log.info("{} 大宗超1亿", code);
					}
				}

				// 小而美模型：未涨&&年报 && 大股东集中
				if (newOne.getZfjjup() >= 2 && mkv <= 50.0 && newOne.getHolderNumP5() >= 50) {// 流通45亿以内的
					c = l.size();
					if (l != null) {
						for (FinanceBaseInfo f : l) {
							if (f.getGsjlr() <= 0) {
								c--;
							}
						}
					}
					if (c >= (l.size() - 1)) {// 亏损最多一次
						newOne.setTagSmallAndBeatf(1);
						log.info("{} 小而美模型", code);
						if (pool.getMonitor() == MonitorType.NO.getCode()) {
							pool.setMonitor(MonitorType.SMALL_AND_BEAUTIFUL.getCode());
							pool.setOffline(1);
							pool.setUpTodayChange(8);
						}
					}
				}
				// 收集筹码的短线-拉过一波，所以市值可以大一点
				newOne.setSortChips(0);
				if (online4Year && mkv > 0 && mkv <= 100.0 && chipsSortService.isCollectChips(code, tradeDate)) {
					newOne.setSortChips(1);
					if (pool.getMonitor() == MonitorType.NO.getCode()) {
						pool.setMonitor(MonitorType.SORT_CHIPS.getCode());
						pool.setRealtime(1);
						pool.setUpTodayChange(5);
					}
					log.info("{} 主力筹码收集", code);
				}
				// 系统自动监听
				// 1.人工没确认或者确认没问题的：newOne.getPls() != 2
				// 2.未涨的
				// 3.增发解禁且未涨
				// 4.75亿以内（50x150%=75）

				if (newOne.getZfjj() > 0 && newOne.getZfjjup() >= 2 && mkv <= 75.0) {// 75亿以内的
					if (pool.getMonitor() == MonitorType.NO.getCode()) {
						pool.setMonitor(MonitorType.ZengFaAuto.getCode());
						pool.setOffline(1);
						pool.setUpTodayChange(9);
						newOne.setMonitor(MonitorType.ZengFaAuto.getCode());
						log.info("{} 增发自动监听", code);
					}

				}
				// 公告通知
				if (pool.getListenerGg() == 1) {
					if (ThsAnnSpider.getLastAnn(code) > tradeDate) {
						annc.append(stockBasicService.getCodeName2(code)).append(",");
					}
				}
				if (newOne.getDzjyRct() == 1 && dz.getAvgPrcie() > d.getClosed()) {
					newOne.setTagDzPriceLow(
							Double.valueOf(CurrencyUitl.cutProfit(d.getClosed(), dz.getAvgPrcie())).intValue());
				}
				// 高质押机会
				if (d.getClosed() < zy.getWarningLine()) {
					newOne.setTagHighZyChance(1);
				}
			} catch (Exception e) {
				ErrorLogFileUitl.writeError(e, s.getCode(), "", "");
			}
		}
		if (listLast.size() > 0) {
			codeBaseModel2Dao.saveAll(listLast);
		}
		if (listHist.size() > 0) {
			codeBaseModelHistDao.saveAll(listHist);
		}
		if (poolList.size() > 0) {
			monitorPoolDao.saveAll(poolList);
		}
//		middleSortV1Service.start(tradeDate, list);
		log.info("CodeModel v2 模型执行完成");
//		WxPushUtil.pushSystem1("CODE-MODEL V2-" + tradeDate + " 共[" + codelist.size() + "]条,今日更新条数:" + listHist.size());
		if (sbc.length() > 0) {
			WxPushUtil.pushSystem1("以下pls==1已到期:" + sbc.toString());
		}
		if (annc.length() > 0) {
			WxPushUtil.pushSystem1("最新公告:" + annc.toString());
		}
//		daliyTradeHistroyService.deleteData();
	}

	private CodeBaseModel2 getBaseAnalyse(StockBaseInfo s, int tradeDate, CodeBaseModel2 oldOne,
			List<CodeBaseModelHist> listHist, DaliyBasicInfo2 d, ZhiYa zy) {
		String code = s.getCode();
		log.info("Code Model  processing for code:{}", code);
		// 基本面池
		CodeBaseModel2 newOne = new CodeBaseModel2();
		copyProperty(newOne, oldOne);// copy原有属性
		newOne.setId(code);
		newOne.setCode(code);
		newOne.setDate(tradeDate);
		// 财务
		List<FinanceBaseInfo> fbis = financeService.getFinacesReportByLteDate(code, tradeDate,
				EsQueryPageUtil.queryPage9999);

		if (fbis == null) {
			ErrorLogFileUitl.writeError(new RuntimeException("无最新财务数据"), code, tradeDate + "", "Code Model错误");
			return newOne;
		}
		baseAnalyseColor(s, newOne, fbis, zy);// 基本面-红蓝绿
		findBigBoss2(code, newOne, fbis);// 基本面-疑似大牛
		susWhiteHorses(code, newOne);// 基本面-疑似白马//TODO白马更多细节，比如市值，基金
		lastDoneZfBoss(newOne, d);// 已完成的增发，更多细节
		// 股东人数
		HolderAnalyse ha = chipsService.holderNumAnalyse(code);
		newOne.setHolderNum(ha.getAnaRes());
		newOne.setHolderDate(ha.getDate());
		newOne.setAvgNum(ha.getAvgNum());// 除开5%股东的人均流通持股
		newOne.setLastNum(ha.getLastNum());
		HolderPercent hp = chipsService.getLastHolderPercent(code);
		newOne.setHolderNumP5(hp.getPercent5());
		newOne.setHolderNumT3(hp.getTopThree());

		newOne.setZfjj(0);
		newOne.setZfjjDate(0);
		newOne.setSortMode7(0);
		newOne.setSortMode6(0);

		sortModel(newOne);// 短线模型
		zfjj(newOne);// 限售解禁
		saveHist(newOne, oldOne, listHist);// 历史
		return newOne;
	}

	private void sortModel(CodeBaseModel2 newOne) {
		String code = newOne.getCode();
		int tradeDate = newOne.getDate();

		// 短线模型6
		if (sortV6Service.isWhiteHorseForSortV6(sortV6Service.is15DayTodayPriceOk(code, tradeDate))) {
			newOne.setSortMode6(1);
		}
		// 短线模型7（箱体震荡新高，是否有波浪走势）
		if (sortV6Service.isWhiteHorseForSortV7(code, tradeDate)) {
			newOne.setSortMode7(1);
		}
	}

	// 增发
	private void chkLastOneYearZf(CodeBaseModel2 newOne) {
		newOne.setZfStatus(ZfStatus.NO.getCode());
		newOne.setZfStatusDesc("");
		newOne.setZfYjAmt(0);
		newOne.setZfPrice(0.0);
		ZengFa last = chipsZfService.getLastZengFa(newOne.getCode());
		// start 一年以前
		if (chipsZfService.isZfDateOk(last, oneYearAgo)) {
			newOne.setZfStatus(last.getStatus());
			newOne.setZfStatusDesc(last.getStatusDesc());
			newOne.setZfYjAmt(last.getYjamt());
			newOne.setZfPrice(last.getPrice());
		}
	}

	private void zfjj(CodeBaseModel2 newOne) {
		String code = newOne.getCode();
		int d = chipsService.getRecentlyZfJiejin(code);
		if (d > 0) {
			newOne.setZfjjDate(d);
			newOne.setZfjj(1);
		}
	}

	private void lastDoneZfBoss(CodeBaseModel2 newOne, DaliyBasicInfo2 d) {
		newOne.setZfself(0);
		newOne.setGsz(0);
		newOne.setZflastOkDate(0);
		// 低于增发价
		newOne.setZfPriceLow(0);
		newOne.setZfObjType(0);

		String code = newOne.getCode();
		ZengFa zf = chipsZfService.getLastZengFa(code, ZfStatus.DONE.getCode());// 已完成的增发
		if (chipsZfService.isZfDateOk(zf, oneYearAgo)) {
			newOne.setZflastOkDate(zf.getEndDate());
			ZengFaExt zfe = chipsZfService.getZengFaExtById(zf.getId());
			if (zfe != null) {
				newOne.setZfself(zfe.getSelfzf());
			}
			if (bonusService.isGsz(code, threeYearAgo)) {
				newOne.setGsz(1);
			}
			// 一年的之中的增发(低于增发价)
			if (chipsZfService.isZfDateOk(zf, oneYearAgo)) {
				if (zf.getPrice() > 0 && zf.getPrice() > d.getClosed()) {
					newOne.setZfPriceLow(
							Double.valueOf(CurrencyUitl.cutProfit(d.getClosed(), zf.getPrice())).intValue());
				}
			}
			// 实际增发类型
			ZengFaDetail zfd = chipsZfService.getLastZengFaDetail(code, 0);
			newOne.setZfObjType(zftype(zfd.getDetails()));
		}
	}

	private String m6 = "6月";
	private String m12 = "12月";
	private String m24 = "24月";
	private String m36 = "36月";

	private int zftype(String str) {
		if (str == null) {
			return 0;
		}
		str.trim().replaceAll(" ", "");
		int c6 = 0;
		int cgt12 = 0;
		int cgt36 = 0;
		if (str.contains(m6) && isM6(str, m6)) {
			c6 = 1;
		}
		if (str.contains(m12)) {
			cgt12 = 1;
		}
		if (str.contains(m24)) {
			cgt12 = 1;
		}
		if (str.contains(m36)) {
			cgt36 = 1;
		}

		if (c6 == 1 && cgt12 == 0 && cgt36 == 0) {
			return 1;// 纯外部
		}
		if (c6 == 0 && cgt12 == 0 && cgt36 == 1) {
			return 3;// 纯大股东
		}
		if (c6 == 1 && cgt36 == 1) {
			return 2;// 内外混合6
		}
		if (c6 == 1 || cgt12 == 1 || cgt36 == 1) {
			return 4;// 其他:关联
		}
		return 0;
	}

	public static boolean isM6(String allstr, String M) {
		String pre = "";
		while (allstr.indexOf(M) >= 0) {
			String org = allstr;
			allstr = allstr.substring(allstr.indexOf(M) + M.length());
			pre = pre + org.substring(0, org.indexOf(M));

			if (pre.endsWith("3")) {
//				return false;
			} else {
				return true;
			}
			pre += M;
			// System.err.println("前面:" + pre + " 剩余:" + allstr);
		}
		return false;
//		System.err.println("count:" + count);
	}

	private void susWhiteHorses(String code, CodeBaseModel2 newOne) {
		// 是否中线(60日线),TODO,加上市值
		if (priceLifeService.getLastIndex(code) >= 80
				&& LineAvgPrice.isWhiteHorseForMidV2(avgService, code, newOne.getDate())) {
			newOne.setSusWhiteHors(1);
		} else {
			newOne.setSusWhiteHors(0);
		}
	}

	private void baseAnalyseColor(StockBaseInfo s, CodeBaseModel2 newOne, List<FinanceBaseInfo> fbis, ZhiYa zy) {
		newOne.setBaseYellow(0);
		newOne.setBaseRed(0);
		newOne.setBaseBlue(0);
		newOne.setBaseGreen(0);
		newOne.setBaseYellowDesc("");
		newOne.setBaseRedDesc("");
		newOne.setBaseBlueDesc("");
		newOne.setBaseGreenDesc("");

		String code = newOne.getCode();
		FinanceAnalyzer fa = new FinanceAnalyzer();
		for (FinanceBaseInfo fbi : fbis) {
			fa.putJidu1(fbi);
		}
		FinanceBaseInfo fbi = fa.getCurrJidu();
		newOne.setCurrYear(fbi.getYear());
		newOne.setCurrQuarter(fbi.getQuarter());
		newOne.setZcfzl(fbi.getZcfzl());
		newOne.setGsjlr(fa.getCurrYear().getGsjlr());// 年报的
		newOne.setGoodWill(fbi.getGoodWill());
		newOne.setGoodWillRatioGsjlr(fbi.getGoodWillRatioGsjlr());
		newOne.setGoodWillRatioNetAsset(fbi.getGoodWillRatioNetAsset());

		// ======== 红色警告 ========
		// ======== 黄色警告 ========
		int red = 1;
		int yellow = 1;
		StringBuffer sb1 = new StringBuffer();
		StringBuffer sb2 = new StringBuffer();
		// 退市风险
		if (fa.profitDown2Year() == 1) {
			newOne.setBaseRed(1);
			sb1.append(red++).append(".退市风险:2年利润亏损").append(Constant.HTML_LINE);
		} else if (fa.getCurrYear().getGsjlr() < 0) {
			newOne.setBaseYellow(1);
			sb2.append(yellow++).append(".年报亏损").append(Constant.HTML_LINE);
		}
		// 营收低于1亿
		if (fa.getCurrYear().getYyzsr() < CurrencyUitl.YI_N.longValue()) {
			if (fa.getCurrYear().getKfjlr() < 0) {
				newOne.setBaseRed(1);
				sb1.append(red++).append(".退市风险:年度扣非净利润为负且营收低于1亿元").append(Constant.HTML_LINE);
			} else {
				newOne.setBaseYellow(1);
				sb2.append(yellow++).append(".年度营收低于1亿元").append(Constant.HTML_LINE);
			}
		}

		// 负债超高-净资产低于应付账款
		if (fbi.getBustUpRisks() == 1) {
			newOne.setBaseYellow(1);
			sb2.append(yellow++).append(".破产风险:负债超高-净资产低于应付账款").append(Constant.HTML_LINE);

			int c = 0;
			int fort = 0;// 最近2年
			if (fbis.size() > 2) {
				for (FinanceBaseInfo ft : fbis) {
					if (ft.getBustUpRisks() == 1) {
						c++;
					}
					fort++;
					if (fort >= 3) {
						break;
					}
				}
			}
			// 连续3季度
			if (c >= 3) {
				newOne.setBaseRed(1);
				sb1.append(red++).append(".暴雷破产风险:连续3季度负债超高-净资产低于应付账款").append(Constant.HTML_LINE);
			} else {// 最近2年
				// 连续季度
				c = 0;
				fort = 0;// 最近2年
				for (FinanceBaseInfo ft : fbis) {
					fort++;
					if (fort > 8) {
						break;
					}
					if (ft.getBustUpRisks() == 1) {
						c++;
					}
				}
				if (c >= (fbis.size() / 2)) {
					newOne.setBaseRed(1);
					sb1.append(red++).append(".暴雷风险:最近" + c + "季度负债超高-净资产低于应付账款").append(Constant.HTML_LINE);
				}
			}

		}
		// 净资产
		if (fbi.getNetAsset() < 0) {
			newOne.setBaseRed(1);
			sb1.append(red++).append(".净资产为负资产").append(Constant.HTML_LINE);

			int c = 0;
			int fort = 0;// 最近2年
			if (fbis.size() > 2) {
				for (FinanceBaseInfo ft : fbis) {
					if (ft.getNetAsset() < 0) {
						c++;
					}
					fort++;
					if (fort >= 3) {
						break;
					}
				}
			}
			// 连续3季度
			if (c >= 3) {
				newOne.setBaseRed(1);
				sb1.append(red++).append(".暴雷风险:连续3季度净资产为负资产").append(Constant.HTML_LINE);
			} else {// 最近2年
				// 连续季度
				c = 0;
				fort = 0;// 最近2年
				for (FinanceBaseInfo ft : fbis) {
					fort++;
					if (fort > 8) {
						break;
					}
					if (ft.getNetAsset() < 0) {
						c++;
					}
				}
				if (c >= (fbis.size() / 2)) {
					newOne.setBaseRed(1);
					sb1.append(red++).append(".暴雷风险:最近" + c + "季度净资产为负资产").append(Constant.HTML_LINE);
				}
			}
		}
		// 流动负债高于流动资产
		if (fbi.getSumLasset() < fbi.getSumDebtLd()) {
			newOne.setBaseYellow(1);
			sb2.append(yellow++).append(".流动负债高于流动资产").append(Constant.HTML_LINE);
			int c = 0;
			int fort = 0;// 最近2年
			if (fbis.size() > 2) {
				for (FinanceBaseInfo ft : fbis) {
					if (ft.getSumLasset() < ft.getSumDebtLd()) {
						c++;
					}
					fort++;
					if (fort >= 3) {
						break;
					}
				}
			}
			// 连续3季度
			if (c >= 3) {
				newOne.setBaseRed(1);
				sb1.append(red++).append(".暴雷风险:连续3季度流动负债高于流动资产").append(Constant.HTML_LINE);
			} else {// 最近2年
				// 连续季度
				c = 0;
				fort = 0;// 最近2年
				for (FinanceBaseInfo ft : fbis) {
					fort++;
					if (fort > 8) {
						break;
					}
					if (ft.getSumLasset() < ft.getSumDebtLd()) {
						c++;
					}
				}
				if (c >= (fbis.size() / 2)) {
					newOne.setBaseRed(1);
					sb1.append(red++).append(".暴雷风险:最近" + c + "季度流动负债高于流动资产").append(Constant.HTML_LINE);
				}
			}
		}
		if (fbi.getFundNotOk3() == 1) {
			newOne.setBaseYellow(1);
			sb2.append(yellow++).append(".财务疑似三高,核查各报告期短长期借款和货币资金(现金流净额?)").append(Constant.HTML_LINE);
		}
		if (fbi.getFundNotOk() == 1) {
			newOne.setBaseYellow(1);
			sb2.append(yellow++).append(".资金紧张:流动负债高于流动/货币资金30%以上").append(Constant.HTML_LINE);
		}
		if (fbi.getFundNotOk2() == 1) {
			newOne.setBaseYellow(1);
			sb2.append(yellow++).append(".资金紧张:应付利息较高").append(Constant.HTML_LINE);

			int c = 0;
			int fort = 0;// 最近2年
			if (fbis.size() > 2) {
				for (FinanceBaseInfo ft : fbis) {
					if (ft.getFundNotOk2() == 1) {
						c++;
					}
					fort++;
					if (fort >= 3) {
						break;
					}
				}
			}
			// 连续3季度
			if (c >= 3) {
				newOne.setBaseRed(1);
				sb1.append(red++).append(".暴雷风险:连续3季度资金紧张-应付利息高").append(Constant.HTML_LINE);
			} else {// 最近2年
				// 连续季度
				c = 0;
				fort = 0;// 最近2年
				for (FinanceBaseInfo ft : fbis) {
					fort++;
					if (fort > 8) {
						break;
					}
					if (ft.getFundNotOk2() == 1) {
						c++;
					}
				}
				if (c >= (fbis.size() / 2)) {
					newOne.setBaseRed(1);
					sb1.append(red++).append(".暴雷风险:最近" + c + "季度资金紧张-应付利息高").append(Constant.HTML_LINE);
				}
			}
		}
		// 资产负债率
		if (fbi.getZcfzl() >= 80) {
			if (fbi.getZcfzl() >= 99) {
				newOne.setBaseRed(1);
				sb1.append(red++).append(".资产负债率超高:").append(fbi.getZcfzl()).append("%").append(Constant.HTML_LINE);
			} else {
				newOne.setBaseYellow(1);
				sb2.append(yellow++).append(".资产负债率高:").append(fbi.getZcfzl()).append("%").append(Constant.HTML_LINE);
			}

			int c = 0;
			int fort = 0;// 最近2年
			if (fbis.size() > 2) {
				for (FinanceBaseInfo ft : fbis) {
					if (ft.getZcfzl() >= 80) {
						c++;
					}
					fort++;
					if (fort >= 3) {
						break;
					}
				}
			}
			// 连续3季度
			if (c >= 3) {
				newOne.setBaseRed(1);
				sb1.append(red++).append(".暴雷风险:连续3季度资产负债率高").append(Constant.HTML_LINE);
			} else {// 最近2年
				// 连续季度
				c = 0;
				fort = 0;// 最近2年
				for (FinanceBaseInfo ft : fbis) {
					fort++;
					if (fort > 8) {
						break;
					}
					if (ft.getZcfzl() >= 80) {
						c++;
					}
				}
				if (c >= (fbis.size() / 2)) {
					newOne.setBaseRed(1);
					sb1.append(red++).append(".暴雷风险:最近" + c + "季度资产负债率高").append(Constant.HTML_LINE);
				}
			}
		}
		// 现金流
		if (fbi.getJyxjlce() <= 0 && fbi.getMgjyxjl() <= 0) {
			if (fbi.getKfjlr() > 0) {
				newOne.setBaseYellow(1);
				sb2.append(yellow++).append(".经营现金流入不敷出,净利存疑").append(Constant.HTML_LINE);
			} else {
				newOne.setBaseYellow(1);
				sb2.append(yellow++).append(".经营现金流入不敷出").append(Constant.HTML_LINE);
			}
		}
		if (fbi.getKfjlr() > 0 && (fbi.getJyxjlce() < 0 || fbi.getMgjyxjl() < 0)) {
			int c = 0;
			int fort = 0;// 最近2年
			if (fbis.size() > 2) {
				for (FinanceBaseInfo ft : fbis) {
					if (ft.getKfjlr() > 0 && (ft.getJyxjlce() < 0 || ft.getMgjyxjl() < 0)) {
						c++;
					}
					fort++;
					if (fort >= 3) {
						break;
					}
				}
			}
			// 连续3季度
			if (c >= 3) {
				newOne.setBaseRed(1);
				sb1.append(red++).append(".暴雷风险:连续3季度经营现金流连续为负却有扣非净利").append(Constant.HTML_LINE).append("靠融资在运转?")
						.append(Constant.HTML_LINE);
			} else {// 最近2年
				// 连续季度
				c = 0;
				fort = 0;// 最近2年
				for (FinanceBaseInfo ft : fbis) {
					fort++;
					if (fort > 8) {
						break;
					}
					if (ft.getKfjlr() > 0 && (ft.getJyxjlce() < 0 || ft.getMgjyxjl() < 0)) {
						c++;
					}
				}
				if (c >= (fbis.size() / 2)) {
					newOne.setBaseRed(1);
					sb1.append(red++).append(".暴雷风险:最近").append(c).append("季度经常现金流为负却有扣非净利").append(Constant.HTML_LINE)
							.append("靠融资在运转?").append(Constant.HTML_LINE);
				}
			}
		}

		// 应收账款
		if (fbi.getAccountrecRatio() > 0) {
			if (fbi.getAccountrecRatio() >= 50.0) {
				newOne.setBaseYellow(1);
				sb2.append(yellow++).append(".应收账款超高:").append(fbi.getAccountrecRatio()).append("%")
						.append(Constant.HTML_LINE);
			} else if (fbi.getAccountrecRatio() >= 25.0) {
				newOne.setBaseYellow(1);
				sb2.append(yellow++).append(".应收账款高:").append(fbi.getAccountrecRatio()).append("%")
						.append(Constant.HTML_LINE);
			}

			int c = 0;
			int fort = 0;// 最近2年
			if (fbis.size() > 2) {
				for (FinanceBaseInfo ft : fbis) {
					if (ft.getAccountrecRatio() >= 50.0) {
						c++;
					}
					fort++;
					if (fort >= 3) {
						break;
					}
				}
			}
			// 连续3季度
			if (c >= 3) {
				newOne.setBaseRed(1);
				sb1.append(red++).append(".暴雷风险:连续3季度应收账款高").append(Constant.HTML_LINE);
			} else {// 最近2年
				// 连续季度
				c = 0;
				fort = 0;// 最近2年
				for (FinanceBaseInfo ft : fbis) {
					fort++;
					if (fort > 8) {
						break;
					}
					if (ft.getAccountrecRatio() >= 50.0) {
						c++;
					}
				}
				if (c >= (fbis.size() / 2)) {
					newOne.setBaseRed(1);
					sb1.append(red++).append(".暴雷风险:最近" + c + "季度应收账款高").append(Constant.HTML_LINE);
				}
			}
		}
		// 行业对比:毛利，应收账款，现金流
		FinanceBaseInfoHangye hy = this.getFinanceBaseInfoHangye(code, fbi.getYear(), fbi.getQuarter());
		if (hy != null) {
			if (hy.getMll() > 0 && hy.getMll() > hy.getMllAvg() && hy.getMllRank() <= 5) {
				newOne.setBaseYellow(1);
				sb2.append(yellow++)
						.append(".毛利率:" + hy.getMll() + " 行业平均:" + hy.getMllAvg() + ", 行业排名:" + hy.getMllRank())
						.append(Constant.HTML_LINE);
			}
			if (hy.getYszk() > 0 && hy.getYszk() > hy.getYszkAvg() && hy.getYszkRank() <= 5) {
				newOne.setBaseYellow(1);
				sb2.append(yellow++)
						.append(".应收账款:" + hy.getYszk() + " 行业平均:" + hy.getYszkAvg() + ", 行业排名:" + hy.getYszkRank())
						.append(Constant.HTML_LINE);
			}
			if (hy.getXjl() < 0 && hy.getXjl() < hy.getXjlAvg() && hy.getXjlRank() <= 5) {
				newOne.setBaseYellow(1);
				sb2.append(yellow++)
						.append(".现金流:" + hy.getXjl() + " 行业平均:" + hy.getXjlAvg() + ", 行业排名:" + hy.getXjlRank())
						.append(Constant.HTML_LINE);
			}
		}
		// 商誉占比
		if (fbi.getGoodWillRatioNetAsset() > 15.0) {// 超过15%
			newOne.setBaseYellow(1);
			sb2.append(yellow++).append(".商誉占比超:" + fbi.getGoodWillRatioNetAsset() + "%").append(Constant.HTML_LINE);
		}
		// 库存占比
		if (fbi.getInventoryRatio() > 45.0) {// 超过50%
			if (s.getThsIndustry() != null && !s.getThsIndustry().contains("地产")) {// 房地产忽悠占比
				double d = fbi.getInventoryRatio();
				if (d > 90.0) {
					newOne.setBaseRed(1);
					sb1.append(red++).append(".库存净资产占比超:" + d + "%").append(Constant.HTML_LINE);
				} else {
					newOne.setBaseYellow(1);
					sb2.append(yellow++).append(".库存净资产占比超:" + d + "%").append(Constant.HTML_LINE);
				}
			}
		}
		// 股东增持（一年）
		AnnouncementHist zengchi = announcementService.getLastRecordType(code, AnnMentParamUtil.zhengchi.getType(),
				oneYearAgo);
		if (zengchi != null) {
			newOne.setBaseYellow(1);
			sb2.append(yellow++).append(".股东/高管增持:" + (zengchi.getRptDate())).append(Constant.HTML_LINE);

		}

		if (zy.getHasRisk() == 1) {//
			newOne.setBaseYellow(1);
			sb2.append(yellow++).append(".高质押风险:" + zy.getDetail()).append(Constant.HTML_LINE);
		}
		// 分红
		FenHong fh = chipsService.getFenHong(code);
		if (fh.getTimes() <= 0) {
			newOne.setBaseYellow(1);
			sb2.append(yellow++).append(".无分红记录").append(Constant.HTML_LINE);
		}
		// 限售股解禁（2年：前后1年）
		List<Jiejin> jj = chipsService.getBf2yearJiejin(code);
		if (jj != null) {
			newOne.setBaseYellow(1);
			sb2.append(yellow++).append(".前后1年解禁记录数:" + jj.size()).append(Constant.HTML_LINE);
		}

		// ======== 蓝色警告 && 绿色 ========
		StringBuffer sb3 = new StringBuffer();
		StringBuffer sb4 = new StringBuffer();
		if (fa.getCurrJidu().getYyzsrtbzz() <= 0) {
			newOne.setBaseBlue(1);
			sb3.append("营收同比下降").append(Constant.HTML_LINE);
		} else {
			newOne.setBaseGreen(1);
			sb4.append("营收同比上涨").append(Constant.HTML_LINE);
		}
		if (fa.getCurrJidu().getGsjlrtbzz() <= 0) {
			newOne.setBaseBlue(1);
			sb3.append("净利同比下降").append(Constant.HTML_LINE);
		} else {
			newOne.setBaseGreen(1);
			sb4.append("净利同比上涨").append(Constant.HTML_LINE);
		}
		evaluateStep1(newOne, fa, fbis);
		if (newOne.getSylType() == 4 || newOne.getSylType() == 1) {
			newOne.setBaseGreen(1);
			sb4.append("资产收益率上涨").append(Constant.HTML_LINE);
		} else {
			newOne.setBaseBlue(1);
			sb3.append("资产收益率下降").append(Constant.HTML_LINE);
		}
		if (newOne.getSylType() == 4 || newOne.getSylType() == 2) {
			newOne.setBaseGreen(1);
			sb4.append("资产收益率年超20%").append(Constant.HTML_LINE);
		}
		// 正在增发中
		chkLastOneYearZf(newOne);
		if (newOne.getZfStatus() == 1 || newOne.getZfStatus() == 2) {
			newOne.setBaseBlue(1);
			if (newOne.getZfStatus() == 1) {
				sb3.append("<font color='red'>");
			} else {
				sb3.append("<font color='green'>");
			}
			sb3.append("增发进度" + (s.getCompnayType() == 1 ? "(国资)" : "") + ":" + newOne.getZfStatusDesc());
			sb3.append("</font>");
			sb3.append(Constant.HTML_LINE);
		}
		// 股东减持（一年）
		AnnouncementHist jianchi = announcementService.getLastRecordType(code, AnnMentParamUtil.jianchi.getType(),
				oneYearAgo);
		if (jianchi != null) {
			newOne.setBaseBlue(1);
			sb3.append(".股东/高管减持:" + (jianchi.getRptDate())).append(Constant.HTML_LINE);
		}
		// 回购（半年内）
		AnnouncementHist huigou = announcementService.getLastRecordType(code, AnnMentParamUtil.huigou.getType(),
				halfYearAgo);
		if (huigou != null) {
			newOne.setBaseBlue(1);
			sb3.append(".回购:" + (huigou.getRptDate())).append(Constant.HTML_LINE);
		}
		// 快预报
		String ykb = financeService.getyjkb(fbi.getCode(), fbi.getYear(), fbi.getQuarter());
		if (StringUtils.isNotBlank(ykb)) {
			newOne.setBaseBlue(1);
			sb3.append(ykb).append(Constant.HTML_LINE);
		}

		if (newOne.getBaseRed() > 0) {
			newOne.setBaseRedDesc(sb1.toString());
		}
		if (newOne.getBaseYellow() > 0) {
			newOne.setBaseYellowDesc(sb2.toString());
		}
		if (newOne.getBaseBlue() > 0) {
			newOne.setBaseBlueDesc(sb3.toString());
		}
		if (newOne.getBaseGreen() > 0) {
			newOne.setBaseGreenDesc(sb4.toString());
		}
	}

	private void copyProperty(CodeBaseModel2 newOne, CodeBaseModel2 oldOne) {
		if (oldOne != null) {
			BeanCopy.copy(oldOne, newOne);
			// 复制一些属性
//			newOne.setMonitor(oldOne.getMonitor());
//			newOne.setZfjjup(oldOne.getZfjjup());
//			newOne.setPls(oldOne.getPls());
//			newOne.setPlst(oldOne.getPlst());
			// 筹码博弈

		}
	}

	private void saveHist(CodeBaseModel2 newOne, CodeBaseModel2 oldOne, List<CodeBaseModelHist> listHist) {
		// copy history
		CodeBaseModelHist hist = new CodeBaseModelHist();
		BeanCopy.copy(newOne, hist);
		hist.setId(hist.getCode() + "_" + hist.getCurrYear() + "_" + hist.getCurrQuarter());
		listHist.add(hist);
	}

	private double chkdouble = 80.0;// 10跌倒5.x

	private void findBigBoss2(String code, CodeBaseModel2 newOne, List<FinanceBaseInfo> fbis) {
		log.info("findBigBoss code:{}", code);
		// 是否符合中线、1.市盈率和ttm在50以内
//		c.setKbygjl(0);
//		c.setKbygys(0);
//		if (yjkb != null) {
//			c.setKbygys(yjkb.getYyzsrtbzz());
//			c.setKbygjl(yjkb.getJlrtbzz());
//		} else if (yjyg != null) {
//			c.setKbygjl(yjyg.getJlrtbzz());
//		}

		// 业绩连续
		int continueJidu1 = 0;
		int continueJidu2 = 0;
		boolean cj1 = true;
		int cj2 = 0;
		List<Double> high = new LinkedList<Double>();
		List<Double> high2 = new LinkedList<Double>();
		for (FinanceBaseInfo fbi : fbis) {
			if (cj1 && fbi.getYyzsrtbzz() >= 1.0 && fbi.getGsjlrtbzz() >= 1.0) {// 连续季度增长
				continueJidu1++;
				high.add(fbi.getYyzsrtbzz());
			} else {
				cj1 = false;
			}
			if (cj2 <= 1 && fbi.getYyzsrtbzz() >= 1.0 && fbi.getGsjlrtbzz() >= 1.0) {// 允许一次断连续
				continueJidu2++;
				high2.add(fbi.getYyzsrtbzz());
			} else {
				cj2++;
			}
		}
		boolean isok = false;
		if (continueJidu1 > 3 || continueJidu2 > 5) {
			if (continueJidu1 > 3) {
				int cn = 0;
				for (Double h : high) {// 连续超过25%的次数超过一半
					if (h > 25.0) {
						cn++;
					}
				}
				if (cn * 2 > continueJidu1) {
					isok = true;
				}
			} else if (continueJidu2 > 5) {
				int cn = 0;
				for (Double h : high2) {
					if (h > 25.0) {
						cn++;
					}
				}
				if (cn * 2 > continueJidu2) {
					isok = true;
				}
			}
		}
		if (isok) {
			// 1年整幅未超过80% -- //chkdouble = 80.0;// 10跌倒5.x
			if (LinePrice.priceCheckForMid(daliyTradeHistroyService, code, newOne.getDate(), chkdouble)) {
				newOne.setSusBigBoss(1);
			}
		} else {
			newOne.setSusBigBoss(0);
		}
	}

	private int threeYearAgo = 0;// 三年以前
	private int oneYearAgo = 0;// 一年以前
	private int halfYearAgo = 0;// 半年以前

	private void evaluateStep1(CodeBaseModel2 newOne, FinanceAnalyzer fa, List<FinanceBaseInfo> fbis) {
		FinanceBaseInfo currJidu = fa.getCurrJidu();
		newOne.setSyl(currJidu.getJqjzcsyl());
		newOne.setSylttm(plateService.getSylTtm(fbis));
		newOne.setSyldjd(plateService.getSyldjd(currJidu));// 单季度？
		newOne.setSylType(0);
		if (newOne.getSyldjd() > newOne.getSylttm()) {
			newOne.setSylType(1);// 自身收益率增长
		}
		if (newOne.getSylttm() >= 5.0) {// 单季度5%，全年20%
			if (newOne.getSylType() == 1) {
				newOne.setSylType(4);// 同时
			} else {
				newOne.setSylType(2);// 年收益率超过5.0%*4=20%
			}
		}
	}

	public CodeBaseModel2 getLastOneByCode2(String code) {
		log.info("getLastOneByCode:{}", code);
		BoolQueryBuilder bqb = QueryBuilders.boolQuery();
		bqb.must(QueryBuilders.matchPhraseQuery("id", code));
		NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder();
		SearchQuery sq = queryBuilder.withQuery(bqb).build();

		Page<CodeBaseModel2> page = codeBaseModel2Dao.search(sq);
		if (page != null && !page.isEmpty()) {
			return page.getContent().get(0);
		}
		CodeBaseModel2 cbm = new CodeBaseModel2();
		cbm.setCode(code);
		return cbm;
	}

	public Map<String, CodeBaseModel2> getALLForMap() {
		List<CodeBaseModel2> list = getALLForList();
		Map<String, CodeBaseModel2> map = new HashMap<String, CodeBaseModel2>();
		if (list != null) {
			for (CodeBaseModel2 c : list) {
				map.put(c.getCode(), c);
			}
		}
		return map;
	}

	private List<CodeBaseModel2> getALLForList() {
		EsQueryPageReq querypage = EsQueryPageUtil.queryPage9999;
		BoolQueryBuilder bqb = QueryBuilders.boolQuery();

		NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder();
		Pageable pageable = PageRequest.of(querypage.getPageNum(), querypage.getPageSize());
		SearchQuery sq = queryBuilder.withQuery(bqb).withPageable(pageable).build();

		Page<CodeBaseModel2> page = codeBaseModel2Dao.search(sq);
		if (page != null && !page.isEmpty()) {
			return page.getContent();
		}
		log.info("no records CodeBaseModels");
		return null;
	}

	public List<CodeBaseModelHist> getListByCode(String code, EsQueryPageReq querypage) {
		log.info("getListByCode:{}", code);
		BoolQueryBuilder bqb = QueryBuilders.boolQuery();
		bqb.must(QueryBuilders.matchPhraseQuery("code", code));
		FieldSortBuilder sort = SortBuilders.fieldSort("date").unmappedType("integer").order(SortOrder.DESC);

		NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder();
		Pageable pageable = PageRequest.of(querypage.getPageNum(), querypage.getPageSize());
		SearchQuery sq = queryBuilder.withQuery(bqb).withPageable(pageable).withSort(sort).build();

		Page<CodeBaseModelHist> page = codeBaseModelHistDao.search(sq);
		if (page != null && !page.isEmpty()) {
			return page.getContent();
		}
		log.info("no records CodeBaseModelHists");
		return null;
	}

	private FinanceBaseInfoHangye getFinanceBaseInfoHangye(String code, int year, int quarter) {
		BoolQueryBuilder bqb = QueryBuilders.boolQuery();
		bqb.must(QueryBuilders.matchPhraseQuery("code", code));
		bqb.must(QueryBuilders.matchPhraseQuery("year", year));
		bqb.must(QueryBuilders.matchPhraseQuery("quarter", quarter));

		NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder();
		SearchQuery sq = queryBuilder.withQuery(bqb).build();

		Page<FinanceBaseInfoHangye> page = esFinanceBaseInfoHyDao.search(sq);
		if (page != null && !page.isEmpty()) {
			return page.getContent().get(0);
		} else {
//			bqb = QueryBuilders.boolQuery();
//			bqb.must(QueryBuilders.matchPhraseQuery("code", code));
//			queryBuilder = new NativeSearchQueryBuilder();
//			FieldSortBuilder sort = SortBuilders.fieldSort("updateDate").unmappedType("integer").order(SortOrder.DESC);
//			sq = queryBuilder.withQuery(bqb).withSort(sort).build();
//			page = esFinanceBaseInfoHyDao.search(sq);
//			if (page != null && !page.isEmpty()) {
//				return page.getContent().get(0);
//			}
		}
		return null;
	}

	public List<CodeBaseModel2> getList(ModelReq mr, EsQueryPageReq querypage) {
		BoolQueryBuilder bqb = QueryBuilders.boolQuery();
		if (StringUtils.isNotBlank(mr.getCode())) {
			String[] cc = mr.getCode().split(",");
			if (cc.length == 1) {
				bqb.must(QueryBuilders.matchPhraseQuery("code", cc[0]));
			} else {
				bqb.must(QueryBuilders.termsQuery("code", cc));
			}
		} else if (StringUtils.isNotBlank(mr.getConceptId())) {
			List<String> list = conceptService.listCodesByAliasCode(mr.getConceptId());
			if (list != null) {
				bqb.must(QueryBuilders.termsQuery("code", list));
			}
		} else if (StringUtils.isNotBlank(mr.getConceptName())) {
			List<String> list = listCodeByCodeConceptName(mr.getConceptName());
			if (list.size() > 0) {
				bqb.must(QueryBuilders.termsQuery("code", list));
			}
		}

		String field = "baseGreen";

		int orderBy = mr.getOrderBy();
		if (orderBy == 2) {
			field = "zflastOkDate";
		} else if (orderBy == 3) {
			field = "sylttm";
		} else if (orderBy == 4) {
			field = "syl";
		} else if (orderBy == 5) {
			field = "sylType";
		} else if (orderBy == 6) {
			field = "holderNumP5";
		} else if (orderBy == 7) {
			field = "zfjjup";
		} else if (orderBy == 8) {
			field = "zfPriceLow";
		} else if (orderBy == 9) {
			field = "avgNum";
		}

		if (StringUtils.isNotBlank(mr.getMonitor())) {
			int m = Integer.valueOf(mr.getMonitor());
			if (m == 9999) {
				bqb.must(QueryBuilders.rangeQuery("monitor").gte(1));
			} else {
				bqb.must(QueryBuilders.matchPhraseQuery("monitor", m));
			}
		}
		if (mr.getPls() != -1) {
			bqb.must(QueryBuilders.matchPhraseQuery("pls", Integer.valueOf(mr.getPls())));
		}
		if (StringUtils.isNotBlank(mr.getBred())) {
			bqb.must(QueryBuilders.matchPhraseQuery("baseRed", Integer.valueOf(mr.getBred())));
		}
		if (StringUtils.isNotBlank(mr.getByellow())) {
			bqb.must(QueryBuilders.matchPhraseQuery("baseYellow", Integer.valueOf(mr.getByellow())));
		}
//		if (StringUtils.isNotBlank(mr.getBblue())) {
//			bqb.must(QueryBuilders.matchPhraseQuery("baseBlue", Integer.valueOf(mr.getBblue())));
//		}
//		if (StringUtils.isNotBlank(mr.getBgreen())) {
//			bqb.must(QueryBuilders.matchPhraseQuery("baseGreen", Integer.valueOf(mr.getBgreen())));
//		}
		if (StringUtils.isNotBlank(mr.getBsyl())) {
			bqb.must(QueryBuilders.matchPhraseQuery("sylType", Integer.valueOf(mr.getBsyl())));
		}
		if (StringUtils.isNotBlank(mr.getMkv())) {
			double mkv = Double.valueOf(mr.getMkv());
			if (mkv > 0) {
				bqb.must(QueryBuilders.rangeQuery("mkv").lte(mkv));
			}
		}
		if (mr.getZfself() == 1) {
			bqb.must(QueryBuilders.matchPhraseQuery("zfself", 1));
		}
		if (mr.getDzjyRct() == 1) {
			bqb.must(QueryBuilders.matchPhraseQuery("dzjyRct", 1));
		}
		if (mr.getSortChips() == 1) {
			bqb.must(QueryBuilders.matchPhraseQuery("sortChips", 1));
		}
		if (mr.getSusBigBoss() == 1) {
			bqb.must(QueryBuilders.matchPhraseQuery("susBigBoss", 1));
		}
		if (mr.getZfbuy() == 1) {
			bqb.must(QueryBuilders.matchPhraseQuery("zfbuy", 1));
		}

		if (mr.getSusWhiteHors() == 1) {
			bqb.must(QueryBuilders.matchPhraseQuery("susWhiteHors", 1));
		}
		if (mr.getSusZfBoss() == 1) {
			bqb.must(QueryBuilders.matchPhraseQuery("susZfBoss", 1));
		}
		if (mr.getSort6() == 1) {
			bqb.must(QueryBuilders.matchPhraseQuery("sortMode6", 1));
		}
		if (mr.getSort7() == 1) {
			bqb.must(QueryBuilders.matchPhraseQuery("sortMode7", 1));
		}
		if (mr.getZfjj() == 1) {
			bqb.must(QueryBuilders.matchPhraseQuery("zfjj", 1));
		}
		if (mr.getCompnayType() == 1) {
			bqb.must(QueryBuilders.matchPhraseQuery("compnayType", 1));
		}
		if (mr.getFinOK() == 1) {
			bqb.must(QueryBuilders.matchPhraseQuery("finOK", 1));
		}
		if (mr.getBousOK() == 1) {
			bqb.must(QueryBuilders.matchPhraseQuery("bousOK", 1));
		}

		if (StringUtils.isNotBlank(mr.getZfStatus())) {
			int t = Integer.valueOf(mr.getZfStatus());
			if (t == 6) {// 证监会核准
				bqb.must(QueryBuilders.matchPhraseQuery("zfStatusDesc", "证监会核准"));
			} else if (t == 8) {// 增发中或已完成
				bqb.must(QueryBuilders.rangeQuery("zfStatus").gte(1).lte(2));
			} else {
				bqb.must(QueryBuilders.matchPhraseQuery("zfStatus", t));
			}
		}
		if (mr.getZfObjType() > 0) {
			bqb.must(QueryBuilders.matchPhraseQuery("zfObjType", mr.getZfObjType()));
		}
		if (StringUtils.isNotBlank(mr.getZfjjup())) {
			int t = Integer.valueOf(mr.getZfjjup().trim());
			if (t > 0) {
				bqb.must(QueryBuilders.rangeQuery("zfjjup").gte(t));
			}
		}
		if (StringUtils.isNotBlank(mr.getZfjjupStable())) {
			int t = Integer.valueOf(mr.getZfjjupStable().trim());
			if (t > 0) {
				bqb.must(QueryBuilders.rangeQuery("zfjjupStable").gte(t));
			}
		}

		if (StringUtils.isNotBlank(mr.getZfPriceLow())) {
			int t = Integer.valueOf(mr.getZfPriceLow().trim());
			if (t > 0) {
				bqb.must(QueryBuilders.rangeQuery("zfPriceLow").gte(t));
			}
		}
		if (StringUtils.isNotBlank(mr.getHolderNumT3())) {
			double t = Double.valueOf(mr.getHolderNumT3().trim());
			if (t > 0) {
				bqb.must(QueryBuilders.rangeQuery("holderNumT3").gte(t));
			}
		}

		// 增发金额小于等于
		if (StringUtils.isNotBlank(mr.getZfYjAmt())) {
			Long zfYjAmt = Long.valueOf(mr.getZfYjAmt());
			if (zfYjAmt > 0) {
				bqb.must(QueryBuilders.rangeQuery("zfYjAmt").lte(zfYjAmt * 100000000));
			}
		}
		if (mr.getSmallModel() > 0) {
			bqb.must(QueryBuilders.rangeQuery("tagSmallAndBeatf").gte(1));
		}

//		<option value="3">资产收益率ttm</option>
//		<option value="4">资产收益率报告期</option>
//		<option value="5">资产收益评级</option>
		SortOrder order = SortOrder.DESC;
		if (mr.getAsc() == 2) {
			order = SortOrder.ASC;
		}

		FieldSortBuilder sort = SortBuilders.fieldSort(field).unmappedType("integer").order(order);

		NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder();
		Pageable pageable = PageRequest.of(querypage.getPageNum(), querypage.getPageSize());
		SearchQuery sq = queryBuilder.withQuery(bqb).withPageable(pageable).withSort(sort).build();

		Page<CodeBaseModel2> page = codeBaseModel2Dao.search(sq);
		if (page != null && !page.isEmpty()) {
			return page.getContent();
		}
		log.info("no records CodeBaseModels");
		return null;
	}

	public CodeBaseModelResp getLastOneByCodeResp(String code) {
		return getModelResp(getLastOneByCode2(code));
	}

	public CodeBaseModelResp getHistOneByCodeYearQuarter(String code, int year, int quarter) {
		BoolQueryBuilder bqb = QueryBuilders.boolQuery();
		bqb.must(QueryBuilders.matchPhraseQuery("code", code));
		bqb.must(QueryBuilders.matchPhraseQuery("currYear", year));
		bqb.must(QueryBuilders.matchPhraseQuery("currQuarter", quarter));
		FieldSortBuilder sort = SortBuilders.fieldSort("date").unmappedType("integer").order(SortOrder.DESC);
		NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder();
		SearchQuery sq = queryBuilder.withQuery(bqb).withSort(sort).build();
		Page<CodeBaseModelHist> page = codeBaseModelHistDao.search(sq);
		if (page != null && !page.isEmpty()) {
			CodeBaseModelHist dh = page.getContent().get(0);
			return getModelResp(dh);
		}
		return null;

	}

	public CodeBaseModelResp getHistOneById(String id) {
		log.info("getHistOneById:{}", id);
		BoolQueryBuilder bqb = QueryBuilders.boolQuery();
		bqb.must(QueryBuilders.matchPhraseQuery("id", id));

		NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder();
		SearchQuery sq = queryBuilder.withQuery(bqb).build();

		Page<CodeBaseModelHist> page = codeBaseModelHistDao.search(sq);
		if (page != null && !page.isEmpty()) {
			CodeBaseModelHist dh = page.getContent().get(0);
			return getModelResp(dh);
		}
		return null;
	}

	private CodeBaseModelResp getModelResp(CodeBaseModel2 dh) {
		CodeBaseModelResp resp = new CodeBaseModelResp();
		BeanUtils.copyProperties(dh, resp);
		resp.setCodeName(stockBasicService.getCodeName(dh.getCode()));
		StockBaseInfo s = stockBasicService.getCode(dh.getCode());
		resp.setCircZb(s.getCircZb());
		resp.setBankuai(s.getThsIndustry() + "<br/> " + s.getThsLightspot());
		StringBuffer sb1 = new StringBuffer("");
		if (dh.getBaseRed() == 1) {
			sb1.append("<font color='red'>红:</font>" + dh.getBaseRedDesc());
		}
		if (dh.getBaseYellow() == 1) {
			sb1.append("<font color='#FF00FF'>黄:</font>" + dh.getBaseYellowDesc());
		}
		if (dh.getBaseBlue() == 1) {
			sb1.append("<font color='blue'>蓝:</font>" + dh.getBaseBlueDesc());
		}
		if (dh.getBaseGreen() == 1) {
			sb1.append("<font color='green'>绿:</font>" + dh.getBaseGreenDesc());
		}
		resp.setBaseInfo(sb1.toString());
		resp.setMonitorDesc(MonitorType.getCodeName(dh.getMonitor()));
		// 收益率
		StringBuffer sb2 = new StringBuffer(SylType.getCodeName(dh.getSylType()));
		sb2.append(Constant.HTML_LINE).append("ttm/jd").append(Constant.HTML_LINE).append(dh.getSylttm()).append("/")
				.append(dh.getSyldjd());
		resp.setSylDesc(sb2.toString());

		// 短线
		StringBuffer sb3 = new StringBuffer("");
		if (dh.getSortChips() == 1) {
			sb3.append("吸筹-收集筹码短线").append(Constant.HTML_LINE);
		}
		if (dh.getSortMode6() == 1) {
			sb3.append("短线6").append(Constant.HTML_LINE);
		}
		if (dh.getSortMode7() == 1) {
			sb3.append("箱体突破").append(Constant.HTML_LINE);
		}
		resp.setSortInfo(sb3.toString());

		// 标签
		StringBuffer tag = new StringBuffer("");
		if (dh.getTagSmallAndBeatf() > 0) {
			tag.append("小而美").append(Constant.HTML_LINE);
		}
		if (dh.getSortChips() > 0) {
			tag.append("吸筹-收集筹码短线").append(Constant.HTML_LINE);
		}
		if (dh.getTagHighZyChance() > 0) {
			tag.append("高质押机会?").append(Constant.HTML_LINE);
		}
		if (dh.getSusBigBoss() == 1) {
			tag.append("疑似大牛").append(Constant.HTML_LINE);
		}
		if (dh.getSusWhiteHors() == 1) {
			tag.append("疑似白马").append(Constant.HTML_LINE);
		}
		resp.setTagInfo(tag.toString());

		// 博弈-基本面
		StringBuffer sb5 = new StringBuffer();
		sb5.append("流通:").append(dh.getMkv()).append("亿,");
		sb5.append("5%以下:").append(dh.getActMkv()).append("亿,");
		if (dh.getZfjjup() > 0) {
			sb5.append(dh.getZfjjup());
			if (dh.getZfjjupStable() > 0) {
				sb5.append("<font color='red'>/stable").append(dh.getZfjjupStable()).append("</font>");
			}
			sb5.append("年未大涨,");
		}
		if (dh.getBousOK() == 1) {
			sb5.append("近5年业绩不亏,");
		}
		if (dh.getFinOK() == 1) {
			sb5.append("近5年分红,");
		}
		sb5.append("前3大股东:").append(dh.getHolderNumT3()).append("%");
		sb5.append(",股东人数(少):").append(CurrencyUitl.covertToString(dh.getLastNum()));
		sb5.append(",人均持股(高):").append(CurrencyUitl.covertToString(dh.getAvgNum()));
		sb5.append(",变化:").append(dh.getHolderNum()).append("%");

		// 博弈-增发
		if (dh.getZfStatus() == 1 || dh.getZfStatus() == 2) {
			if (dh.getZfStatus() == 1) {
				sb5.append("<font color='red'>");
			} else {
				sb5.append("<font color='green'>");
			}
			sb5.append("增发进度" + (dh.getCompnayType() == 1 ? "(国资)" : "") + ":" + dh.getZfStatusDesc());
			sb5.append("</font>");

			if (dh.getZfStatus() == 1) {
				if (dh.getZfYjAmt() > 0) {
					sb5.append(",预增发金额:").append(CurrencyUitl.covertToString(dh.getZfYjAmt()));
				}
			} else {
				if (dh.getZfYjAmt() > 0) {
					sb5.append(",增发金额:").append(CurrencyUitl.covertToString(dh.getZfYjAmt()));
				}
				sb5.append(",增发价格:").append(dh.getZfPrice());
			}

		}
		// 最近一次增发
		if (dh.getZflastOkDate() > 0) {
			sb5.append(",实施日期:").append(dh.getZflastOkDate()).append(",");
			if (dh.getZfself() == 1) {
				sb5.append("<font color='green'>底部增发</font>,");
			}
			if (dh.getZfPriceLow() > 0) {
				sb5.append("<font color='red'>低于增发价:").append(dh.getZfPriceLow()).append("%</font>,");
			}
			if (dh.getGsz() == 1) {
				sb5.append("3年内有高送转,");
			}
			if (dh.getZfObjType() == 1) {
				sb5.append("6个月,");
			} else if (dh.getZfObjType() == 2) {
				sb5.append("混合(6月+大股东),");
			} else if (dh.getZfObjType() == 3) {
				sb5.append("大股东,");
			} else if (dh.getZfObjType() == 4) {
				sb5.append("其他,");
			}
		}
		// 解禁
		if (dh.getZfjj() == 1) {
			sb5.append("增发解禁(" + dh.getZfjjDate() + ")").append(Constant.HTML_LINE);
		}
		sb5.append(Constant.HTML_LINE);
		// 大宗
		if (dh.getDzjyRct() > 0) {
			sb5.append(",1年大宗超亿(均价:").append(dh.getDzjyAvgPrice()).append(")");
			if (dh.getTagDzPriceLow() > 0) {
				sb5.append(",低于均价:").append(dh.getTagDzPriceLow()).append("%");
			}
		}
		sb5.append(Constant.HTML_LINE);

		// 是否确定
		if (dh.getPls() == 0) {
			sb5.append("人工: 未确定").append(Constant.HTML_LINE);
		} else if (dh.getPls() == 1) {
			sb5.append("人工: 已确定").append(Constant.HTML_LINE);
		} else if (dh.getPls() == 2) {
			sb5.append("人工: 排除").append(Constant.HTML_LINE);
		}
		resp.setZfjjInfo(sb5.toString());
//		resp.setIncomeShow(dh.getCurrIncomeTbzz() + "%");
//		if (dh.getForestallIncomeTbzz() > 0) {
//			resp.setIncomeShow(resp.getIncomeShow() + "(" + dh.getForestallIncomeTbzz() + "%)");
//		}
//		resp.setProfitShow(dh.getCurrProfitTbzz() + "%");
//		if (dh.getForestallIncomeTbzz() > 0) {
//			resp.setProfitShow(resp.getProfitShow() + "(" + dh.getForestallProfitTbzz() + "%)");
//		}
		return resp;
	}

	public List<CodeBaseModelResp> getListForWeb(ModelReq mr, EsQueryPageReq querypage) {
		log.info("CodeBaseModel getListForWeb mr={}", mr);

		List<CodeBaseModel2> list = getList(mr, querypage);
		List<CodeBaseModelResp> res = new LinkedList<CodeBaseModelResp>();
		if (list != null) {
			for (CodeBaseModel2 dh : list) {
				res.add(getModelResp(dh));
			}
		}
		return res;
	}

	public List<String> listCodeByCodeConceptName(String conceptName) {
		List<String> codes = new LinkedList<String>();
		List<StockBaseInfo> l = stockBasicService.getAllOnStatusListWithSort();
		conceptName = conceptName.trim();
		for (StockBaseInfo s : l) {
			if (s.getThsIndustry().contains(conceptName)) {
				codes.add(s.getCode());
			}
		}
		return codes;
	}

	public void addPlsManual(ModelManulReq req) {
		String code = req.getCode();
		int pls = req.getPls();
		int timemonth = req.getTimemonth();
		if (pls != 1 && pls != 2) {
			throw new RuntimeException("i != 1 && i != 2 ? ");
		}

		int date = -1;
		if (timemonth == 9) {
			pls = 0;
			date = 0;
		} else {
			int days = 0;
			if (timemonth == 1) {
				days = 30;
			} else if (timemonth == 2) {
				days = 60;
			} else if (timemonth == 3) {
				days = 90;
			} else if (timemonth == 4) {
				days = 180;
			} else if (timemonth == 5) {
				days = 365;
			}
			if (days > 0) {
				Date now = new Date();
				date = DateUtil.formatYYYYMMDDReturnInt(DateUtil.addDate(now, days));
			}
		}
		if (date != 1) {
			String remark = req.getBuyRea() + " " + req.getSoldRea();
			CodeBaseModel2 c = getLastOneByCode2(code);
			BeanCopy.copy(req, c);
			c.setPls(pls);
			c.setPlst(date);
			c.setLstmt(DateUtil.getTodayIntYYYYMMDD());
			c.setBuyRea(remark);
			codeBaseModel2Dao.save(c);

			MonitorPool pool = monitorPoolService.getMonitorPool(code);
			pool.setRealtime(0);
			pool.setOffline(0);
			pool.setUpTodayChange(0);
			pool.setDzjy(0);
			pool.setHolderNum(0);
			pool.setYkb(0);
			pool.setZfdone(0);
			pool.setMonitor(MonitorType.NO.getCode());
			pool.setListenerGg(req.getListenerGg());
			if (pls == 1) {// 1在池子，2不在池子
				pool.setMonitor(MonitorType.MANUAL.getCode());
				pool.setRemark(remark);
				pool.setUpTodayChange(3);
				pool.setRealtime(1);
				int dt = DateUtil.formatYYYYMMDDReturnInt(DateUtil.addDate(new Date(), -1));
				pool.setDzjy(dt);
				pool.setHolderNum(dt);
				pool.setYkb(1);
				pool.setZfdone(1);
			} else if (pls == 2) {
				pool.setRemark("人工复核去掉");
			}
			monitorPoolDao.save(pool);
		}
	}
}
