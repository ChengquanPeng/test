package com.stable.service.model.prd;

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.elasticsearch.search.sort.SortOrder;

import com.stable.constant.EsQueryPageUtil;
import com.stable.service.DaliyTradeHistroyService;
import com.stable.utils.CurrencyUitl;
import com.stable.utils.ErrorLogFileUitl;
import com.stable.utils.WxPushUtil;
import com.stable.vo.bus.Prd1;
import com.stable.vo.bus.TradeHistInfoDaliyNofq;

import lombok.extern.log4j.Log4j2;

@Log4j2
public class PreSelectSearch {// Sort2Feeling35Day
	private DaliyTradeHistroyService daliyTradeHistroyService;
	private PreSelectSave preSelectSave;

	public PreSelectSearch(DaliyTradeHistroyService a, PreSelectSave preSelectSave) {
		this.daliyTradeHistroyService = a;
		this.preSelectSave = preSelectSave;
	}

	// 计数器：开始
	public AtomicInteger cntDone = new AtomicInteger();

	private final double mkvcheckLine = 100.0;// 市值
	private double chkrateline = -7;// 3-5天跌幅
	private final double upchkLine = 65.0;// 一年涨幅超过

	List<Prd1> newList = Collections.synchronizedList(new LinkedList<Prd1>());

	public void done() {
		// 等待所有code执行完成
		int last = 0;
		int times = 0;
		int cur = 0;
		while (true) {
			cur = cntDone.get();
			if (cur == 0) {
				break;
			} else {
				log.info("PRD PreSelectCode waiting cnt ====>" + cur);
				try {
					TimeUnit.SECONDS.sleep(60);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				if (last == cur) {
					times++;
				} else {
					times = 0;
					last = cur;
				}
				if (times >= 5) {
					WxPushUtil.pushSystem1("错误:连续5分钟未成功执行任务PRD PreSelectCode PrdModeService.java");
					break;
				}
			}
		}
		log.info("PRD PreSelectCode ALL Done! 筛选到记录:{}", newList.size());
		this.preSelectSave.save(newList);
	}

	public void sort2ModeChk(String code, double mkv, int date) {
		try {
			if (mkv >= mkvcheckLine) {// 100亿
				Prd1 p1 = new Prd1();
				if (isPriceVolOk(code, date, p1) && isKline(code, date, p1)) {
					p1.setCode(code);
					p1.setPrd(1);
					newList.add(p1);
				}
			}
		} catch (Exception e) {
			ErrorLogFileUitl.writeError(e, code, date, mkv);
		} finally {
			cntDone.decrementAndGet();
		}
	}

	/**
	 * 一年涨了65%?
	 */
	private boolean isKline(String code, int date, Prd1 p1) {
		List<TradeHistInfoDaliyNofq> l2 = daliyTradeHistroyService.queryListByCodeWithLastNofq(code, 0, date,
				EsQueryPageUtil.queryPage250, SortOrder.DESC);

		TradeHistInfoDaliyNofq topDate = l2.stream().max(Comparator.comparingDouble(TradeHistInfoDaliyNofq::getHigh))
				.get();
		double maxPrice = topDate.getHigh();

		TradeHistInfoDaliyNofq lowDate = l2.stream().min(Comparator.comparingDouble(TradeHistInfoDaliyNofq::getLow))
				.get();
		double minPrice = lowDate.getLow();
		// d1.getDate() < d2.getDate():是上涨趋势。
		if (lowDate.getDate() < topDate.getDate() && CurrencyUitl.cutProfit(minPrice, maxPrice) >= upchkLine) {
			return true;
		}
		return false;
	}

	// 1.短期大幅下跌且缩量
	private boolean isPriceVolOk(String code, int date, Prd1 p1) {
		boolean isOk = false;
		List<TradeHistInfoDaliyNofq> l2 = daliyTradeHistroyService.queryListByCodeWithLastNofq(code, 0, date,
				EsQueryPageUtil.queryPage5, SortOrder.DESC);
		TradeHistInfoDaliyNofq today = l2.get(0);
		TradeHistInfoDaliyNofq preday = l2.get(1);

		// 1.放巨量在水上？？

		// 第一种情况：连续两天大幅放量下跌超过-8,量缩减
		if (today.getTodayChangeRate() <= 0.0 && preday.getTodayChangeRate() <= 0.0
				&& (chkrateline > (today.getTodayChangeRate() + preday.getTodayChangeRate()))) {
			if (preday.getVolume() > (today.getVolume() * 2)) {
				p1.setPrdsub(1);
				return true;
			}
		}
		// 第二种情况：连续3天下跌超过-8,量缩减
		if (today.getTodayChangeRate() <= 0.0 && preday.getTodayChangeRate() <= 0.0
				&& l2.get(2).getTodayChangeRate() <= 0.0 && (chkrateline > (today.getTodayChangeRate()
						+ preday.getTodayChangeRate() + l2.get(2).getTodayChangeRate()))) {
			if (today.getVolume() < preday.getVolume() && preday.getVolume() < l2.get(2).getVolume()) {
				if (l2.get(2).getVolume() > (today.getVolume() * 2)) {
					p1.setPrdsub(2);
					return true;
				}
			}
		}
		return isOk;
	}

	public static void main(String[] args) {
		double k = 100000;// 10万本金
		for (int i = 0; i < 22; i++) {// 一年250天
			k = k * 1.005;
		}
		System.err.println(k);
	}
}
