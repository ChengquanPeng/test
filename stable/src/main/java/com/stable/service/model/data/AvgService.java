package com.stable.service.model.data;

import java.util.LinkedList;
import java.util.List;

import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.stable.service.DaliyTradeHistroyService;
import com.stable.spider.tushare.TushareSpider;
import com.stable.utils.ErrorLogFileUitl;
import com.stable.utils.PythonCallUtil;
import com.stable.utils.SMAUtil;
import com.stable.utils.ThreadsUtil;
import com.stable.vo.bus.StockAvg;
import com.stable.vo.bus.StockAvgBase;
import com.stable.vo.bus.StockAvgNofq;
import com.stable.vo.bus.TradeHistInfoDaliy;
import com.stable.vo.bus.TradeHistInfoDaliyNofq;
import com.stable.vo.spi.req.EsQueryPageReq;

import lombok.extern.log4j.Log4j2;

@Service
@Log4j2
public class AvgService {

	@Value("${python.file.daily.avg}")
	private String pythonFileName;
	@Autowired
	private DaliyTradeHistroyService daliyTradeHistroyService;
	@Autowired
	private SMAUtil smaUtil;

	public List<StockAvgBase> getSMA30(String code, int startDate, int endDate, boolean isQfq) {
		List<StockAvgBase> rs = smaUtil.getSMA5_30(code, startDate, endDate, isQfq);
		if (rs != null) {
			log.info("SMA-30 -> From 本地计算 code={},startDate={},endDate={},size={}", code, startDate, endDate,
					rs.size());
			return rs;
		} else {
			rs = getDPriceAvgFromTushar(code, startDate, endDate, isQfq);
			log.info("SMA-30 -> From Thshare");
			return rs;
		}
	}

	private List<StockAvgBase> getSMA60(String code, int startDate, int endDate, boolean isQfq) {
		List<StockAvgBase> rs = smaUtil.getSMA5_60(code, startDate, endDate, isQfq);
		if (rs != null) {
			log.info("SMA-60 -> From 本地计算 code={},startDate={},endDate={},size={}", code, startDate, endDate,
					rs.size());
			return rs;
		} else {
			rs = getDPriceAvgFromTushar(code, startDate, endDate, isQfq);
			log.info("SMA-60 -> From Thshare");
			return rs;
		}
	}

	// 比如5日均线，开始和结束日期参数跨度必须要超过5日。
	// 本方法要超过250个交易日。
	private List<StockAvgBase> getDPriceAvgFromTushar(String code, int startDate, int endDate, boolean isQfq) {
		String params = "";
		if (isQfq) {
			params = TushareSpider.formatCode(code) + " " + startDate + " " + endDate + " qfq D";
		} else {
			params = TushareSpider.formatCode(code) + " " + startDate + " " + endDate + " None D";
		}

		boolean gotData = false;
		List<String> lines = null;
		int i = 0;
		do {
			// 最多返回30条, 而且日期倒序返回
			lines = PythonCallUtil.callPythonScript(pythonFileName, params);
			if (lines == null || lines.isEmpty() || lines.get(0).startsWith(PythonCallUtil.EXCEPT)) {
				if (i >= 3) {
					log.warn("pythonFileName：{}，未获取到数据 params：{}", pythonFileName, code, params);
					if (lines != null && !lines.isEmpty()) {
						log.error("Python 错误：code：{}，PythonCallUtil.EXCEPT：{}", code, lines.get(0));
					}
					ErrorLogFileUitl.writeError(new RuntimeException(), code, "未获取到均价信息", startDate + " " + endDate);
					return null;
				}
				ThreadsUtil.sleepRandomSecBetween5And15();
			} else {
				gotData = true;
			}
			i++;
		} while (!gotData);

		try {
			List<StockAvgBase> list30 = new LinkedList<StockAvgBase>();
			for (int j = 0; j < 30; j++) {
				String[] strs = lines.get(j).replaceAll("nan", "0").split(",");
				// code,date,3,5,10,20,30,120,250
				// 600408.SH,20200403,2.2933,2.302,2.282,2.3255,2.297,2.2712,2.4559
				StockAvgBase av;
				if (isQfq) {
					av = new StockAvg();
				} else {
					av = new StockAvgNofq();
				}
				av.setCode(code);
				av.setDate(Integer.valueOf(strs[1]));
				av.setAvgPriceIndex5(Double.valueOf(strs[3]));
				av.setAvgPriceIndex10(Double.valueOf(strs[4]));
				av.setAvgPriceIndex20(Double.valueOf(strs[5]));
				av.setAvgPriceIndex30(Double.valueOf(strs[6]));
				av.setAvgPriceIndex60(Double.valueOf(strs[7]));
				av.setAvgPriceIndex120(Double.valueOf(strs[8]));
				av.setAvgPriceIndex250(Double.valueOf(strs[9]));
				list30.add(av);
			}
			return list30;
		} catch (Exception e) {
			String msg = "获取到的数据:" + lines.get(0);
//			log.error(msg);
			e.printStackTrace();
			throw new RuntimeException(msg, e);
		}
	}

	public List<StockAvgBase> queryListByCodeForModelWithLastN(String code, int endDate, EsQueryPageReq queryPage,
			boolean isQfq, boolean is60) {
		int sd = 0;
		if (isQfq) {
			List<TradeHistInfoDaliy> tradedaliylist = daliyTradeHistroyService.queryListByCodeWithLastQfq(code, 0,
					endDate, queryPage, SortOrder.DESC);
			if (tradedaliylist != null) {
				sd = tradedaliylist.get(tradedaliylist.size() - 1).getDate();
			}
		} else {
			List<TradeHistInfoDaliyNofq> tradedaliylistNofq = daliyTradeHistroyService.queryListByCodeWithLastNofq(code,
					0, endDate, queryPage, SortOrder.DESC);
			if (tradedaliylistNofq != null) {
				sd = tradedaliylistNofq.get(tradedaliylistNofq.size() - 1).getDate();
			}
		}
		if (sd > 0) {
			if (is60) {// 60日均线
				List<StockAvgBase> result = getSMA60(code, sd, endDate, isQfq);
				return result;
			} else {// 30日均线
				List<StockAvgBase> result = getSMA30(code, sd, endDate, isQfq);
				return result;
			}
		}
		return null;
	}

}
