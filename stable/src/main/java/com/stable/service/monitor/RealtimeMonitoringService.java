package com.stable.service.monitor;

import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.stable.constant.Constant;
import com.stable.constant.RedisConstant;
import com.stable.msg.MsgPushServer;
import com.stable.service.StockBasicService;
import com.stable.service.TradeCalService;
import com.stable.service.biz.BizPushService;
import com.stable.service.biz.UserService;
import com.stable.service.model.WebModelService;
import com.stable.spider.tick.TencentTickReal;
import com.stable.utils.DateUtil;
import com.stable.utils.RedisUtil;
import com.stable.vo.bus.MonitorPoolTemp;
import com.stable.vo.bus.UserInfo;

import lombok.extern.log4j.Log4j2;

@Service
@Log4j2
public class RealtimeMonitoringService {

	@Autowired
	private MonitorPoolService monitorPoolService;
	@Autowired
	private TradeCalService tradeCalService;
	@Autowired
	private StockBasicService stockBasicService;
	private Map<String, RealtimeDetailsAnalyzer> map = null;
	@Autowired
	private WebModelService modelWebService;
//	@Autowired
//	private ShotPointCheck shotPointCheck;
//	@Autowired
//	private Prd1Service prd1Service;
//	@Autowired
//	private TickService tickService;
	@Autowired
	private RedisUtil redisUtil;
	@Autowired
	private UserService userService;
	@Autowired
	private BizPushService bizPushService;

	public synchronized void startObservable() {
		String date = DateUtil.getTodayYYYYMMDD();
		int idate = Integer.valueOf(date);
		if (!tradeCalService.isOpen(idate)) {
			// WxPushUtil.pushSystem1("非交易日结束监听");
			log.info("非交易日结束监听");
			return;
		}
		long now = new Date().getTime();
		long starttime = DateUtil.parseTodayYYYYMMDDHHMMSS(date + " 09:25:35").getTime();
		long endtime = DateUtil.parseTodayYYYYMMDDHHMMSS(date + " 15:03:00").getTime();
		if (now < starttime || now > endtime) {// 已超时
			log.info("now > isAlivingMillis,已超时");
			return;
		}

		try {
			// 获取监听列表-常规
			List<UserInfo> ulist = userService.getUserListForMonitorS1();
			HashMap<String, List<RtmVo>> allmap = new HashMap<String, List<RtmVo>>();
			for (UserInfo u : ulist) {
				List<MonitorPoolTemp> tl = monitorPoolService.getPoolListForMonitor(u.getId(), 1, 0, getMonisort1());
				if (tl != null) {
					for (MonitorPoolTemp t : tl) {
						if (t.getDownPrice() <= 0 && t.getDownTodayChange() <= 0 && t.getUpPrice() <= 0
								&& t.getUpTodayChange() <= 0) {
							log.info("{} {} 没有在线价格监听", t.getUserId(), t.getCode());
							continue;
						}
						List<RtmVo> ml = allmap.get(t.getCode());
						if (ml == null) {
							ml = new LinkedList<RtmVo>();
						}
						RtmVo rv = new RtmVo(t,
								modelWebService.getLastOneByCodeResp(t.getCode(), t.getUserId() == Constant.MY_ID));
						rv.setWxpush(u.getWxpush());
						ml.add(rv);// code对应的每个人
						allmap.put(t.getCode(), ml);
					}
				}
			}
			// 起爆点监听
			Set<MonitorPoolTemp> tl = monitorPoolService.getMyQibao();
			if (tl != null) {
				for (MonitorPoolTemp t : tl) {
					List<RtmVo> ml = allmap.get(t.getCode());
					if (ml == null) {
						ml = new LinkedList<RtmVo>();
					}

					RtmVo rv = new RtmVo(t,
							modelWebService.getLastOneByCodeResp(t.getCode(), t.getUserId() == Constant.MY_ID));
					rv.setServiceAndPrew(bizPushService);
					ml.add(rv);
					allmap.put(t.getCode(), ml);
				}
			}

			List<RealtimeDetailsAnalyzer> list = new LinkedList<RealtimeDetailsAnalyzer>();
			int failtt = 0;
			if (allmap.size() > 0) {
				// ====启动监听线程====
				map = new ConcurrentHashMap<String, RealtimeDetailsAnalyzer>();
				for (String code : allmap.keySet()) {
					log.info(code);
					RealtimeDetailsAnalyzer task = new RealtimeDetailsAnalyzer();
					int r = task.init(code, allmap.get(code), stockBasicService.getCodeName2(code),
							redisUtil.get(RedisConstant.YEAR_PRICE_ + code, 0.0));
					if (r == 1) {
						new Thread(task).start();
						list.add(task);
						map.put(code, task);
					} else {
						if (r < 0) {
							failtt++;
						}
					}
				}
			}
			MsgPushServer.pushSystem1("实时监听，监听总数:[" + allmap.size() + "],牛熊环境开启:[" + getMonisort1() + "],短线实际总数["
					+ list.size() + "],监听失败[" + failtt + "]");

			// ====产品1：三五天 => 买点 === 卖点 ====
			TencentTickReal.tradeDate = idate;
			// Prd1RealtimeMonitor prd1m = new
			// Prd1RealtimeMonitor(prd1Service.getMonitorList(), tickService, prd1Service);
			// new Thread(prd1m).start();

			long from3 = new Date().getTime();
			int millis = (int) ((endtime - from3));
			if (millis > 0) {
				Thread.sleep(millis);
			}

			/** 到点停止所有线程 */
			for (RealtimeDetailsAnalyzer t : list) {
				t.stop();
			}

			// 停止线程
			// prd1m.stop();
			// OnlineTesting -> 转换持仓量:可卖=vol，今日买归0
			// Map<String, OnlineTesting> testinglist = prd1m.getTestinglist();
//			for (String code : testinglist.keySet()) {
//				OnlineTesting p1 = testinglist.get(code);
//				if (p1.getVol() > 0) {// 未卖完
//					p1.setCanSold(p1.getVol());
//					p1.setBuyToday(0);
//					prd1Service.saveTesting(p1);
//				}
//			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			map = null;
			System.gc();
		}
	}

	public void stopThread(String code) {
		if (map != null) {
			if (map.containsKey(code)) {
				map.get(code).stop();
			}
		}
	}

	private boolean getMonisort1() {
		return redisUtil.get(RedisConstant.MONI_SORT1, 1) == 1;
	}
}
