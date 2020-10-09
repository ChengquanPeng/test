package com.stable.utils;

import java.util.LinkedList;
import java.util.List;

import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.stable.service.DaliyTradeHistroyService;
import com.stable.vo.bus.StockAvg;
import com.stable.vo.bus.TradeHistInfoDaliy;
import com.stable.vo.spi.req.EsQueryPageReq;

import lombok.extern.log4j.Log4j2;

@Service
@Log4j2
public class QfqUtil {
	private final int PERIODS_AVERAGE_30 = 30;
	private final int PERIODS_AVERAGE_20 = 20;
	private final int PERIODS_AVERAGE_10 = 10;
	private final int PERIODS_AVERAGE_5 = 5;
	private EsQueryPageReq queryPage30 = new EsQueryPageReq(30);
	private EsQueryPageReq queryPage9999 = new EsQueryPageReq(9999);

	@Autowired
	private DaliyTradeHistroyService daliyTradeHistroyService;

	public List<StockAvg> getSMA5_30(String code, int startDate, int endDate) {
		// 倒序的结果列表
		List<TradeHistInfoDaliy> temp = daliyTradeHistroyService.queryListByCodeWithLastQfq(code, 0, startDate,
				queryPage30, SortOrder.DESC);

		if (temp.size() != 30) {
			throw new RuntimeException(
					"计算错误,前面30个交易日获取数据错误,code=" + code + ",startDate=" + startDate + ",endDate=" + endDate);
		}
		List<TradeHistInfoDaliy> list = daliyTradeHistroyService.queryListByCodeWithLastQfq(code,
				temp.get(temp.size() - 1).getDate(), endDate, queryPage9999, SortOrder.DESC);
		double[] closePrice = new double[list.size()];
		// 顺序的收盘价数组
		int j = 0;
		for (int i = list.size() - 1; i >= 0; i--) {
			closePrice[i] = list.get(j).getClosed();
			j++;
		}

		// 顺序的结果
		double[] avg30 = TaLabUtil.sma(closePrice, PERIODS_AVERAGE_30);
		if (avg30 != null) {
			int dividendDate = DateUtil.getTodayIntYYYYMMDD();
			double[] avg20 = TaLabUtil.sma(closePrice, PERIODS_AVERAGE_20);
			double[] avg10 = TaLabUtil.sma(closePrice, PERIODS_AVERAGE_10);
			double[] avg5 = TaLabUtil.sma(closePrice, PERIODS_AVERAGE_5);

			// 倒序列表
			List<StockAvg> rs = new LinkedList<StockAvg>();
			for (TradeHistInfoDaliy td : list) {
				if (td.getDate() >= startDate) {
					StockAvg sa = new StockAvg();
					sa.setCode(code);
					sa.setDate(td.getDate());
					sa.setLastDividendDate(dividendDate);
					sa.setId();
					rs.add(sa);
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

	public StockAvg getSMA5_30(String code, int date) {
		// 倒序的结果列表
		List<TradeHistInfoDaliy> list = daliyTradeHistroyService.queryListByCodeWithLastQfq(code, 0, date, queryPage30,
				SortOrder.DESC);
		if (list.size() != 30) {
			throw new RuntimeException("计算错误,前面30个交易日获取数据错误.code=" + code + ",date=" + date);
		}
		double[] closePrice = new double[list.size()];
		// 顺序的收盘价数组
		int j = 0;
		for (int i = list.size() - 1; i >= 0; i--) {
			closePrice[i] = list.get(j).getClosed();
			j++;
		}

		double[] out = TaLabUtil.sma(closePrice, PERIODS_AVERAGE_30);
		if (out != null) {
			StockAvg sa = new StockAvg();
			sa.setCode(code);
			sa.setDate(date);
			sa.setId();
			sa.setLastDividendDate(DateUtil.getTodayIntYYYYMMDD());
			// 第一个数
			sa.setAvgPriceIndex30(CurrencyUitl.roundHalfUp(out[0]));

			double[] out20 = TaLabUtil.sma(closePrice, PERIODS_AVERAGE_20);
			for (int i = out20.length - 1; i >= 0; i--) {
				if (out20[i] != 0.0) {
					// 倒数最后一个不为0的数
					sa.setAvgPriceIndex20(CurrencyUitl.roundHalfUp(out20[i]));
					break;
				}
			}
			double[] out10 = TaLabUtil.sma(closePrice, PERIODS_AVERAGE_10);
			for (int i = out10.length - 1; i >= 0; i--) {
				if (out10[i] != 0.0) {
					// 倒数最后一个不为0的数
					sa.setAvgPriceIndex10(CurrencyUitl.roundHalfUp(out10[i]));
					break;
				}
			}
			double[] out5 = TaLabUtil.sma(closePrice, PERIODS_AVERAGE_5);
			for (int i = out5.length - 1; i >= 0; i--) {
				if (out5[i] != 0.0) {
					// 倒数最后一个不为0的数
					sa.setAvgPriceIndex5(CurrencyUitl.roundHalfUp(out5[i]));
					break;
				}
			}
			return sa;
		} else {
			log.info("均线计算错误,code={},date={}", code, date);
			return null;
		}
	}

//	@PostConstruct
//	private void test() {
//		System.err.println("前复权测试");
//		List<StockAvg> out = getSMA5_30("002928", 20180926, 20200930);
//		for (StockAvg sa : out) {
//			System.err.println(sa);
//		}
//
//		System.err.println(getSMA5_30("002928", 20180927));
//		System.err.println(getSMA5_30("002928", 20180928));
//	}

	public static void main(String[] args) {
		double d1 = 51.04499999999999;
		double d2 = 51.214999999999996;
		double d3 = 51.495999999999995;

		System.err.println(CurrencyUitl.roundHalfUp(d1));
		System.err.println(CurrencyUitl.roundHalfUp(d2));
		System.err.println(CurrencyUitl.roundHalfUp(d3));
	}
}
