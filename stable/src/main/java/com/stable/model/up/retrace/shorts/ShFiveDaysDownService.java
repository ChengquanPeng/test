package com.stable.model.up.retrace.shorts;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Semaphore;

import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.stable.enums.RunCycleEnum;
import com.stable.enums.RunLogBizTypeEnum;
import com.stable.job.MyCallable;
import com.stable.service.DaliyTradeHistroyService;
import com.stable.service.StockBasicService;
import com.stable.utils.DateUtil;
import com.stable.utils.DoubleUtil;
import com.stable.utils.RetraceLogFileUitl;
import com.stable.utils.TasksWorker;
import com.stable.vo.bus.StockBaseInfo;
import com.stable.vo.bus.TradeHistInfoDaliy;
import com.stable.vo.spi.req.EsQueryPageReq;
import com.stable.vo.up.strategy.ShFiveDaysDown;

import lombok.extern.log4j.Log4j2;

@Service
@Log4j2
public class ShFiveDaysDownService {
	@Autowired
	private StockBasicService stockBasicService;
	@Autowired
	private DaliyTradeHistroyService daliyTradeHistroyService;
	private final Semaphore semap = new Semaphore(1);

	/**
	 * 最近10条
	 */
	private void realTimeTrace() {
		EsQueryPageReq page = new EsQueryPageReq();
		page.setPageSize(10);// 1年200多，10年2000多，20年，5000条
		this.toworking(page);
	}

	/**
	 * 历史回测
	 */
//	@PostConstruct
	private void historyTrace() {
		EsQueryPageReq page = new EsQueryPageReq();
		page.setPageSize(10000);// 1年200多，10年2000多，20年，5000条
		this.toworking(page);
	}

	RetraceLogFileUitl retracefile = new RetraceLogFileUitl("ShFiveDaysDown.log");
	private List<ShFiveDaysDown> samples = null;

	private void toworking(EsQueryPageReq page) {
		try {
			semap.acquire();
			samples = new LinkedList<ShFiveDaysDown>();
			toFileLog("==========start========");
			toFileLog("==date:" + DateUtil.getTodayYYYYMMDDHHMMSS() + ",pageSize:" + page.getPageSize());
			toFileLog("==========started========");
			List<StockBaseInfo> list = stockBasicService.getAllOnStatusList();
			for (StockBaseInfo si : list) {
				List<TradeHistInfoDaliy> listr = daliyTradeHistroyService.queryListByCode(si.getCode(), page,
						SortOrder.ASC);
				if (listr != null) {
					LinkedList<TradeHistInfoDaliy> ll = new LinkedList<TradeHistInfoDaliy>();
					ll.addAll(listr);
					int size = ll.size();
					log.info("代码code:{},查询到RK前复权记录records:{}", si.getCode(), size);

					for (int i = 0; i < size; i++) {
						ShFiveDaysDown record = this.retrace(ll);
						if (record != null) {
							// esShFiveDaysDownDao.save(record);
							samples.add(record);
						}
						if (ll.size() > 0) {
							ll.remove(0);
						}
					}
				} else {
					log.info("代码code:{},查询到RK前复权记录records:0", si.getCode());
				}
			}
			getResult();
			toFileLog("==========end========");
		} catch (Exception e) {
			e.printStackTrace();
			toFileLog(e.getMessage());
		} finally {
			semap.release();
		}
	}

	private void toFileLog(String str) {
		retracefile.writeLine(str);
	}

	private void getResult() {
		List<ShFiveDaysDown> l6s = new LinkedList<ShFiveDaysDown>();
		List<ShFiveDaysDown> l6f = new LinkedList<ShFiveDaysDown>();
		List<ShFiveDaysDown> l7s = new LinkedList<ShFiveDaysDown>();
		List<ShFiveDaysDown> l7f = new LinkedList<ShFiveDaysDown>();
		List<ShFiveDaysDown> l8s = new LinkedList<ShFiveDaysDown>();
		List<ShFiveDaysDown> l8f = new LinkedList<ShFiveDaysDown>();

		toFileLog("采样记录总数：" + samples.size());
		for (ShFiveDaysDown r : samples) {
			// 第6天
			if (r.getIsThe6thDayUp() == 1) {
				l6s.add(r);
			} else {
				l6f.add(r);
			}
			// 第7天
			if (r.getIsThe7thDayUp() == 1) {
				l7s.add(r);
			} else if (r.getIsThe7thDayUp() == 0) {
				l7f.add(r);
			}
			// 第8天
			if (r.getIsThe8thDayUp() == 1) {
				l8s.add(r);
			} else if (r.getIsThe8thDayUp() == 0) {
				l8f.add(r);
			}
		}
		int total6 = (l6s.size() + l6f.size());
		int total7 = (l7s.size() + l7f.size());
		int total8 = (l8s.size() + l8f.size());
		toFileLog("采样记录第6天的总数：" + (total6) + ",上涨数：" + l6s.size() + "("
				+ DoubleUtil.formatDouble2Bit(Double.valueOf(l6s.size()) / Double.valueOf(total6)) + "),下跌数："
				+ l6f.size() + "(" + DoubleUtil.formatDouble2Bit(Double.valueOf(l6f.size()) / Double.valueOf(total6))
				+ ")");
		toFileLog("采样记录第7天的总数：" + (total7) + ",上涨数：" + l7s.size() + "("
				+ DoubleUtil.formatDouble2Bit(Double.valueOf(l7s.size()) / Double.valueOf(total7)) + "),下跌数："
				+ l7f.size() + "(" + DoubleUtil.formatDouble2Bit(Double.valueOf(l7f.size()) / Double.valueOf(total7))
				+ ")");
		toFileLog("采样记录第8天的总数：" + (total8) + ",上涨数：" + l8s.size() + "("
				+ DoubleUtil.formatDouble2Bit(Double.valueOf(l8s.size()) / Double.valueOf(total8)) + "),下跌数："
				+ l8f.size() + "(" + DoubleUtil.formatDouble2Bit(Double.valueOf(l8f.size()) / Double.valueOf(total8))
				+ ")");
		toFileLog("======detail========");
		toFileLog("======6s========");
		printDetail(l6s);
		toFileLog("======6f========");
		printDetail(l6f);
		toFileLog("======7s========");
		printDetail(l7s);
		toFileLog("======7f========");
		printDetail(l7f);
		toFileLog("======8s========");
		printDetail(l8s);
		toFileLog("======8f========");
		printDetail(l8f);
	}

	private void printDetail(List<ShFiveDaysDown> list) {
		for (ShFiveDaysDown r : list) {
			toFileLog(r.toString());
		}
	}

	private ShFiveDaysDown retrace(List<TradeHistInfoDaliy> list) {
		if (list == null || list.size() < 5) {
			return null;
		}
		TradeHistInfoDaliy d1 = list.get(0);
		TradeHistInfoDaliy d2 = list.get(1);
		TradeHistInfoDaliy d3 = list.get(2);
		TradeHistInfoDaliy d4 = list.get(3);
		TradeHistInfoDaliy d5 = list.get(4);
		boolean isCondition = false;
		// 价格依次下跌
		if (d1.getClosed() > d2.getClosed() && d2.getClosed() > d3.getClosed() && d3.getClosed() > d4.getClosed()
				&& d4.getClosed() > d5.getClosed()) {
			// 股票下跌10%
			double p10 = d1.getYesterdayPrice() * 0.1;
			double realdyDown = d1.getYesterdayPrice() - d5.getClosed();
			log.info("d1昨收价格:{},d5收盘价格:{},5天实际下跌价格:{},理论10%:{},是否下跌10%:{}", d1.getYesterdayPrice(), d5.getClosed(),
					realdyDown, p10, (realdyDown >= p10));
			if (realdyDown >= p10) {
				// 交易量依次下跌
				if (d1.getVolume() > d2.getVolume() && d2.getVolume() > d3.getVolume()
						&& d3.getVolume() > d4.getVolume() && d4.getVolume() > d5.getVolume()) {
					double total4 = d1.getVolume() + d2.getVolume() + d3.getVolume() + d4.getVolume();
					double avg4 = total4 / 4;
					double p30 = avg4 * 0.3;
					log.info("前4天交易总量:{},平均交易量:{},平均交易量的3分子1的量:{},是否满足条件:{}", total4, avg4, p30,
							(p30 >= d5.getVolume()));
					if (p30 >= d5.getVolume()) {
						isCondition = true;
					}
				}
			}
		}

		if (isCondition) {
			ShFiveDaysDown r = new ShFiveDaysDown();
			TradeHistInfoDaliy d6 = list.get(5);
			if (d6.getTodayChangeRate() > 0) {
				r.setIsThe6thDayUp(1);
			} else {
				r.setIsThe6thDayUp(0);
			}
			r.setThe6thChange(d6.getTodayChange());
			r.setThe6thChangeRate(DoubleUtil.formatDouble2Bit(d6.getTodayChangeRate()));

			if (list.size() > 6) {
				TradeHistInfoDaliy d7 = list.get(6);
				if (d7.getClosed() > d5.getClosed()) {
					r.setIsThe7thDayUp(1);
				} else {
					r.setIsThe7thDayUp(0);
				}
				r.setThe7thChange(d7.getClosed() - d5.getClosed());
				r.setThe7thChangeRate(DoubleUtil.getRateDouble2Bit((d7.getClosed() - d5.getClosed()), d5.getClosed()));
			} else {
				r.setIsThe7thDayUp(-1);
				r.setThe7thChange(0);
				r.setThe7thChangeRate(0);
			}
			if (list.size() > 7) {
				TradeHistInfoDaliy d8 = list.get(7);
				if (d8.getClosed() > d5.getClosed()) {
					r.setIsThe8thDayUp(1);
				} else {
					r.setIsThe8thDayUp(0);
				}
				r.setThe8thChange(d8.getClosed() - d5.getClosed());
				r.setThe8thChangeRate(DoubleUtil.getRateDouble2Bit((d8.getClosed() - d5.getClosed()), d5.getClosed()));
			} else {
				r.setIsThe8thDayUp(-1);
				r.setThe8thChange(0);
				r.setThe8thChangeRate(0);
			}
			r.setCode(d1.getCode());
			r.setFirstDay(d1.getDate());
			r.genRecordId();
			return r;
		} else {
			return null;
		}

	}

	public void jobRetraceEveryDay() {
		TasksWorker.getInstance().getService()
				.submit(new MyCallable(RunLogBizTypeEnum.RETRACE_SH_FIVEDAY, RunCycleEnum.DAY) {
					@Override
					public Object mycall() {
						realTimeTrace();
						return null;
					}

				});
	}

	public void manualHistory() {
		TasksWorker.getInstance().getService()
				.submit(new MyCallable(RunLogBizTypeEnum.RETRACE_SH_FIVEDAY, RunCycleEnum.MANUAL) {
					@Override
					public Object mycall() {
						historyTrace();
						return null;
					}

				});
	}

}
