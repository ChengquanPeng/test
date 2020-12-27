package com.stable.utils;

import java.util.LinkedList;
import java.util.List;

import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.stable.constant.EsQueryPageUtil;
import com.stable.service.DaliyTradeHistroyService;
import com.stable.vo.bus.StockAvg;
import com.stable.vo.bus.StockAvgBase;
import com.stable.vo.bus.StockAvgNofq;
import com.stable.vo.bus.TradeHistInfoDaliy;
import com.stable.vo.bus.TradeHistInfoDaliyNofq;

import lombok.extern.log4j.Log4j2;

@Service
@Log4j2
public class SMAUtil {
	private final int PERIODS_AVERAGE_60 = 60;
	private final int PERIODS_AVERAGE_30 = 30;
	private final int PERIODS_AVERAGE_20 = 20;
	private final int PERIODS_AVERAGE_10 = 10;
	private final int PERIODS_AVERAGE_5 = 5;

	@Autowired
	private DaliyTradeHistroyService daliyTradeHistroyService;

	public List<StockAvgBase> getSMA5_30(String code, int startDate, int endDate, boolean isQfq) {
		double[] closePrice;
		List<TradeHistInfoDaliy> fqlist = null;
		List<TradeHistInfoDaliyNofq> nfqlist = null;
		if (isQfq) {
			// 倒序的结果列表
			List<TradeHistInfoDaliy> temp = daliyTradeHistroyService.queryListByCodeWithLastQfq(code, 0, startDate,
					EsQueryPageUtil.queryPage30, SortOrder.DESC);// startDate:开始日期的前30个交易日

			if (temp.size() != 30) {
				throw new RuntimeException(
						"计算错误,前面30个交易日获取数据错误,code=" + code + ",startDate=" + startDate + ",endDate=" + endDate);
			}
			fqlist = daliyTradeHistroyService.queryListByCodeWithLastQfq(code, temp.get(temp.size() - 1).getDate(),
					endDate, EsQueryPageUtil.queryPage9999, SortOrder.DESC);
			closePrice = new double[fqlist.size()];

			// 顺序的收盘价数组
			int j = 0;
			for (int i = fqlist.size() - 1; i >= 0; i--) {
				closePrice[i] = fqlist.get(j).getClosed();
				j++;
			}
		} else {
			// 倒序的结果列表
			List<TradeHistInfoDaliyNofq> temp = daliyTradeHistroyService.queryListByCodeWithLastNofq(code, 0, startDate,
					EsQueryPageUtil.queryPage30, SortOrder.DESC);// startDate:开始日期的前30个交易日

			if (temp.size() != 30) {
				throw new RuntimeException(
						"计算错误,前面30个交易日获取数据错误,code=" + code + ",startDate=" + startDate + ",endDate=" + endDate);
			}
			nfqlist = daliyTradeHistroyService.queryListByCodeWithLastNofq(code, temp.get(temp.size() - 1).getDate(),
					endDate, EsQueryPageUtil.queryPage9999, SortOrder.DESC);
			closePrice = new double[nfqlist.size()];

			// 顺序的收盘价数组
			int j = 0;
			for (int i = nfqlist.size() - 1; i >= 0; i--) {
				closePrice[i] = nfqlist.get(j).getClosed();
				j++;
			}
		}

		// 顺序的结果
		double[] avg30 = TaLabUtil.sma(closePrice, PERIODS_AVERAGE_30);
		if (avg30 != null) {
			int dividendDate = DateUtil.getTodayIntYYYYMMDD();
			double[] avg20 = TaLabUtil.sma(closePrice, PERIODS_AVERAGE_20);
			double[] avg10 = TaLabUtil.sma(closePrice, PERIODS_AVERAGE_10);
			double[] avg5 = TaLabUtil.sma(closePrice, PERIODS_AVERAGE_5);
			List<StockAvgBase> rs = new LinkedList<StockAvgBase>();
			// 倒序列表
			if (isQfq) {
				for (TradeHistInfoDaliy td : fqlist) {
					if (td.getDate() >= startDate) {
						StockAvg sa = new StockAvg();
						sa.setCode(code);
						sa.setDate(td.getDate());
						sa.setLastDividendDate(dividendDate);
						sa.setId();
						rs.add(sa);
					}
				}
			} else {
				for (TradeHistInfoDaliyNofq td : nfqlist) {
					if (td.getDate() >= startDate) {
						StockAvgNofq sa = new StockAvgNofq();
						sa.setCode(code);
						sa.setDate(td.getDate());
						sa.setLastDividendDate(dividendDate);
						sa.setId();
						rs.add(sa);
					}
				}
			}
			int index = rs.size() - 1;
			for (int i = 0; i < avg30.length; i++) {// 结果顺序开始
				if (avg30[i] != 0.0) {// 最后有29个为0数字
					if (index >= 0) {
						rs.get(index).setAvgPriceIndex30(CurrencyUitl.roundHalfUp(avg30[i]));
						index--;
					}
				}
			}

			index = 0;
			int endIndex = rs.size() - 1;
			for (int i = avg20.length - 1; i >= 0; i--) {// 结果倒序开始
				if (avg20[i] != 0.0) {
					if (index >= 0 && index <= endIndex) {// 倒数最后一个不为0的数开始,list最大值结束。
						rs.get(index).setAvgPriceIndex20(CurrencyUitl.roundHalfUp(avg20[i]));
						index++;
					}
				}
			}
			index = 0;
			for (int i = avg10.length - 1; i >= 0; i--) {// 结果倒序开始
				if (avg10[i] != 0.0) {// 最后有N个为0数字
					if (index >= 0 && index <= endIndex) {// 倒数最后一个不为0的数开始,list最大值结束。
						rs.get(index).setAvgPriceIndex10(CurrencyUitl.roundHalfUp(avg10[i]));
						index++;
					}
				}
			}
			index = 0;
			for (int i = avg5.length - 1; i >= 0; i--) {// 结果倒序开始
				if (avg5[i] != 0.0) {// 最后有N个为0数字
					if (index >= 0 && index <= endIndex) {// 倒数最后一个不为0的数开始,list最大值结束。
						rs.get(index).setAvgPriceIndex5(CurrencyUitl.roundHalfUp(avg5[i]));
						index++;
					}
				}
			}
			return rs;
		} else {
			log.info("多日均线计算错误,code={},startDate={},startDate={}", code, startDate, endDate);
			return null;
		}
	}

	public List<StockAvgBase> getSMA5_60(String code, int startDate, int endDate, boolean isQfq) {
		double[] closePrice;
		List<TradeHistInfoDaliy> fqlist = null;
		List<TradeHistInfoDaliyNofq> nfqlist = null;
		if (isQfq) {
			// 倒序的结果列表
			List<TradeHistInfoDaliy> temp = daliyTradeHistroyService.queryListByCodeWithLastQfq(code, 0, startDate,
					EsQueryPageUtil.queryPage60, SortOrder.DESC);// startDate:开始日期的前60个交易日

			if (temp.size() != 60) {
				throw new RuntimeException(
						"计算错误,前面60个交易日获取数据错误,code=" + code + ",startDate=" + startDate + ",endDate=" + endDate);
			}
			fqlist = daliyTradeHistroyService.queryListByCodeWithLastQfq(code, temp.get(temp.size() - 1).getDate(),
					endDate, EsQueryPageUtil.queryPage9999, SortOrder.DESC);
			closePrice = new double[fqlist.size()];

			// 顺序的收盘价数组
			int j = 0;
			for (int i = fqlist.size() - 1; i >= 0; i--) {
				closePrice[i] = fqlist.get(j).getClosed();
				j++;
			}
		} else {
			// 倒序的结果列表
			List<TradeHistInfoDaliyNofq> temp = daliyTradeHistroyService.queryListByCodeWithLastNofq(code, 0, startDate,
					EsQueryPageUtil.queryPage60, SortOrder.DESC);// startDate:开始日期的前30个交易日

			if (temp.size() != 60) {
				throw new RuntimeException(
						"计算错误,前面60个交易日获取数据错误,code=" + code + ",startDate=" + startDate + ",endDate=" + endDate);
			}
			nfqlist = daliyTradeHistroyService.queryListByCodeWithLastNofq(code, temp.get(temp.size() - 1).getDate(),
					endDate, EsQueryPageUtil.queryPage9999, SortOrder.DESC);
			closePrice = new double[nfqlist.size()];

			// 顺序的收盘价数组
			int j = 0;
			for (int i = nfqlist.size() - 1; i >= 0; i--) {
				closePrice[i] = nfqlist.get(j).getClosed();
				j++;
			}
		}

		// 顺序的结果
		double[] avg60 = TaLabUtil.sma(closePrice, PERIODS_AVERAGE_60);
		if (avg60 != null) {
			int dividendDate = DateUtil.getTodayIntYYYYMMDD();
			double[] avg30 = TaLabUtil.sma(closePrice, PERIODS_AVERAGE_30);
			double[] avg20 = TaLabUtil.sma(closePrice, PERIODS_AVERAGE_20);
			double[] avg10 = TaLabUtil.sma(closePrice, PERIODS_AVERAGE_10);
			double[] avg5 = TaLabUtil.sma(closePrice, PERIODS_AVERAGE_5);
			List<StockAvgBase> rs = new LinkedList<StockAvgBase>();
			// 倒序列表
			if (isQfq) {
				for (TradeHistInfoDaliy td : fqlist) {
					if (td.getDate() >= startDate) {
						StockAvg sa = new StockAvg();
						sa.setCode(code);
						sa.setDate(td.getDate());
						sa.setLastDividendDate(dividendDate);
						sa.setId();
						rs.add(sa);
					}
				}
			} else {
				for (TradeHistInfoDaliyNofq td : nfqlist) {
					if (td.getDate() >= startDate) {
						StockAvgNofq sa = new StockAvgNofq();
						sa.setCode(code);
						sa.setDate(td.getDate());
						sa.setLastDividendDate(dividendDate);
						sa.setId();
						rs.add(sa);
					}
				}
			}
			int index = rs.size() - 1;
			for (int i = 0; i < avg60.length; i++) {// 结果顺序开始
				if (avg60[i] != 0.0) {// 最后有60个为0数字
					if (index >= 0) {
						rs.get(index).setAvgPriceIndex60(CurrencyUitl.roundHalfUp(avg60[i]));
						index--;
					}
				}
			}

			int endIndex = rs.size() - 1;

			index = 0;
			for (int i = avg30.length - 1; i >= 0; i--) {// 结果倒序开始
				if (avg30[i] != 0.0) {
					if (index >= 0 && index <= endIndex) {// 倒数最后一个不为0的数开始,list最大值结束。
						rs.get(index).setAvgPriceIndex30(CurrencyUitl.roundHalfUp(avg30[i]));
						index++;
					}
				}
			}

			index = 0;
			for (int i = avg20.length - 1; i >= 0; i--) {// 结果倒序开始
				if (avg20[i] != 0.0) {
					if (index >= 0 && index <= endIndex) {// 倒数最后一个不为0的数开始,list最大值结束。
						rs.get(index).setAvgPriceIndex20(CurrencyUitl.roundHalfUp(avg20[i]));
						index++;
					}
				}
			}
			index = 0;
			for (int i = avg10.length - 1; i >= 0; i--) {// 结果倒序开始
				if (avg10[i] != 0.0) {// 最后有N个为0数字
					if (index >= 0 && index <= endIndex) {// 倒数最后一个不为0的数开始,list最大值结束。
						rs.get(index).setAvgPriceIndex10(CurrencyUitl.roundHalfUp(avg10[i]));
						index++;
					}
				}
			}
			index = 0;
			for (int i = avg5.length - 1; i >= 0; i--) {// 结果倒序开始
				if (avg5[i] != 0.0) {// 最后有N个为0数字
					if (index >= 0 && index <= endIndex) {// 倒数最后一个不为0的数开始,list最大值结束。
						rs.get(index).setAvgPriceIndex5(CurrencyUitl.roundHalfUp(avg5[i]));
						index++;
					}
				}
			}
			return rs;
		} else {
			log.info("多日均线计算错误,code={},startDate={},startDate={}", code, startDate, endDate);
			return null;
		}
	}

//	@PostConstruct
//	private void test() {
//		System.err.println("前复权测试");
//		List<StockAvgBase> out = getSMA5_60("000505", 20180926, 20201225, true);
//		for (StockAvgBase sa : out) {
//			System.err.println(sa.getDate() + " " + sa.getAvgPriceIndex30() + " " + sa.getAvgPriceIndex60());
//		}
//	}

}
