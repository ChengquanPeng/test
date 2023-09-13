
package com.stable.service.model;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
import com.stable.service.DzjyService;
import com.stable.service.FinanceService;
import com.stable.service.ReducingHoldingSharesService;
import com.stable.service.StockBasicService;
import com.stable.service.ZhiYaService;
import com.stable.service.model.data.FinanceAnalyzer;
import com.stable.service.model.prd.msg.MsgPushServer;
import com.stable.service.monitor.MonitorPoolService;
import com.stable.utils.CurrencyUitl;
import com.stable.utils.DateUtil;
import com.stable.utils.ErrorLogFileUitl;
import com.stable.utils.TagUtil;
import com.stable.vo.HolderAnalyse;
import com.stable.vo.ReducingHoldingSharesStat;
import com.stable.vo.YgInfo;
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
import com.stable.vo.bus.ZengFa;
import com.stable.vo.bus.ZengFaDetail;
import com.stable.vo.bus.ZengFaExt;
import com.stable.vo.bus.ZhiYa;

import lombok.extern.log4j.Log4j2;

@Service
@Log4j2
public class CodeModelService {
	private static final long ZF_50YI = 50 * 100000000l;
	private static final long ZF_20YI = 20 * 100000000l;
	@Value("${small.stock.limit}")
	private double smallStocklimit;
	private double smallStocklimitAck = 50;// 50亿

	@Autowired
	private StockBasicService stockBasicService;
	@Autowired
	private EsCodeBaseModel2Dao codeBaseModel2Dao;
	@Autowired
	private WebModelService modelWebService;
	@Autowired
	private FinanceService financeService;
	@Autowired
	private ChipsService chipsService;
	@Autowired
	private ZhiYaService zhiYaService;
	@Autowired
	private MonitorPoolService monitorPoolService;
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
	private BuyBackService buyBackService;
	@Autowired
	private ReducingHoldingSharesService reducingHoldingSharesService;

//	@javax.annotation.PostConstruct
//	public void test() {
//		new Thread(new Runnable() {
//
//			@Override
//			public void run() {
//				runModel1(20220822, false);
//				System.err.println("runModel1 done");
//			}
//		}).start();
//	}

	public synchronized void runModel1(int date, boolean isweekend) {
		try {
			runByJobv2(date);
		} catch (Exception e) {
			e.printStackTrace();
			ErrorLogFileUitl.writeError(e, "CodeModel模型运行异常", "", "");
			MsgPushServer.pushToSystem("CodeModel模型运行异常..");
		}
	}

	public int tradeDate = 0;
	private int pre1Year = 0;// 一年以前
	private int pre2Year = 0;// 一年以前
	private int pre3Year = 0;// 三年以前
	private double yzdzamt = 0.45 * WebModelService.WAN;

	private synchronized void runByJobv2(int t) {
		tradeDate = t;
		pre1Year = DateUtil.getPreYear(tradeDate);
		pre2Year = DateUtil.getPreYear(tradeDate, 2);
		pre3Year = DateUtil.getPreYear(tradeDate, 3);

		// 基本面
		List<StockBaseInfo> codelist = stockBasicService.getAllOnStatusListWithSort();
		Map<String, MonitorPoolTemp> poolMap = monitorPoolService.getMonitorPoolMap();
		List<CodeBaseModel2> listLast = new LinkedList<CodeBaseModel2>();
		List<MonitorPoolTemp> poolList = new LinkedList<MonitorPoolTemp>();
		// 到期提醒
		StringBuffer sbc = new StringBuffer();
		// 做小坐地大宗
		StringBuffer xddz = new StringBuffer();
		// 做小坐地减持
		StringBuffer xdjc = new StringBuffer();
		// 确定扣非大牛
		StringBuffer yjm1 = new StringBuffer();
		// 疑似扣非大牛
		StringBuffer yjm2 = new StringBuffer();

		Map<String, CodeBaseModel2> histMap = modelWebService.getALLForMap();
		for (StockBaseInfo s : codelist) {
			try {
				this.processingByCode(s, poolMap, poolList, listLast, histMap, sbc, xddz, xdjc, yjm1, yjm2);
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
			MsgPushServer.pushToSystem("人工pls==1已到期:", sbc.toString());
		}
		if (xddz.length() > 0) {
			MsgPushServer.pushToSystem("最新标小-大宗:", xddz.toString());
		}
		if (xdjc.length() > 0) {
			MsgPushServer.pushToSystem("最新标小-减持:", xdjc.toString());
		}
		if (yjm1.length() > 0 || yjm2.length() > 0) {
			String mt = "";
			if (yjm1.length() > 0) {
				mt = "确定扣非业绩大牛:" + yjm1.toString();
			}
			if (yjm2.length() > 0) {
				mt += "<br/> 疑似扣非业绩大牛:" + yjm2.toString();
			}
			MsgPushServer.pushHtmlToSystem("扣非业绩大牛(最好是小票)", mt);
		}
	}

	public MonitorPoolTemp getPool(String code, Map<String, MonitorPoolTemp> poolMap, List<MonitorPoolTemp> poolList) {
		MonitorPoolTemp pool = poolMap.get(code);
		if (pool == null) {
			pool = new MonitorPoolTemp();
			pool.setCode(code);
			pool.setUserId(Constant.MY_ID);
			pool.setId(monitorPoolService.getId(pool.getUserId(), code));
		}
		pool.setUpdatedate(tradeDate);
		poolList.add(pool);
		return pool;
	}

	private void processingByCode(StockBaseInfo s, Map<String, MonitorPoolTemp> poolMap, List<MonitorPoolTemp> poolList,
			List<CodeBaseModel2> listLast, Map<String, CodeBaseModel2> histMap, StringBuffer sbc, StringBuffer xddz,
			StringBuffer xdjc, StringBuffer yjm1, StringBuffer yjm2) {
		String code = s.getCode();
		// 股票池
		CodeBaseModel2 newOne = histMap.get(s.getCode());
		if (newOne == null) {
			newOne = new CodeBaseModel2();
			newOne.setId(code);
			newOne.setCode(code);
		}
		newOne.setDate(tradeDate);
		listLast.add(newOne);
		boolean onlineYear = stockBasicService.onlinePreYearChk(code, pre1Year);
		if (!onlineYear) {// 不买卖新股
			return;
		}
		// 突然大宗有效期重置
		if (newOne.getDzjyBreaksDate() >= tradeDate) {
			newOne.setDzjyBreaksDate(0);
			newOne.setDzjyBreaks(0);
		}
		// 监听池
		MonitorPoolTemp pool = getPool(code, poolMap, poolList);
		// 最新收盘情况
		DaliyBasicInfo2 lastTrade = daliyBasicHistroyService.queryLastest(code, 0, 0);
		if (lastTrade == null) {
			lastTrade = new DaliyBasicInfo2();
		}
		newOne.setPb(lastTrade.getPb());// 市盈率ttm
		newOne.setPettm(lastTrade.getPeTtm());// 市盈率ttm
		// 小市值
		boolean isSmallStock = isSmallStock(newOne.getMkv(), newOne.getActMkv());
		// N年未大涨
		financeAndBonus(newOne, isSmallStock);
		// 财报分析排雷
		baseAnalyse(s, newOne, lastTrade, yjm1, yjm2);
		// 资金筹码-博弈
		game(newOne, lastTrade);
		// 国企|民企
		newOne.setCompnayType(s.getCompnayType());
		// 人工审核是否时间到期-重置
		if (newOne.getPlst() < tradeDate) {
			if (newOne.getPls() == 1) {
				sbc.append(stockBasicService.getCodeName2(code)).append(",");
			}
			newOne.setBuyRea(newOne.getBuyRea() + " 人工确定到期:" + newOne.getPlst());
			newOne.setPls(0);
			newOne.setPlst(0);
		}
		// 增发自动监听-重置
		if (newOne.getPls() == 0) {
			this.monitorPoolService.reset(pool);
		}
		boolean online4Year = stockBasicService.onlinePreYearChk(code, pre3Year);

		// 以下是系统指标，没有4年直接退出
		if (!online4Year) {// 4年以下，退出
			return;
		}

		boolean isOk1 = false;
		boolean isOk2 = false;
		boolean isOk6 = false;
		boolean isOk7 = false;
		boolean isOk8 = false;
		newOne.setShooting1(0);
		newOne.setShooting2(0);
		newOne.setShooting4(0);
		newOne.setShooting6(0);
		newOne.setShooting7(0);
		newOne.setShooting8(0);
		newOne.setShooting9(0);

		// 减持
		ReducingHoldingSharesStat rhss = reducingHoldingSharesService.getLast(newOne.getCode(), pre1Year);
		newOne.setReducZb(rhss.getZb());// 占比
		newOne.setReducYg(rhss.getYg());// 数量-股
		newOne.setReduceTims(rhss.getTims());// 次数

		/** 纯基本面 **/
		if (newOne.getFinOK() > 0 || newOne.getBousOK() > 0) {
			/** 行情指标4：底部股东人数：大幅减少(3年减少40%) **/
			if (newOne.getHolderNum() < -25.0) {// 股价3年没大涨，人数少了接近一半人
				log.info("{} 股东人数少了一半人", code);
				newOne.setShooting4(1);
			}

			/** 定增:底部大票1 **/
			if (TagUtil.isDibu21(newOne) && newOne.getMkv() >= smallStocklimit) {
				// 行情指标2：底部大票增发：超过50亿(越大越好),股东集中,证监会核准-之前有明显底部拿筹痕迹-涨停？
				if (ZfStatus.ZF_ZJHHZ.getDesc().equals(newOne.getZfStatusDesc())) {
					if (newOne.getZfYjAmt() >= ZF_50YI) {
						isOk2 = true;
						log.info("{} 大票，底部定增超过50亿", code);
					} else if (newOne.getZfYjAmt() >= ZF_20YI) {// 定增超过20亿,活动筹码小于40亿
						// 增发金额接近活动的筹码的1半
						long ackm = CurrencyUitl.covertToLong(newOne.getActMkv() + CurrencyUitl.YI);
						if ((ackm / Double.valueOf(newOne.getZfYjAmt())) <= 2.0) {
							isOk2 = true;
						}
					}
				}
			}
//			/** 定增:底部大票2: 底部优质票也可以这样。 **/
//			if (TagUtil.isDibuSmall(isSmallStock, newOne)
//					&& ZfStatus.ZF_ZJHHZ.getDesc().equals(newOne.getZfStatusDesc())) {
//				// 增发金额接近活动的筹码的1半
//				long ackm = CurrencyUitl.covertToLong(newOne.getActMkv() + CurrencyUitl.YI);
//				if ((ackm / Double.valueOf(newOne.getZfYjAmt())) <= 2.0) {
//					isOk2 = true;
//				}
//			}
		}
		/** 国资不低于净资产定增 ,pb负数：资不抵债,1年未涨 */
		if (s.getCompnayType() == 1 && newOne.getZfStatus() == 1 && newOne.getPb() > 0 && newOne.getPb() < 1
				&& newOne.getZfjjup() >= 1) {
			isOk2 = true;
		}
		boolean db1 = TagUtil.isDibuSmall(isSmallStock, newOne);// getZfjjup >= 2 && ZfjjupStable() >= 1;
		/** 底部横盘小票(不看基本面) **/
		if (db1) {
			newOne.setShooting9(1);
			/** 底部横盘小票:看基本面 **/
			if (newOne.getFinOK() > 0 || newOne.getBousOK() > 0) {

				/** 小票:增发&大宗&减持 **/
				if (newOne.getHolderNumP5() > 40.0) {// 三大股东持股比例
					if (newOne.getHolderNumP5() > 45.0
							// 基本面没有问题:连续盈利或者分红，连续3年盈利
							&& ((newOne.getFinOK() > 0 && newOne.getBousOK() > 0) || (newOne.getFinOK() >= 2))) {
						isOk7 = true;// 做小做底模型
						// 行情指标8：底部小票增发：横盘3-4年以上==>1.基本面没问题，2.没涨，3:底部自己人增发，4排除大股东 (已完成的底部自己人增发)
						if (newOne.getZfStatus() == ZfStatus.DONE.getCode() && newOne.getZfself() == 1
								&& newOne.getZfObjType() != 3) {
							isOk8 = true;
							log.info("{} 小票,底部定增", code);
						}
					}

					// 行情指标1：底部小票大宗：超活筹5%,董监高机构代减持?
					if (newOne.getDzjyp365d() >= 4.5) {// 大宗超过4.5%
						isOk1 = true;
						log.info("{} 小票,底部大宗超4.5%", code);
					}

					// 行情指标6：底部小票减持-业绩不能亏
					if (newOne.getReducZb() >= 2 && newOne.getFinOK() >= 1) {
						log.info("{} 小票,减持%", code);
						isOk6 = true;
					}
				}
			}
		}
		boolean db2 = TagUtil.isDibuSmall2(isSmallStock, newOne);// FinOK() > 0 && Zfjjup() >= 4 &&P5() > 45.0
		// 底部大宗
		if (db2 && (newOne.getDzjyp365d() >= Constant.DZ_RATE
				|| newOne.getDzjy365d() >= Constant.DZ_WARNING_LINE_YEAR)) {
			isOk1 = true;
		}
		// 标准小票-大宗
		if (newOne.getPls() != 2 && isOk7 && newOne.getDzjy365d() >= yzdzamt) {
			if (newOne.getShooting6661() == 0) {
				xddz.append(stockBasicService.getCodeName2(code)).append(",");
			}
			newOne.setShooting6661(1);
		} else {
			newOne.setShooting6661(0);
		}

		// 标准小票-减持
		if (newOne.getPls() != 2 && isOk7 && rhss.getLastPlanDate() > 0
				&& rhss.getLastPlanDate() >= newOne.getReduceLastPlanDate()) {// 大于等于是保持6662=1
			newOne.setShooting6662(1);
		} else {
			newOne.setShooting6662(0);
		}
		// 底部小票模式或者人工,减持计划通知
		if (newOne.getPls() != 2 && (isOk7 || newOne.getPls() == 1) && rhss.getLastPlanDate() > 0
				&& rhss.getLastPlanDate() > newOne.getReduceLastPlanDate()) {//// 没有等于为了通知
			xdjc.append(stockBasicService.getCodeName2(code)).append(",");
		}
		newOne.setReduceLastPlanDate(rhss.getLastPlanDate());// 最新减持计划时间（大股东）

		// 系统指标->自动化监听:底部优质小票，底部大票定增，底部小票大宗，底部小票定增，底部小票减持
		if (newOne.getPls() != 2 && (isOk1 || isOk2 || isOk6 || isOk7 || isOk8)) {
			int motp = 0;
			if (isOk7) {
				motp = MonitorType.SmallLow.getCode();
				newOne.setShooting7(1);
			}
			if (isOk8) {
				motp = MonitorType.ZengFaAuto.getCode();
				newOne.setShooting8(1);
			}
			if (isOk6) {
				motp = MonitorType.Reduce.getCode();
				newOne.setShooting6(1);
			}
			if (isOk1) {
				motp = MonitorType.DZJY.getCode();
				newOne.setShooting1(1);
			}
			if (isOk2) {
				motp = MonitorType.PreZengFa.getCode();
				newOne.setShooting2(1);
			}
			// 自动监听
			if (newOne.getPls() == 0) {// 未确定的自动监听，// 0不确定，1确定，2排除
				pool.setMonitor(motp);
				pool.setRealtime(1);
				pool.setOffline(1);
				pool.setUpTodayChange(5);
				if (isOk2 || isOk1) {// 确定性高
					pool.setUpTodayChange(3);
				}
//				pool.setShotPointCheck(1);
//				pool.setRemark(Constant.AUTO_MONITOR + TagUtil.getSystemPoint(newOne, Constant.FEN_HAO));
			}
		}

		// 人工的票，自动预警
		if (newOne.getPls() == 1 && ZfStatus.ZF_ZJHHZ.getDesc().equals(newOne.getZfStatusDesc())) {
			pool.setZfdone(1);
		}

		if (newOne.getPls() != 2 && newOne.getShooting6661() == 1 && pool.getUpTodayChange() > 0) {
			pool.setUpTodayChange(3);
		}
		// 同步监听
		if (pool.getMonitor() > MonitorType.NO.getCode()) {
			newOne.setMoni(pool.getMonitor());
		} else {
			newOne.setMoni(0);
			pool.reset0();// 重置监听
		}
		// 同步备注
		pool.setRemark(newOne.getBuyRea());
	}

	// 小市值股票(流通市值小于70亿，5%以下的流通小于50亿)
	public boolean isSmallStock(double mkv, double actMkv) {
		return TagUtil.mkvChk(mkv, actMkv, smallStocklimitAck);
	}

	// 周末计算-至少N年未大涨?
	private void financeAndBonus(CodeBaseModel2 newOne, boolean isSmallStock) {
		// 周末计算-至少N年未大涨?
		newOne.setFinOK(0);
		newOne.setFinanceInc(0);

		String code = newOne.getCode();
		List<FinanceBaseInfo> yearRpts = financeService.getFinacesReportByYearRpt(code, EsQueryPageUtil.queryPage5);
		int start = Integer.MAX_VALUE;
		int end = 0;
		if (yearRpts != null) {
			Set<Integer> set = new HashSet<Integer>();
			for (FinanceBaseInfo f : yearRpts) {
				if (f.getGsjlr() < 0) {// 归属净利润
				} else {
					set.add(f.getYear());// 获取不亏年份
				}

				// 结束年份
				if (f.getYear() > end) {
					end = f.getYear();
				}
				// 开始年份
				if (f.getYear() < start) {
					start = f.getYear();
				}
			}

			if (set.size() >= 5) {
				newOne.setFinOK(5);
			} else {
				int c = 0;
				for (int s = end; s >= start; s--) {// 从最近年份开始往前循环
					if (set.contains(s)) {
						c++;
					} else {// 没有找到就结束
						break;
					}
				}
				newOne.setFinOK(c);
			}

			int inc = 0;
			FinanceBaseInfo tmp = null;
			for (FinanceBaseInfo f : yearRpts) {
				if (tmp == null) {
					tmp = f;
				} else {
					if (tmp.getGsjlr() > f.getGsjlr()) {
						inc++;
					} else {
						break;
					}
					tmp = f;
				}
			}
			if (inc >= 2) {
				newOne.setFinanceInc(inc);
			}

		}
		// 小而美模型：未涨&&年报 && 大股东集中
		newOne.setTagSmallAndBeatf(0);
		if (TagUtil.isDibuSmall(isSmallStock, newOne) && newOne.getHolderNumP5() >= 45 && newOne.getFinOK() >= 3) {
			newOne.setTagSmallAndBeatf(1);
		}
		// 分红
		if (start == Integer.MAX_VALUE) {
			start = 0;
		}
		bonusService.bonusYear(code, start, end, newOne);
	}

	private void baseAnalyse(StockBaseInfo s, CodeBaseModel2 newOne, DaliyBasicInfo2 lastTrade, StringBuffer yjm1,
			StringBuffer yjm2) {
		String code = s.getCode();
		log.info("Code Model  processing for code:{}", code);
		// 基本面池
		// 财务
		List<FinanceBaseInfo> fbis = financeService.getFinacesReportByLteDate(code, tradeDate,
				EsQueryPageUtil.queryPage8);
		if ((fbis == null || fbis.size() < EsQueryPageUtil.queryPage8.getPageSize())
				&& stockBasicService.onlinePreYearChk(code, pre2Year)) {
			ErrorLogFileUitl.writeError(
					new RuntimeException("无最新财务数据,或者少于条数：" + EsQueryPageUtil.queryPage8.getPageSize()), code,
					tradeDate + "", "Code Model错误");
			return;
		}
		// 基本面-红蓝绿
		color(s, newOne, fbis, lastTrade, yjm1, yjm2);
		// 基本面-疑似大牛
//		findBigBoss2(code, newOne, fbis);
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
	}

	private void holderNum(CodeBaseModel2 newOne) {
		HolderAnalyse ha = chipsService.holderNumAnalyse(newOne.getCode(), pre3Year);// 4年股东人数分析
		newOne.setHolderNum(ha.getAnaRes());
		newOne.setHolderDate(ha.getDate());
		newOne.setAvgNum(ha.getAvgNum());// 除开5%股东的人均流通持股
		newOne.setLastNum(ha.getLastNum());
		HolderPercent hp = chipsService.getLastHolderPercent(newOne.getCode());
		newOne.setHolderNumP5(hp.getPercent5());
		newOne.setHolderNumT3(hp.getTopThree());
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
		if (chipsZfService.isZfDateOk(last, pre3Year)) {
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
		if (chipsZfService.isZfDateOk(zf, pre3Year)) {
			newOne.setZflastOkDate(zf.getEndDate());
			ZengFaExt zfe = chipsZfService.getZengFaExtById(zf.getId());
			if (zfe != null) {
				newOne.setZfself(zfe.getSelfzf());
			}
			if (bonusService.isGsz(code, pre3Year)) {
				newOne.setGsz(1);
			}
			// 一年的之中的增发(低于增发价)
			if (chipsZfService.isZfDateOk(zf, pre3Year)) {
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

	private void color(StockBaseInfo s, CodeBaseModel2 newOne, List<FinanceBaseInfo> fbis, DaliyBasicInfo2 lastTrade,
			StringBuffer yjm1, StringBuffer yjm2) {
		newOne.setBaseYellow(0);
		newOne.setBaseRed(0);
		newOne.setBaseBlue(0);
		newOne.setBaseYellowDesc("");
		newOne.setBaseRedDesc("");
		newOne.setBaseBlueDesc("");

		String code = newOne.getCode();
		FinanceAnalyzer fa = new FinanceAnalyzer();
		for (FinanceBaseInfo fbi : fbis) {
			fa.putJidu1(fbi);
		}
		FinanceBaseInfo fbi = fa.getCurrJidu();
		newOne.setCurrYear(fbi.getYear());
		newOne.setCurrQuarter(fbi.getQuarter());
		// 业绩暴涨,至少超1亿吧
		newOne.setFinDbl(0);
		if (fa.getCurrYear().getGsjlr() >= CurrencyUitl.YI_N_DOUBLE) {
			if (fa.getPrevYear().getGsjlr() > 0) {
				double a = CurrencyUitl
						.roundHalfUp(fa.getCurrYear().getGsjlr() / Double.valueOf(fa.getPrevYear().getGsjlr()));
				if (a >= 2) {
					newOne.setFinDbl(1);
				}
			}
			if (fa.getCurrJidu().getGsjlr() > 0) {
				double a = CurrencyUitl
						.roundHalfUp(fa.getCurrJidu().getGsjlr() / Double.valueOf(fa.getPrevYear().getGsjlr()));
				if (a >= 2) {
					newOne.setFinDbl(1);
				}
			}
		}
		// ======== 红色警告 ========
		// ======== 黄色警告 ========
		int red = 1;
		int yellow = 1;
		StringBuffer sb1 = new StringBuffer();
		StringBuffer sb2 = new StringBuffer();
		// 退市风险:营收低于1亿
		if (fbi.getYyzsr() < CurrencyUitl.YI_N.longValue()) {
			boolean chk = false;
			if (fbi.getQuarter() <= 3) {
				// 前面3季度要算平均值，且要超过1.3亿
				if ((fbi.getYyzsr() / fbi.getQuarter()) * 4 <= (CurrencyUitl.YI_N.longValue() * 1.3)) {
					chk = true;
				}
			} else {
				chk = true;
			}
			if (chk) {
				if (fbi.getKfjlr() < 0) {
					newOne.setBaseRed(1);
					sb1.append(red++).append(".退市风险(ST):扣非净利润为负且营收低于1亿元").append(Constant.HTML_LINE);
				} else {
					newOne.setBaseRed(1);
					sb1.append(red++).append(".退市风险(ST):营收低于1亿元(净利暂不为负)").append(Constant.HTML_LINE);
				}
			}
		}
		// 退市风险:净资产为负
		if (fbi.getNetAsset() < CurrencyUitl.YI_N.longValue() && fbi.getNetAsset() != 0.0) {
			if (fbi.getNetAsset() < 0) {
				newOne.setBaseRed(1);
				sb1.append(red++).append(".退市风险(ST):净资产为负值").append(Constant.HTML_LINE);
			} else {
				newOne.setBaseRed(1);
				sb1.append(red++).append(".退市风险(ST):净资产低于1亿元(容易暴雷)").append(Constant.HTML_LINE);
			}
		}
		// 年报
		if (fa.profitDown2Year() == 1) {
			newOne.setBaseRed(1);
			sb1.append(red++).append(".至少2年亏损").append(Constant.HTML_LINE);
		} else if (fa.getCurrYear().getGsjlr() < 0) {
			newOne.setBaseRed(1);
			sb1.append(red++).append(".年报亏损").append(Constant.HTML_LINE);
		}

		if (fbis.size() >= 5) {
			FinanceBaseInfo f0 = fbis.get(0);
			FinanceBaseInfo f1 = fbis.get(1);
			FinanceBaseInfo f2 = fbis.get(2);
			FinanceBaseInfo f3 = fbis.get(3);
			FinanceBaseInfo f4 = fbis.get(4);

			// 应缴税费
			if (f0.getTaxPayable() >= f1.getTaxPayable() && f1.getTaxPayable() >= f2.getTaxPayable()
					&& f2.getTaxPayable() >= f3.getTaxPayable() && f3.getTaxPayable() >= f4.getTaxPayable()
					&& f0.getTaxPayable() > 0) {
				newOne.setBaseRed(1);
				sb1.append(red++).append(".暴雷风险:应交税费连续增长").append(Constant.HTML_LINE);
			}

			// 应收帐连续增长
			if (f0.getAccountrec() >= f1.getAccountrec() && f1.getAccountrec() >= f2.getAccountrec()
					&& f2.getAccountrec() >= f3.getAccountrec() && f3.getAccountrec() >= f4.getAccountrec()
					&& f0.getAccountrec() > 0) {
				if (f0.getJyxjlce() <= 0 && f0.getMgjyxjl() <= 0) {
					newOne.setBaseRed(1);
					sb1.append(red++).append(".暴雷风险:应收账款连续增长且现金流为负").append(Constant.HTML_LINE);
				} else {
					newOne.setBaseYellow(1);
					sb2.append(yellow++).append(".应收账款连续增长").append(Constant.HTML_LINE);
				}
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
				sb1.append(red++).append(".暴雷风险:连续4季度负债超高-净资产低于应付账款").append(Constant.HTML_LINE);
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
			sb2.append(yellow++).append(".财务三高造假?短长期借款?货币资金?现金流?").append(Constant.HTML_LINE);
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
				sb1.append(red++).append(".资产负债率超高:").append(CurrencyUitl.roundHalfUp(fbi.getZcfzl())).append("%")
						.append(Constant.HTML_LINE);
			} else {
				newOne.setBaseYellow(1);
				sb2.append(yellow++).append(".资产负债率高:").append(CurrencyUitl.roundHalfUp(fbi.getZcfzl())).append("%")
						.append(Constant.HTML_LINE);
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
				sb2.append(yellow++).append(".经营现金流为负,净利存疑,详查财报").append(Constant.HTML_LINE);
			} else {
				newOne.setBaseYellow(1);
				sb2.append(yellow++).append(".经营现金流为负").append(Constant.HTML_LINE);
			}
		}
		if (fbi.getKfjlr() > 0 && (fbi.getJyxjlce() < 0 || fbi.getMgjyxjl() < 0)) {
			int c = 0;
			int fort = 0;// 最近2年
			if (fbis.size() > 2) {
				for (FinanceBaseInfo ft : fbis) {
					if (ft.getKfjlr() > 0 && (ft.getJyxjlce() <= 0 && ft.getMgjyxjl() <= 0)) {
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

				// 最新年报数据现金流
				if (fa.getCurrYear().getJyxjlce() <= 0 && fa.getCurrYear().getMgjyxjl() <= 0) {
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
						sb1.append(red++).append(".暴雷风险:最近").append(c).append("季度经常现金流为负却有扣非净利")
								.append(Constant.HTML_LINE).append("靠融资在运转?").append(Constant.HTML_LINE);
					}
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
		// 增持
		String zc = buyBackService.getLastRecordZc(code, pre1Year);
		if (StringUtils.isNotBlank(zc)) {
			newOne.setBaseBlue(1);
			sb3.append(zc).append(Constant.HTML_LINE);
		}
		// 回购
		String bb = buyBackService.getLastRecordBuyBack(code, pre1Year);
		if (StringUtils.isNotBlank(bb)) {
			newOne.setBaseBlue(1);
			sb3.append(bb).append(Constant.HTML_LINE);
		}
		// 快预报
		StringBuffer ykbm = new StringBuffer("");
		YgInfo yi = financeService.getyjkb(fbi.getCode(), fbi.getYear(), fbi.getQuarter(), ykbm);
		if (ykbm.length() > 0) {
			newOne.setBaseBlue(1);
			sb3.append(ykbm).append(Constant.HTML_LINE);
		}

		int yjn = 0;
		// 股票范围：5%持股30以上，底部未涨
		if (newOne.getHolderNumP5() > 30.0 && (TagUtil.isDibu11(newOne) || newOne.getZfjjup() >= 4)) {
			yjn = financeService.finBigBoss(yi, fa, fbis, newOne);
		}
		if (yjn == 0) {
			newOne.setFinBoss(0);
			newOne.setFinSusBoss(0);
			newOne.setBossVal(0);
			newOne.setBossInc(0);
		}

		// 确定
		if (yjn == 1) {
			if (newOne.getFinBoss() == 0) {
				yjm1.append(stockBasicService.getCodeName2(code)).append(",");
			}
			newOne.setFinBoss(1);
			newOne.setFinSusBoss(0);
		} else if (yjn == 2) {// 疑似
			if (newOne.getFinSusBoss() == 0) {
				yjm2.append(stockBasicService.getCodeName2(code)).append(",");
			}
			newOne.setFinBoss(0);
			newOne.setFinSusBoss(1);
		}

		if (zy.getLastNoticeDate() > 0 && zy.getLastNoticeDate() > this.pre1Year) {
			newOne.setLastZyDate(zy.getLastNoticeDate());
			newOne.setBaseBlue(1);
			sb3.append("<font color='blue'>最新解质押日期:" + zy.getLastNoticeDate() + "</font>").append(Constant.HTML_LINE);
		} else {
			newOne.setLastZyDate(0);
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

	public void cleanOfflineCode() {
		Map<String, MonitorPoolTemp> poolMap = monitorPoolService.getMonitorPoolMap();
		Map<String, CodeBaseModel2> histMap = modelWebService.getALLForMap();
		List<StockBaseInfo> list = this.stockBasicService.getAllOnStatusListWithSort();
		Map<String, StockBaseInfo> map = new HashMap<String, StockBaseInfo>();
		if (list != null) {
			for (StockBaseInfo c : list) {
				map.put(c.getCode(), c);
			}
		}

		List<StockBaseInfo> noolist = this.stockBasicService.nonHhuShen();
		Map<String, StockBaseInfo> map2 = new HashMap<String, StockBaseInfo>();
		if (noolist != null) {
			for (StockBaseInfo c : noolist) {
				map2.put(c.getCode(), c);
			}
		}

		List<CodeBaseModel2> listLast = new LinkedList<CodeBaseModel2>();
		List<MonitorPoolTemp> poolList = new LinkedList<MonitorPoolTemp>();
		for (CodeBaseModel2 cbm : histMap.values()) {
			if (!map.containsKey(cbm.getCode()) || map2.containsKey(cbm.getCode())) {
				listLast.add(cbm);
				log.info("删除 model: " + cbm.getCode());
			}
		}
		for (MonitorPoolTemp cbm : poolMap.values()) {
			if (!map.containsKey(cbm.getCode()) || map2.containsKey(cbm.getCode())) {
				poolList.add(cbm);
				log.info("删除监听: " + cbm.getCode());
			}
		}
		if (listLast.size() > 0) {
			codeBaseModel2Dao.deleteAll(listLast);
		}
		log.info("codeBaseModel2Dao:" + listLast.size());
		if (poolList.size() > 0) {
			monitorPoolDao.deleteAll(poolList);
		}
		log.info("monitorPoolDao:" + poolList.size());

		stockBasicService.deleteTuiShi();
	}
//
//	private void findBigBoss2(String code, CodeBaseModel2 newOne, List<FinanceBaseInfo> fbis) {
//		log.info("findBigBoss code:{}", code);
//		// 业绩连续
//		int continueJidu1 = 0;
//		List<Double> high = new LinkedList<Double>();
//		for (FinanceBaseInfo fbi : fbis) {
//			if (fbi.getYyzsrtbzz() >= 1.0 && fbi.getGsjlrtbzz() >= 1.0) {// 连续增长季度的数量
//				continueJidu1++;
//				high.add(fbi.getGsjlrtbzz());
//			} else {
//				break;
//			}
//		}
//		// 连续6季度增长，且最近一季度同比增长超20%
//		if (continueJidu1 >= 4 && high.get(0) >= 20.0) {
//			newOne.setSusBigBoss(1);
//		} else {
//			newOne.setSusBigBoss(0);
//		}
//	}
}
