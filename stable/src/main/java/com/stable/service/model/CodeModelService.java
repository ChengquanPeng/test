package com.stable.service.model;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.stable.constant.Constant;
import com.stable.constant.EsQueryPageUtil;
import com.stable.enums.MonitorType;
import com.stable.enums.ZfStatus;
import com.stable.es.dao.base.EsCodeBaseModel2Dao;
import com.stable.es.dao.base.MonitorPoolUserDao;
import com.stable.service.BonusService;
import com.stable.service.BuyBackService;
import com.stable.service.ChipsService;
import com.stable.service.ChipsZfService;
import com.stable.service.DaliyBasicHistroyService;
import com.stable.service.DaliyTradeHistroyService;
import com.stable.service.DataChangeService;
import com.stable.service.DzjyService;
import com.stable.service.FinanceService;
import com.stable.service.PlateService;
import com.stable.service.PriceLifeService;
import com.stable.service.ReducingHoldingSharesService;
import com.stable.service.StockBasicService;
import com.stable.service.TradeCalService;
import com.stable.service.ZhiYaService;
import com.stable.service.model.data.AvgService;
import com.stable.service.model.data.FinanceAnalyzer;
import com.stable.service.model.data.LineAvgPrice;
import com.stable.service.monitor.MonitorPoolService;
import com.stable.utils.BeanCopy;
import com.stable.utils.CurrencyUitl;
import com.stable.utils.DateUtil;
import com.stable.utils.ErrorLogFileUitl;
import com.stable.utils.WxPushUtil;
import com.stable.vo.HolderAnalyse;
import com.stable.vo.bus.CodeBaseModel2;
import com.stable.vo.bus.DaliyBasicInfo2;
import com.stable.vo.bus.DzjyYiTime;
import com.stable.vo.bus.FenHong;
import com.stable.vo.bus.FinanceBaseInfo;
import com.stable.vo.bus.FinanceBaseInfoHangye;
import com.stable.vo.bus.HolderPercent;
import com.stable.vo.bus.Jiejin;
import com.stable.vo.bus.MonitorPoolTemp;
import com.stable.vo.bus.StockBaseInfo;
import com.stable.vo.bus.TradeHistInfoDaliy;
import com.stable.vo.bus.ZengFa;
import com.stable.vo.bus.ZengFaDetail;
import com.stable.vo.bus.ZengFaExt;
import com.stable.vo.bus.ZhiYa;

import lombok.extern.log4j.Log4j2;

@Service
@Log4j2
public class CodeModelService {
	private static final long ZF_50YI = 50 * 100000000l;
	@Value("${small.stock.limit}")
	private double smallStocklimit;

	@Autowired
	private TradeCalService tradeCalService;
	@Autowired
	private StockBasicService stockBasicService;
	@Autowired
	private EsCodeBaseModel2Dao codeBaseModel2Dao;
	@Autowired
	private ModelWebService modelWebService;
	@Autowired
	private FinanceService financeService;
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
	private MonitorPoolUserDao monitorPoolDao;
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
	@Autowired
	private DataChangeService dataChangeService;
	@Autowired
	private Sort1ModeService sort1ModeService;
	@Autowired
	private BuyBackService buyBackService;
	@Autowired
	private ReducingHoldingSharesService reducingHoldingSharesService;
	@Autowired
	private Sort0Service sort0Service;
	@Autowired
	private Sort6Service sort6Service;

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

	private int tradeDate = 0;
	private int pre1Year = 0;// 一年以前
	private int pre3Year = 0;// 三年以前
	private int pre4Year = 0;// 四年以前
	private int bonusCheckYear = 0;

	private synchronized void runByJobv2(int t, boolean isweekend) {
		log.info("CodeModel processing request date={}", t);
		if (tradeCalService.isOpen(t)) {
			tradeDate = t;
		} else {
			tradeDate = tradeCalService.getPretradeDate(t);
		}
		log.info("Actually processing request date={}", tradeDate);
		pre1Year = DateUtil.getPreYear(tradeDate);
		pre3Year = DateUtil.getPreYear(tradeDate, 3);
		pre4Year = DateUtil.getPreYear(tradeDate, 4);

		// 基本面
		List<CodeBaseModel2> listLast = new LinkedList<CodeBaseModel2>();
		List<StockBaseInfo> codelist = stockBasicService.getAllOnStatusListWithSort();
		// 大牛
		Map<String, MonitorPoolTemp> poolMap = monitorPoolService.getMonitorPoolMap();
		List<MonitorPoolTemp> poolList = new LinkedList<MonitorPoolTemp>();
		// 到期提醒
		StringBuffer sbc = new StringBuffer();

		Map<String, CodeBaseModel2> histMap = modelWebService.getALLForMap();
		int currYear = DateUtil.getCurYYYY();
		bonusCheckYear = currYear - 5;// TODO 是否准确?
		for (StockBaseInfo s : codelist) {
			try {
				this.processingByCode(s, poolMap, poolList, listLast, histMap, isweekend, sbc);
			} catch (Exception e) {
				ErrorLogFileUitl.writeError(e, s.getCode(), "", "");
			}
		}
		if (listLast.size() > 0) {
			codeBaseModel2Dao.saveAll(listLast);
		}
		if (poolList.size() > 0) {
			monitorPoolDao.saveAll(poolList);
		}
		log.info("CodeModel v2 模型执行完成");
		if (sbc.length() > 0) {
			WxPushUtil.pushSystem1("人工pls==1已到期:" + sbc.toString());
		}
	}

	private void processingByCode(StockBaseInfo s, Map<String, MonitorPoolTemp> poolMap, List<MonitorPoolTemp> poolList,
			List<CodeBaseModel2> listLast, Map<String, CodeBaseModel2> histMap, boolean isweekend, StringBuffer sbc) {
		String code = s.getCode();
		// 监听池
		MonitorPoolTemp pool = poolMap.get(code);
		if (pool == null) {
			pool = new MonitorPoolTemp();
			pool.setCode(code);
		}
		poolList.add(pool);
		boolean onlineYear = stockBasicService.onlinePreYearChk(code, pre1Year);
		if (!onlineYear) {// 不买卖新股
			CodeBaseModel2 tone = new CodeBaseModel2();
			tone.setId(code);
			tone.setCode(code);
			tone.setDate(tradeDate);
			listLast.add(tone);
			return;
		}
		// 最新收盘情况
		DaliyBasicInfo2 lastTrade = daliyBasicHistroyService.queryLastest(code, 0, 0);
		if (lastTrade == null) {
			lastTrade = new DaliyBasicInfo2();
		}
		if (lastTrade.getCircMarketVal() <= 0) {
			lastTrade = daliyBasicHistroyService.queryLastest(code, 0, 1);
		}
		// 财报分析排雷
		CodeBaseModel2 oldOne = histMap.get(s.getCode());
		CodeBaseModel2 newOne = baseAnalyse(s, oldOne, lastTrade);
		listLast.add(newOne);
		// 市盈率ttm
		dataChangeService.getPeTtmData(code, newOne, oldOne);
		// 资金筹码-博弈
		game(newOne, lastTrade);
		// 国企|民企
		newOne.setCompnayType(s.getCompnayType());
		// 市值-死筹计算
		double mkv = lastTrade.getCircMarketVal();
		newOne.setMkv(mkv);
		newOne.setActMkv(0);
		if (mkv > 0 && s.getCircZb() > 0) {// 5%以下的流通股份
			newOne.setActMkv(CurrencyUitl.roundHalfUp(Double.valueOf(mkv * (100 - s.getCircZb()) / 100)));
		}
		// 人工审核是否时间到期-重置
		if (newOne.getPlst() < tradeDate) {
			if (newOne.getPls() == 1) {
				sbc.append(stockBasicService.getCodeName2(code)).append(",");
			}
			newOne.setPls(0);
			newOne.setPlst(0);
		}

		// 增发自动监听-重置
		if (newOne.getPls() == 0 && (pool.getMonitor() == MonitorType.NO.getCode()
				|| pool.getMonitor() > MonitorType.MANUAL.getCode())) {// 自动监听归0
			pool.setMonitor(MonitorType.NO.getCode());
			pool.setRealtime(0);
			pool.setOffline(0);
			pool.setUpTodayChange(0);
			pool.setShotPointCheck(0);
		}
		boolean online4Year = stockBasicService.onlinePreYearChk(code, pre4Year);
		// N年未大涨
		List<FinanceBaseInfo> yearRpts = noup(isweekend, online4Year, newOne, s.getList_date());
		// 小而美模型：未涨&&年报 && 大股东集中
		if (newOne.getZfjjup() >= 2 && mkv <= smallStocklimit && newOne.getHolderNumP5() >= 50) {
			if (yearRpts == null) {
				yearRpts = financeService.getFinacesReportByYearRpt(code, EsQueryPageUtil.queryPage5);
			}
			int c = yearRpts.size();
			if (yearRpts != null) {
				for (FinanceBaseInfo f : yearRpts) {
					if (f.getGsjlr() <= 0) {
						c--;
					}
				}
			}
			if (c >= (yearRpts.size() - 1)) {// 亏损最多一次
				newOne.setTagSmallAndBeatf(1);
				log.info("{} 小而美模型", code);
			}
		}
		// 收集筹码的短线-拉过一波，所以市值可以大一点
		newOne.setSortChips(0);
		if (online4Year && mkv > 0 && mkv <= smallStocklimit && chipsSortService.isCollectChips(code, tradeDate)) {
			newOne.setSortChips(1);
			log.info("{} 主力筹码收集", code);
		}

		// 以下是系统指标，没有4年直接退出
		if (!online4Year) {// 4年以上，退出
			return;
		}

		boolean isOk1 = false;
		boolean isOk2 = false;
		boolean isOk8 = false;
		boolean isOk9 = false;
		newOne.setShooting1(0);
		newOne.setShooting2(0);
		newOne.setShooting4(0);
		newOne.setShooting8(0);
		newOne.setShooting9(0);
		// 系统指标：自动监听
		if ((newOne.getBousOK() == 1 || newOne.getFinOK() == 1)) {// 1.基本面没有什么大问题
			if (newOne.getZfjjupStable() >= 2 || newOne.getZfjjup() >= 2) {// 2.底部没涨
				if (mkv <= smallStocklimit) {// 市值
					if (newOne.getHolderNumT3() > 45.0) {// 三大股东持股比例
						// 行情指标1：底部小票大宗：超活筹5%,董监高机构代减持?
						if (newOne.getDzjyp365d() >= 4.5) {// 大宗超过4.5%
							isOk1 = true;
							log.info("{} 小票，底部大宗超5千万", code);
						}
						// 行情指标8：底部小票增发：横盘3-4年以上==>1.基本面没问题，2.没涨，3:底部自己人增发，4排除大股东 (已完成的底部自己人增发)
						if (!isOk1 && newOne.getZfStatus() == ZfStatus.DONE.getCode() && newOne.getZfself() == 1
								&& (newOne.getZfjjup() >= 3 || newOne.getZfjjupStable() >= 3)
								&& newOne.getZfObjType() != 3) {
							isOk8 = true;
							log.info("{} 小票，底部横盘定增", code);
						}
					}

					if (!isOk1 && !isOk8 && newOne.getZfStatus() == ZfStatus.DONE.getCode() && newOne.getZfself() == 1
							&& newOne.getZfjjup() >= 2 && newOne.getZfjjupStable() >= 2 && newOne.getZfObjType() != 3) {
						isOk9 = true;
						log.info("{} 小票，底部横盘定增2年", code);
					}
				} else {
					// 行情指标2：底部大票增发：超过50亿(越大越好),股东集中,证监会核准-之前有明显底部拿筹痕迹-涨停？
					if (newOne.getZfYjAmt() >= ZF_50YI
							&& ZfStatus.ZF_ZJHHZ.getDesc().equals(newOne.getZfStatusDesc())) {
						isOk2 = true;
						log.info("{} 大票，底部增发超过50亿", code);
					}
				}
			}
			// 行情指标4：底部股东人数：大幅减少(3年减少40%)
			if (newOne.getZfjjup() >= 2 && newOne.getHolderNum() < -40.0) {// 股价3年没大涨，人数少了接近一半人
				log.info("{} 股东人数少了一半人", code);
				newOne.setShooting4(1);
			}
		}

		// 系统指标：自动化监听
		newOne.setShootingw(0);
		if (isOk1 || isOk2 || isOk8 || isOk9) {
			int motp = 0;
			if (isOk8) {
				motp = MonitorType.ZengFaAuto.getCode();
				newOne.setShooting8(1);
			}
			if (isOk1) {
				motp = MonitorType.DZJY.getCode();
				newOne.setShooting1(1);
			}

			if (isOk2) {
				motp = MonitorType.PreZengFa.getCode();
				newOne.setShooting2(1);
			}
			if (isOk9) {
				motp = MonitorType.ZengFaAuto2.getCode();
				newOne.setShooting9(1);
			}

			// 自动监听
			if (newOne.getPls() == 0) {// 未确定的自动监听，// 0不确定，1确定，2排除
				pool.setMonitor(motp);
				pool.setRealtime(1);
				pool.setOffline(1);
				pool.setUpTodayChange(7.5);
				pool.setShotPointCheck(1);
				pool.setRemark(Constant.AUTO_MONITOR + this.modelWebService.getSystemPoint(newOne, Constant.FEN_HAO));
			}
			sort0Service.attackAndW(code, tradeDate, newOne);
		}
		// ==============技术面-量价==============
		// 一年新高
		if (newOne.getPls() == 1) {
			if (pool.getYearHigh1() <= 0.0) {
				TradeHistInfoDaliy high = daliyTradeHistroyService.queryHighRecord(code, tradeDate);
				pool.setYearHigh1(high.getHigh());// 一年新高的价格（前复权）
			}
		} else {
			pool.setYearHigh1(0);
		}
		// 短线：妖股形态，短线拉的急，说明货多。一倍了，说明资金已经投入。新高:说明出货失败或者有更多的想法，要继续拉。
		sort1ModeService.sort1ModeChk(newOne, pool, tradeDate);
		// 基本面-疑似白马//TODO白马更多细节，比如市值，基金
		susWhiteHorses(code, newOne);
		// 短线模型
		sortModel(newOne);
	}

	// 周末计算-至少N年未大涨?
	private List<FinanceBaseInfo> noup(boolean isweekend, boolean online4Year, CodeBaseModel2 newOne,
			String listdatestr) {
		// 周末计算-至少N年未大涨?
		if (isweekend) {
			newOne.setZfjjup(0);
			newOne.setZfjjupStable(0);
			newOne.setFinOK(0);
			newOne.setBousOK(0);

			String code = newOne.getCode();
			if (online4Year) {
				int listdate = Integer.valueOf(listdatestr);
				newOne.setZfjjup(priceLifeService.noupYear(code, listdate));
				if (newOne.getZfjjup() >= 2) {
					newOne.setZfjjupStable(priceLifeService.noupYearstable(code, listdate));
				}
			}
			int c = 0;
			List<FinanceBaseInfo> yearRpts = financeService.getFinacesReportByYearRpt(code, EsQueryPageUtil.queryPage5);
			if (yearRpts != null) {
				c = yearRpts.size();
				for (FinanceBaseInfo f : yearRpts) {
					if (f.getGsjlr() < 0 || f.getKfjlr() < 0) {
						c--;
					}
				}
				if (c == yearRpts.size()) {
					newOne.setFinOK(1);
				}
			}
			if (bonusService.isBonusOk(code, bonusCheckYear)) {
				newOne.setBousOK(1);
			}
			return yearRpts;
		}
		return null;
	}

	private CodeBaseModel2 baseAnalyse(StockBaseInfo s, CodeBaseModel2 oldOne, DaliyBasicInfo2 lastTrade) {
		String code = s.getCode();
		log.info("Code Model  processing for code:{}", code);
		// 基本面池
		CodeBaseModel2 newOne = new CodeBaseModel2();
		if (oldOne != null) {// copy原有属性
			BeanCopy.copy(oldOne, newOne);
		}
		newOne.setId(code);
		newOne.setCode(code);
		newOne.setDate(tradeDate);
		// 财务
		List<FinanceBaseInfo> fbis = financeService.getFinacesReportByLteDate(code, tradeDate,
				EsQueryPageUtil.queryPage8);
		if (fbis == null) {
			ErrorLogFileUitl.writeError(new RuntimeException("无最新财务数据"), code, tradeDate + "", "Code Model错误");
			return newOne;
		}
		// 基本面-红蓝绿
		color(s, newOne, fbis, lastTrade);
		// 基本面-疑似大牛
		findBigBoss2(code, newOne, fbis);
		return newOne;
	}

	// 资金博弈
	private void game(CodeBaseModel2 newOne, DaliyBasicInfo2 lastTrade) {
		// 正在增发中
		chkLastOneYearZf(newOne);
		// 已完成的增发，更多细节
		lastDoneZfBoss(newOne, lastTrade);
		// 限售解禁(原始股东-增发-股权激励)
		zfjj(newOne);
		// 股东人数
		holderNum(newOne);
		// 大宗交易
		DzjyYiTime dz = dzjyService.dzjyF(newOne.getCode());
		newOne.setDzjyAvgPrice(dz.getAvgPrcie());
		newOne.setDzjy60d(dz.getTotalAmt60d());
		newOne.setDzjy365d(dz.getTotalAmt());
		newOne.setDzjyp365d(dz.getP365d());
		newOne.setDzjyp60d(dz.getP60d());
		// 低于大宗增发价
		newOne.setTagDzPriceLow(0);
		if (newOne.getDzjyRct() == 1 && dz.getAvgPrcie() > lastTrade.getClosed()) {
			newOne.setTagDzPriceLow(
					Double.valueOf(CurrencyUitl.cutProfit(lastTrade.getClosed(), dz.getAvgPrcie())).intValue());
		}
		// 减持占比
		newOne.setReducZb(reducingHoldingSharesService.getLastStat(newOne.getCode(), pre1Year).getZb());
	}

	private void holderNum(CodeBaseModel2 newOne) {
		HolderAnalyse ha = chipsService.holderNumAnalyse(newOne.getCode(), pre4Year);// 4年股东人数分析
		newOne.setHolderNum(ha.getAnaRes());
		newOne.setHolderDate(ha.getDate());
		newOne.setAvgNum(ha.getAvgNum());// 除开5%股东的人均流通持股
		newOne.setLastNum(ha.getLastNum());
		HolderPercent hp = chipsService.getLastHolderPercent(newOne.getCode());
		newOne.setHolderNumP5(hp.getPercent5());
		newOne.setHolderNumT3(hp.getTopThree());
	}

	private void sortModel(CodeBaseModel2 newOne) {
		String code = newOne.getCode();
		int tradeDate = newOne.getDate();
		newOne.setSortMode7(0);
		// 短线模型7(箱体震荡新高，是否有波浪走势)
		if (sort6Service.isWhiteHorseForSortV7(code, tradeDate)) {
			newOne.setSortMode7(1);
		}
	}

	// 增发
	private void chkLastOneYearZf(CodeBaseModel2 newOne) {
		newOne.setZfStatus(ZfStatus.NO.getCode());
		newOne.setZfStatusDesc("");
		newOne.setZfYjAmt(0);
		newOne.setZfAmt("");
		newOne.setZfPrice(0.0);
		ZengFa last = chipsZfService.getLastZengFa(newOne.getCode());
		// start 一年以前
		if (chipsZfService.isZfDateOk(last, pre1Year)) {
			newOne.setZfStatus(last.getStatus());
			newOne.setZfStatusDesc(last.getStatusDesc());
			newOne.setZfYjAmt(last.getYjamt());
			newOne.setZfPrice(last.getPrice());
			if (StringUtils.isNotBlank(last.getAmt()) && !"--".equals(last.getAmt().trim())) {
				newOne.setZfAmt(last.getAmt());
				newOne.setZfYjAmt(CurrencyUitl.covertToLong(last.getAmt()));
			}
		}
	}

	private void zfjj(CodeBaseModel2 newOne) {
		newOne.setZfjj(0);
		newOne.setZfjjDate(0);
		String code = newOne.getCode();
		int d = chipsService.getRecentlyZfJiejin(code);
		if (d > 0) {
			newOne.setZfjjDate(d);
			newOne.setZfjj(1);
		}
	}

	private void lastDoneZfBoss(CodeBaseModel2 newOne, DaliyBasicInfo2 lastTrade) {
		newOne.setZfself(0);
		newOne.setGsz(0);
		newOne.setZflastOkDate(0);
		// 低于增发价
		newOne.setZfPriceLow(0);
		newOne.setZfObjType(0);

		String code = newOne.getCode();
		ZengFa zf = chipsZfService.getLastZengFa(code, ZfStatus.DONE.getCode());// 已完成的增发
		if (chipsZfService.isZfDateOk(zf, pre1Year)) {
			newOne.setZflastOkDate(zf.getEndDate());
			ZengFaExt zfe = chipsZfService.getZengFaExtById(zf.getId());
			if (zfe != null) {
				newOne.setZfself(zfe.getSelfzf());
			}
			if (bonusService.isGsz(code, pre3Year)) {
				newOne.setGsz(1);
			}
			// 一年的之中的增发(低于增发价)
			if (chipsZfService.isZfDateOk(zf, pre1Year)) {
				if (zf.getPrice() > 0 && zf.getPrice() > lastTrade.getClosed()) {
					newOne.setZfPriceLow(
							Double.valueOf(CurrencyUitl.cutProfit(lastTrade.getClosed(), zf.getPrice())).intValue());
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

	// 增发解禁规则：根据证监会规则动态改变
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
		return 4;// 其他:关联
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
		// 是否中线(60日线),市值300亿以上
		if (newOne.getMkv() > 200 && priceLifeService.getLastIndex(code) >= 80
				&& LineAvgPrice.isWhiteHorseForMidV2(avgService, code, newOne.getDate())) {
			newOne.setSusWhiteHors(1);
		} else {
			newOne.setSusWhiteHors(0);
		}
	}

	private void color(StockBaseInfo s, CodeBaseModel2 newOne, List<FinanceBaseInfo> fbis, DaliyBasicInfo2 lastTrade) {
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
		if (fa.getCurrJidu().getYyzsr() < CurrencyUitl.YI_N.longValue()) {
			if (fa.getCurrJidu().getKfjlr() < 0) {
				newOne.setBaseRed(1);
				sb1.append(red++).append(".st风险或退市风险:扣非净利润为负且营收低于1亿元").append(Constant.HTML_LINE);
			} else {
				newOne.setBaseYellow(1);
				sb2.append(yellow++).append(".营收低于1亿元").append(Constant.HTML_LINE);
			}
		}
		// 应缴税费
		if (fbis.size() >= 5) {
			FinanceBaseInfo f0 = fbis.get(0);
			FinanceBaseInfo f1 = fbis.get(1);
			FinanceBaseInfo f2 = fbis.get(2);
			FinanceBaseInfo f3 = fbis.get(3);
			FinanceBaseInfo f4 = fbis.get(4);

			if (f0.getTaxPayable() >= f1.getTaxPayable() && f1.getTaxPayable() >= f2.getTaxPayable()
					&& f2.getTaxPayable() >= f3.getTaxPayable() && f3.getTaxPayable() >= f4.getTaxPayable()
					&& f0.getTaxPayable() > 0) {
				newOne.setBaseRed(1);
				sb1.append(red++).append(".暴雷风险:应交税费连续增长").append(Constant.HTML_LINE);
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
			if (c >= 4) {
				newOne.setBaseRed(1);
				sb1.append(red++).append(".暴雷破产风险:连续4季度负债超高-净资产低于应付账款").append(Constant.HTML_LINE);
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
			if (c >= 4) {
				newOne.setBaseRed(1);
				sb1.append(red++).append(".暴雷风险:连续4季度净资产为负资产").append(Constant.HTML_LINE);
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
			if (c >= 4) {
				newOne.setBaseRed(1);
				sb1.append(red++).append(".暴雷风险:连续4季度流动负债高于流动资产").append(Constant.HTML_LINE);
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
			sb2.append(yellow++).append(".财务疑似三高,详查各报告期短长期借款和货币资金(现金流净额?)").append(Constant.HTML_LINE);
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
			// 连续5季度
			if (c >= 4) {
				newOne.setBaseRed(1);
				sb1.append(red++).append(".暴雷风险:连续4季度资金紧张-应付利息高").append(Constant.HTML_LINE);
			} else {
				// 最近2年
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
				if (c >= 5) {
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
			if (c >= 4) {
				newOne.setBaseRed(1);
				sb1.append(red++).append(".暴雷风险:连续4季度资产负债率高").append(Constant.HTML_LINE);
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
			if (c >= 4) {
				newOne.setBaseRed(1);
				sb1.append(red++).append(".暴雷风险:连续4季度经营现金流连续为负却有扣非净利,靠融资在运转?").append(Constant.HTML_LINE);
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
			if (c >= 4) {
				newOne.setBaseRed(1);
				sb1.append(red++).append(".暴雷风险:连续4季度应收账款高").append(Constant.HTML_LINE);
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
		FinanceBaseInfoHangye hy = this.modelWebService.getFinanceBaseInfoHangye(code, fbi.getYear(), fbi.getQuarter());
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
		// 高质押风险与&机会(是否大股东?)
		newOne.setTagHighZyChance(0);
		ZhiYa zy = zhiYaService.getZhiYa(code);
		if (lastTrade.getClosed() < zy.getWarningLine()) {
			newOne.setTagHighZyChance(1);
		}
		if (zy.getHasRisk() == 1) {
			newOne.setBaseYellow(1);
			sb2.append(yellow++).append(".高质押风险:" + zy.getDetail()).append(Constant.HTML_LINE);
		}
		// 分红
		FenHong fh = chipsService.getFenHong(code);
		if (fh.getTimes() <= 0) {
			newOne.setBaseYellow(1);
			sb2.append(yellow++).append(".无分红记录").append(Constant.HTML_LINE);
		}
		// 限售股解禁(2年：前后1年)
		List<Jiejin> jj = chipsService.getBf2yearJiejin(code);
		if (jj != null) {
			newOne.setBaseYellow(1);
			sb2.append(yellow++).append(".前后1年解禁记录数:" + jj.size()).append(Constant.HTML_LINE);
		}

		// ======== 蓝色警告 ========
		StringBuffer sb3 = new StringBuffer();
		if (fa.getCurrJidu().getGsjlrtbzz() <= 0) {
			newOne.setBaseBlue(1);
			sb3.append("净利同比下降").append(Constant.HTML_LINE);
		}
		syl(newOne, fa, fbis);
		if (newOne.getSylType() == 4 || newOne.getSylType() == 1) {
		} else {
			newOne.setBaseBlue(1);
			sb3.append("资产收益率下降").append(Constant.HTML_LINE);
		}
		// 增持
		String zc = buyBackService.getLastRecordZc(code);
		if (StringUtils.isNotBlank(zc)) {
			newOne.setBaseBlue(1);
			sb3.append(zc).append(Constant.HTML_LINE);
		}
		// 回购
		String bb = buyBackService.getLastRecordBuyBack(code);
		if (StringUtils.isNotBlank(bb)) {
			newOne.setBaseBlue(1);
			sb3.append(bb).append(Constant.HTML_LINE);
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
	}

	private void findBigBoss2(String code, CodeBaseModel2 newOne, List<FinanceBaseInfo> fbis) {
		log.info("findBigBoss code:{}", code);
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
			newOne.setSusBigBoss(1);
		} else {
			newOne.setSusBigBoss(0);
		}
	}

	private void syl(CodeBaseModel2 newOne, FinanceAnalyzer fa, List<FinanceBaseInfo> fbis) {
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
}
