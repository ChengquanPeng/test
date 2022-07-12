package com.stable.service.model.prd;

import java.util.Date;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.stable.constant.EsQueryPageUtil;
import com.stable.service.DaliyTradeHistroyService;
import com.stable.service.StockBasicService;
import com.stable.service.TradeCalService;
import com.stable.service.model.CodeModelService;
import com.stable.service.model.RunModelService;
import com.stable.service.model.WebModelService;
import com.stable.service.model.data.LineAvgPrice;
import com.stable.utils.CurrencyUitl;
import com.stable.utils.DateUtil;
import com.stable.utils.TagUtil;
import com.stable.vo.QiBaoInfo;
import com.stable.vo.bus.CodeBaseModel2;
import com.stable.vo.bus.MonitorPoolTemp;
import com.stable.vo.bus.TradeHistInfoDaliy;
import com.stable.vo.bus.TradeHistInfoDaliyNofq;

import lombok.extern.log4j.Log4j2;

@Service
@Log4j2
public class Prd1Service {
	@Autowired
	private RunModelService runModelService;
	@Autowired
	private DaliyTradeHistroyService daliyTradeHistroyService;
	@Autowired
	private WebModelService modelWebService;
	@Autowired
	private StockBasicService stockBasicService;
	@Autowired
	private CodeModelService codeModelService;
	@Autowired
	private TradeCalService tradeCalService;

	// @javax.annotation.PostConstruct
	public void test() {
		new Thread(new Runnable() {
			public void run() {
				t1();
			}
		}).start();

	}

	private void t1() {
		int enddate = 20220711;
		Date d = DateUtil.parseDate(20220601);
		while (true) {
			int date = DateUtil.formatYYYYMMDDReturnInt(d);
			if (tradeCalService.isOpen(date)) {
				int pre1Year = DateUtil.getPreYear(date);
				List<TradeHistInfoDaliyNofq> listNofq = daliyTradeHistroyService.queryListByCodeNofq("", date, date,
						EsQueryPageUtil.queryPage9999, SortOrder.DESC);
				Set<String> set = todaySzx(listNofq);
				Map<String, CodeBaseModel2> histMap = modelWebService.getALLForMap();
				MonitorPoolTemp pool = new MonitorPoolTemp();
				List<CodeBaseModel2> resl = new LinkedList<CodeBaseModel2>();
				for (String code : set) {
					CodeBaseModel2 cbm = histMap.get(code);
					if (cbm != null) {
						boolean isSamll = codeModelService.isSmallStock(cbm.getMkv(), cbm.getActMkv());
						if (stockBasicService.onlinePreYearChk(code, pre1Year)) {
							prd(date, cbm, pool, isSamll);
							if (cbm.getPrd1() == 1) {
								resl.add(cbm);
							}
						}
					}
				}
				runModelService.genPrdHtml(date, resl);
			}
			System.err.println("===================================");
			System.err.println("===================================");
			System.err.println("=========== " + date + " ================");
			System.err.println("===================================");
			System.err.println("===================================");
			if (date == enddate) {
				break;
			}
			d = DateUtil.addDate(d, 1);
		}
	}

	public Set<String> todaySzx(List<TradeHistInfoDaliyNofq> listNofq) {
		Set<String> prd1 = new HashSet<String>();
		for (TradeHistInfoDaliyNofq t : listNofq) {
			if (todayPrickOK(t.getClosed(), t.getOpen(), t.getAmt())) {
				prd1.add(t.getCode());
			}
		}
		return prd1;
	}

	// 是否十字星
	public boolean todayPrickOK(double close, double open, double amt) {
		if (close == open) {
			if (amt < CurrencyUitl.YI_N_DOUBLE) {
				return true;
			}
		} else {
			double p = CurrencyUitl.cutProfit(open, close);
			if (-0.55 <= p && p <= 0.55) {// 收盘在0.5之间
				return true;
			}
		}
		return false;
	}

	/** 起爆 */
	public void prd(int date, CodeBaseModel2 newOne, MonitorPoolTemp pool, boolean isSamll) {
		if (runModelService.stTuiShi(newOne)) {
			setQxRes(newOne, pool);
			return;
		}

		if (isSamll && TagUtil.isDibu11(newOne)) {// 底部1年小票
			findBigUp(date, newOne, pool);
		} else {
			setQxRes(newOne, pool);
		}

	}

	/** 起爆点,底部旗形1：大旗形 **/
	private void findBigUp(int date, CodeBaseModel2 newOne, MonitorPoolTemp pool) {
		List<TradeHistInfoDaliy> list = daliyTradeHistroyService.queryListByCodeWithLastQfq(newOne.getCode(), 0, date,
				EsQueryPageUtil.queryPage30, SortOrder.DESC);
		QiBaoInfo res = null;
		/** 找到大涨的那段 */
		TradeHistInfoDaliy today = list.get(0);

		for (int i = 1; i < list.size(); i++) {
			TradeHistInfoDaliy curr = list.get(i);
			if (curr.getTodayChangeRate() >= 9.0 && i >= 3) {
				TradeHistInfoDaliy chk = null;
				TradeHistInfoDaliy di2tian = list.get(i - 1);
				if (di2tian.getTodayChangeRate() > 0) {// 第二天涨了
					chk = di2tian;
				} else {// 没涨
					chk = curr;
				}
				res = isQixingType1(chk, list, today);
				if (res != null) {// 1.是否旗形
					break;
				}
			}
		}
		if (res != null) {
			newOne.setPrd1(1);
//			pool.setPrd1(prd1); todo
		}
	}

	/**
	 * chkdate之后的验证旗形1-大阳
	 */
	private QiBaoInfo isQixingType1(TradeHistInfoDaliy chk, List<TradeHistInfoDaliy> list, TradeHistInfoDaliy today) {
		List<TradeHistInfoDaliy> after = new LinkedList<TradeHistInfoDaliy>();
		for (TradeHistInfoDaliy nf : list) {// 倒序循环
			if (nf.getDate() > chk.getDate()) {
				after.add(0, nf);// 改为正序循环
			}
		}
		if (after.size() >= 3) {// 至少大涨后有三天数据

			TradeHistInfoDaliy d2tian = after.get(0);
			// 如果第二天涨了，是十字星或者高开低走
			if (d2tian.getTodayChangeRate() > 0) {
				if (d2tian.getClosed() > d2tian.getOpen()) {// 正常收红了
					// 不是十字星或不是上影线
					if (!LineAvgPrice.isShangYingXian(d2tian)
							&& CurrencyUitl.cutProfit(d2tian.getOpen(), d2tian.getClosed()) > 0.5) {

//						System.err.println(chk.getDate());
//						System.err.println(tmp.get(1).getDate());
//						System.err.println(tmp.get(1).getYesterdayPrice());
//						System.err.println(tmp.get(1).getYesterdayPrice() * 1.01);
//						System.err.println(tmp.get(1).getHigh());
//						System.err.println((tmp.get(1).getYesterdayPrice() * 1.01) >= tmp.get(1).getHigh());

						if (d2tian.getTodayChangeRate() >= 9.5 && after.get(1).getTodayChangeRate() <= -6.5
								&& ((after.get(1).getYesterdayPrice() * 1.01) >= after.get(1).getHigh())) {
							// 2天大涨&涨停，第三天大阴线
//							log.info("d2tian=" + d2tian.getDate());
							if (after.size() >= 4) {
								double t0 = after.get(1).getTodayChangeRate() + after.get(2).getTodayChangeRate()
										+ after.get(3).getTodayChangeRate();
								if (t0 <= -13.5) {
									// 3天下跌13.5%以上
								} else {
									log.info("=====>非十字星或上影线1");
									return null;
								}
							} else {
								log.info("=====>非十字星或上影线2");
								return null;
							}
						} else {
							log.info("=====>非十字星或上影线3");
							return null;
						}
					}

				} // else 高开低走
			}
			// 收阴线
			double high = (chk.getHigh() > d2tian.getHigh()) ? chk.getHigh() : d2tian.getHigh();
			if (isOk(high, after, today)) {
				return new QiBaoInfo();
			}
		}
		log.info("=====>默认false");
		return null;
	}

	// 旗形：后续的都不能超过之前高点(只允许一次)
	private boolean isOk(double high, List<TradeHistInfoDaliy> tmp, TradeHistInfoDaliy today) {
		int tims = 0;
		// 是否一路下跌
		TradeHistInfoDaliy start = tmp.get(0);
		if (start.getHigh() > high) {
			tims++;
		}
		int xd = 0;
		double vol = Integer.MAX_VALUE;
		for (int i = 1; i < tmp.size(); i++) {
			TradeHistInfoDaliy t = tmp.get(i);
			if (t.getHigh() > high) {
				tims++;
				if (tims > 1) {
					log.info("=====>超过最高");
					return false;
				}
			}
			if (t.getTodayChangeRate() < 0) {
				xd++;
			} else if (t.getLow() < start.getLow()) {
				xd++;
			}
			start = t;
			if (t.getTodayChangeRate() >= -9 && t.getVolume() < vol) {// 跌停不算
				vol = t.getVolume();
			}
		}

		// 1.不能超过最高一次
		// 2.xd下跌趋势
		// 3.量最小(或者不能超过10%)
		if (tims <= 1 && (xd * 2 > tmp.size()) && (today.getVolume() <= vol || (vol * 1.1 > today.getVolume()))) {
			return true;
		}
		log.info("=====>超过最高2");
		return false;
	}

	private void setQxRes(CodeBaseModel2 newOne, MonitorPoolTemp pool) {
		newOne.setPrd1(0);
		pool.setPrd1(0);
	}
}
