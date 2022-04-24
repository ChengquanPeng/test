package com.stable.web.controller;

import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.stable.constant.Constant;
import com.stable.constant.EsQueryPageUtil;
import com.stable.service.StockBasicService;
import com.stable.service.monitor.MonitorPoolService;
import com.stable.vo.bus.MonitorPoolTemp;
import com.stable.vo.bus.UserInfo;
import com.stable.vo.http.JsonResult;
import com.stable.vo.http.resp.MonitorPoolResp;
import com.stable.vo.spi.req.EsQueryPageReq;

@RequestMapping("/monitorPool")
@RestController
public class MonitorPoolController {
	@Autowired
	private StockBasicService stockBasicService;
	@Autowired
	private MonitorPoolService monitorPoolService;

	private long getUserId(HttpServletRequest req) {
		UserInfo l = (UserInfo) req.getSession().getAttribute(Constant.SESSION_USER);
		if (l != null && l.getId() >= Constant.MY_ID) {
			return l.getId();
		}
		throw new RuntimeException("无法获取userId");
	}

	/**
	 * 根据code
	 */
	@RequestMapping(value = "/list", method = RequestMethod.GET)
	public ResponseEntity<JsonResult> list(String code, String aliasCode, String monitoreq, EsQueryPageReq querypage,
			HttpServletRequest req) {
		JsonResult r = new JsonResult();
		try {
			int mq = StringUtils.isNotBlank(monitoreq) ? Integer.valueOf(monitoreq) : 0;
			int q1 = (mq == 99 ? 1 : 0);
			int q2 = (mq == 99 ? 0 : mq);
			r.setResult(monitorPoolService.getListForWeb(getUserId(req), code, q1, q2, querypage, aliasCode));
			r.setStatus(JsonResult.OK);
		} catch (Exception e) {
			r.setResult(e.getClass().getName() + ":" + e.getMessage());
			r.setStatus(JsonResult.ERROR);
			e.printStackTrace();
		}
		return ResponseEntity.ok(r);
	}

	@RequestMapping(value = "/byCode", method = RequestMethod.GET)
	public ResponseEntity<JsonResult> byCode(String code, HttpServletRequest req) {
		JsonResult r = new JsonResult();
		try {
			List<MonitorPoolResp> l = monitorPoolService.getListForWeb(getUserId(req), code, 0, 0,
					EsQueryPageUtil.queryPage1, "");
			if (l != null && l.size() > 0) {
				r.setResult(l.get(0));
			} else {
				MonitorPoolResp rk = new MonitorPoolResp();
				rk.setCode(code);
				rk.setCodeName(stockBasicService.getCodeName(code));
				rk.setRemark("");
				r.setResult(rk);
			}
			r.setStatus(JsonResult.OK);
		} catch (Exception e) {
			r.setResult(e.getClass().getName() + ":" + e.getMessage());
			r.setStatus(JsonResult.ERROR);
			e.printStackTrace();
		}
		return ResponseEntity.ok(r);
	}

	@RequestMapping(value = "/delMonit")
	public ResponseEntity<JsonResult> delMonit(String code, String remark, HttpServletRequest req) {
		JsonResult r = new JsonResult();
		try {
			monitorPoolService.delMonit(getUserId(req), code, remark);
			r.setStatus(JsonResult.OK);
		} catch (Exception e) {
			r.setResult(e.getClass().getName() + ":" + e.getMessage());
			r.setStatus(JsonResult.ERROR);
			e.printStackTrace();
		}
		return ResponseEntity.ok(r);
	}

	@RequestMapping(value = "/addMonitor")
	public ResponseEntity<JsonResult> addMonitor(String code, String monitor, String realtime, String offline,
			String upPrice, String downPrice, String upTodayChange, String downTodayChange, String remark, String ykb,
			String zfdone, String holderNum, String buyLowVol, String xjl, String dzjy, String listenerGg,
			String shotPointCheck, HttpServletRequest req) {
		JsonResult r = new JsonResult();
		try {
			long userId = getUserId(req);
			if (Integer.valueOf(monitor) > 0 && userId != Constant.MY_ID) {
				List<MonitorPoolTemp> tl = monitorPoolService.getMonitorPool(userId, true);
				if (tl != null && tl.size() > 20) {
					r.setStatus(JsonResult.FAIL);
					r.setResult("目前只支持20只股票的监听，后续扩容");
					return ResponseEntity.ok(r);
				}
			}
			monitorPoolService.addMonitor(userId, code, StringUtils.isNotBlank(monitor) ? Integer.valueOf(monitor) : 0,
					StringUtils.isNotBlank(realtime) ? Integer.valueOf(realtime) : 0,
					StringUtils.isNotBlank(offline) ? Integer.valueOf(offline) : 0,
					StringUtils.isNotBlank(upPrice) ? Double.valueOf(upPrice) : 0,
					StringUtils.isNotBlank(downPrice) ? Double.valueOf(downPrice) : 0,
					StringUtils.isNotBlank(upTodayChange) ? Double.valueOf(upTodayChange) : 0,
					StringUtils.isNotBlank(downTodayChange) ? Double.valueOf(downTodayChange) : 0, remark,
					StringUtils.isNotBlank(ykb) ? Integer.valueOf(ykb) : 0,
					StringUtils.isNotBlank(zfdone) ? Integer.valueOf(zfdone) : 0,
					StringUtils.isNotBlank(holderNum) ? Integer.valueOf(holderNum) : 0,
					StringUtils.isNotBlank(buyLowVol) ? Integer.valueOf(buyLowVol) : 0,
					StringUtils.isNotBlank(xjl) ? Integer.valueOf(xjl) : 0,
					StringUtils.isNotBlank(dzjy) ? Integer.valueOf(dzjy) : 0,
					StringUtils.isNotBlank(listenerGg) ? Integer.valueOf(listenerGg) : 0,
					StringUtils.isNotBlank(shotPointCheck) ? Integer.valueOf(shotPointCheck) : 0);
			r.setStatus(JsonResult.OK);
		} catch (Exception e) {
			r.setStatus(JsonResult.FAIL);
			r.setResult(e.getMessage());
		}
		return ResponseEntity.ok(r);
	}
}
