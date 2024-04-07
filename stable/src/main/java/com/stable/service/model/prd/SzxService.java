package com.stable.service.model.prd;

import java.util.List;

import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.stable.constant.EsQueryPageUtil;
import com.stable.service.DaliyTradeHistroyService;
import com.stable.service.model.data.LineAvgPrice;
import com.stable.utils.CurrencyUitl;
import com.stable.utils.StringUtil;
import com.stable.vo.bus.CodeBaseModel2;
import com.stable.vo.bus.MonitorPoolTemp;
import com.stable.vo.bus.TradeHistInfoDaliy;

@Service
public class SzxService {
	@Autowired
	private DaliyTradeHistroyService daliyTradeHistroyService;

//	@javax.annotation.PostConstruct
//	public void test() {
//		 十字星
//		String[] codes = { "002752", "000498", "601117" };
//		int[] dates = { 20211115, 20220105, 20210608 };
//		String[] codes = { "603289" };
//		int[] dates = { 20220707 };
//		for (int i = 0; i < codes.length; i++) {
//			String code = codes[i];
//			int date = dates[i];
//
//			CodeBaseModel2 newOne = new CodeBaseModel2();
//			newOne.setZfjjup(2);
//			newOne.setZfjjupStable(1);
//			newOne.setCode(code);
//			newOne.setPls(1);
//			MonitorPoolTemp pool = new MonitorPoolTemp();
//			qibao(date, newOne, pool, true, new StringBuffer(), new StringBuffer());
//			System.err.println(
//					code + "=====" + "Qixing:" + newOne.getQixing() + ",大旗形:" + newOne.getDibuQixing() + ",小旗形:"
//							+ newOne.getDibuQixing2() + ",十字星:" + newOne.getZyxing() + ",EX:" + newOne.getQixingStr());
//			System.err.println(pool);
//		}
//		System.exit(0);
//	}

	/** 十字星 */
	public void szx(int date, CodeBaseModel2 newOne, MonitorPoolTemp pool, boolean isSamll) {
//		szx1(date, newOne, pool, isSamll);
		szx2(date, newOne, pool);
	}

	/** 特别处理:最强逻辑十字星 */
	private void szx2(int date, CodeBaseModel2 newOne, MonitorPoolTemp pool) {
		// 人工,大宗,大票定增,做小做底+业绩不错
//		if (newOne.getPls() == 1 || newOne.getShooting1() == 1 || newOne.getShooting2() == 1
//				|| (newOne.getShooting7() == 1 && TagUtil.isFinPerfect(newOne))) {
		if (newOne.getPls() == 1 || newOne.getDibuQixing() > 0 || newOne.getDibuQixing2() > 0
				|| newOne.getNxipan() > 0) {

			List<TradeHistInfoDaliy> list = daliyTradeHistroyService.queryListByCodeWithLastQfq(newOne.getCode(), 0,
					date, EsQueryPageUtil.queryPage30, SortOrder.DESC);
			// 起爆点：中阳线,第二天十字星或者小影线
			// 1. 第一天,,第二天十字星或者小影线
			// 2.前5天涨幅低于2%
			TradeHistInfoDaliy d2tian = list.get(0);
			TradeHistInfoDaliy chk = list.get(1);
			TradeHistInfoDaliy preChk = list.get(2);
			boolean isOk = false;
			// 1.放量对比前日
			if (chk.getVolume() > preChk.getVolume() * 1.45) {
				// 2.中阳线,3-6个点,不是上影线,实体阳性-不能高开
				if ((3.0 <= chk.getTodayChangeRate() && chk.getTodayChangeRate() <= 6.5)
						&& !LineAvgPrice.isShangYingXian(chk)) {// 第一天中阳线,3-6个点
					// 3.收影线或者10字星
					if ((d2tian.getOpen() >= d2tian.getClosed()
							|| CurrencyUitl.cutProfit(d2tian.getOpen(), d2tian.getClosed()) <= 0.99)
							&& chk.getVolume() > d2tian.getVolume()) {// 第二天缩量,十字星或者小影线
						boolean preChkOk = true;
						// 阳线要收实体
						if (chk.getOpen() > chk.getYesterdayPrice()
								&& CurrencyUitl.cutProfit(chk.getYesterdayPrice(), chk.getOpen()) > 1.1) {
							preChkOk = false;
						}
						isOk = preChkOk;
					}
				}
			}
			if (isOk) {
				newOne.setZyxing(chk.getDate());
			}
		}

	}

	public void szx1(int date, CodeBaseModel2 newOne, MonitorPoolTemp pool, boolean isSamll) {
		// 人工或者底部优质票
		if (newOne.getPls() != 1 && newOne.getShooting7() != 1) {
			setSzxRes(newOne, pool);
			return;
		}
		List<TradeHistInfoDaliy> list = daliyTradeHistroyService.queryListByCodeWithLastQfq(newOne.getCode(), 0, date,
				EsQueryPageUtil.queryPage30, SortOrder.DESC);
		// 起爆点：中阳线,第二天十字星或者小影线
		// 1. 第一天,,第二天十字星或者小影线
		// 2.前5天涨幅低于2%
		TradeHistInfoDaliy d2tian = list.get(0);
		TradeHistInfoDaliy chk = list.get(1);
		TradeHistInfoDaliy preChk = list.get(2);
		boolean isOk = false;
		// 1.放量对比前日
		if (chk.getVolume() > preChk.getVolume() * 1.8) {
			// 2.中阳线,3-6个点,不是上影线,实体阳性-不能高开
			if ((3.0 <= chk.getTodayChangeRate() && chk.getTodayChangeRate() <= 6.5)
					&& !LineAvgPrice.isShangYingXian(chk)) {// 第一天中阳线,3-6个点
				// 3.收影线或者10字星
				if ((d2tian.getOpen() >= d2tian.getClosed()
						|| CurrencyUitl.cutProfit(d2tian.getOpen(), d2tian.getClosed()) <= 0.99)
						&& chk.getVolume() > d2tian.getVolume()) {// 第二天缩量,十字星或者小影线
					boolean preChkOk = true;
					// 2. 5天涨幅低于3%
					for (int i = 2; i <= 6; i++) {
						TradeHistInfoDaliy t = list.get(i);
						if (t.getTodayChangeRate() > 3) {
							preChkOk = false;
							break;
						}
					}
					// 阳线要收实体
					if (chk.getOpen() > chk.getYesterdayPrice()
							&& CurrencyUitl.cutProfit(chk.getYesterdayPrice(), chk.getOpen()) > 1.1) {
						preChkOk = false;
					}
					isOk = preChkOk;
				}
			}
		}

		if (isOk) {
			double moniHigh = (chk.getHigh() > d2tian.getHigh()) ? chk.getHigh() : d2tian.getHigh();
			newOne.setZyxing(chk.getDate());
			pool.setShotPointPriceSzx(moniHigh);
		} else {
			setSzxRes(newOne, pool);
		}
	}

	public void setSzxRes(CodeBaseModel2 newOne, MonitorPoolTemp pool) {
		if (newOne.getZyxing() > 0) {
			String jsHist = newOne.getZyxing() + "十字星" + ";" + newOne.getJsHist();
			newOne.setJsHist(StringUtil.subString(jsHist, 100));
		}
		newOne.setZyxing(0);
		pool.setShotPointPriceSzx(0);
	}
}
