package com.stable.service.model;

import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.stable.constant.EsQueryPageUtil;
import com.stable.constant.RedisConstant;
import com.stable.service.ChipsService;
import com.stable.service.DaliyTradeHistroyService;
import com.stable.utils.CurrencyUitl;
import com.stable.utils.RedisUtil;
import com.stable.vo.bus.HolderNum;
import com.stable.vo.bus.TradeHistInfoDaliyNofq;

import lombok.extern.log4j.Log4j2;

/**
 * 
 * 大涨收集筹码后，对应股东人数下跌的短线
 *
 */
@Service
@Log4j2
public class ChipsSortService {
	@Autowired
	private DaliyTradeHistroyService daliyTradeHistroyService;
	@Autowired
	private RedisUtil redisUtil;
	@Autowired
	private ChipsService chipsService;
//	@Autowired
//	private ThsHolderSpider thsHolderSpider;
//
//	@PostConstruct
//	public void init() {
//		new Thread(new Runnable() {
//			public void run() {
//				thsHolderSpider.doCodeFe("002612");
//				System.err.println();
//				log.info("init done:" + isCollectChips("002612", 1, 20200910));
//			}
//		}).start();
//	}

	public boolean isCollectChips(String code, int tradeDate) {
		List<TradeHistInfoDaliyNofq> origlist = daliyTradeHistroyService.queryListByCodeWithLastNofq(code, 0, tradeDate,
				EsQueryPageUtil.queryPage180, SortOrder.DESC);
		if (origlist != null && origlist.size() > 0) {
			int okDate = redisUtil.get(RedisConstant.RDS_CHIPS_SORT_OK_DT_ + code, 0);
			boolean stPrice = false;
			int startChkDate = origlist.get(origlist.size() - 1).getDate();
			if (okDate >= startChkDate) {
				// OK
				stPrice = true;
			} else {
				// check daliy
				okDate = chkAndGetDate(code, origlist);
				if (okDate > 0) {
					stPrice = true;
				}
			}

			// check 股东人数
			if (stPrice) {
				HolderNum nb = chipsService.getLastHolderNumBfDate(code, okDate);// 拉升之前的
				if (nb != null) {
					List<HolderNum> holders = chipsService.getHolderNumList45(code, nb.getDate());
					if (holders != null) {
						HolderNum dmax = holders.stream().max(Comparator.comparingInt(HolderNum::getNum)).get();
						HolderNum dmin = holders.stream().min(Comparator.comparingInt(HolderNum::getNum)).get();
						if (dmax.getDate() < dmin.getDate()
								&& CurrencyUitl.cutProfit(dmin.getNum(), dmax.getNum()) > 30) {
							// 1.拉升后人数减少
							// 2.人数减少30%以上
							return true;
						}
					}
				}
				log.info("{} 有冲高行为,但无吸筹行为", code);
			} else {
				log.info("{} 无冲高行为", code);
			}
		} else {
			log.info("{} ignore,origlist is null", code);
		}
		return false;
	}

	// 15个工作日超过70以上
	private int chkAndGetDate(String code, List<TradeHistInfoDaliyNofq> origlist) {

		int lastChkDate = redisUtil.get(RedisConstant.RDS_CHIPS_SORT_END_DT_ + code, 0);
		TradeHistInfoDaliyNofq getObj = null;

		List<TradeHistInfoDaliyNofq> chklist = new LinkedList<TradeHistInfoDaliyNofq>();
		for (int i = origlist.size() - 1; i >= 0; i--) {
			if (chklist.size() < 15) {
				chklist.add(origlist.get(i));
			} else {
				chklist.remove(0);// 去掉第一个
				chklist.add(origlist.get(i));// 尾部添加另一个
			}

			TradeHistInfoDaliyNofq n = chklist.get(0);

			if (n.getDate() > lastChkDate && chklist.size() >= 15) {
				double maxPrice = chklist.stream().max(Comparator.comparingDouble(TradeHistInfoDaliyNofq::getHigh))
						.get().getHigh();
				double minPrice = chklist.stream().min(Comparator.comparingDouble(TradeHistInfoDaliyNofq::getLow)).get()
						.getLow();
				double persent3 = CurrencyUitl.cutProfit(minPrice, maxPrice);
				if (persent3 >= 70.0) {
					getObj = n;
					break;
				}
			}
		}

		redisUtil.set(RedisConstant.RDS_CHIPS_SORT_END_DT_ + code, origlist.get(origlist.size() - 1).getDate());
		if (getObj != null) {
			int okDate = getObj.getDate();
			redisUtil.set(RedisConstant.RDS_CHIPS_SORT_OK_DT_ + code, okDate);
			return okDate;
		}
		return 0;
	}

	public static void main(String[] args) {
		List<Integer> chklist = new LinkedList<Integer>();
		for (int i = 0; i < 10; i++) {
			chklist.add(i);
		}
		print(chklist);
		for (int i = 100; i < 110; i++) {
			chklist.remove(0);
			chklist.add(i);
			print(chklist);
		}
	}

	private static void print(List<Integer> chklist) {
		for (Integer i : chklist) {
			System.err.print(i + ",");
		}
		System.err.println();
		System.err.println("=============");
	}
}
