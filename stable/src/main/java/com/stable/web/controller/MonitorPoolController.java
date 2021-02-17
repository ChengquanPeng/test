package com.stable.web.controller;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.stable.constant.EsQueryPageUtil;
import com.stable.service.StockBasicService;
import com.stable.service.monitor.MonitorPoolService;
import com.stable.utils.DateUtil;
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

	/**
	 * 根据code
	 */
	@RequestMapping(value = "/list", method = RequestMethod.GET)
	public ResponseEntity<JsonResult> list(String code, String aliasCode, String monitor, String monitoreq,
			EsQueryPageReq querypage) {
		JsonResult r = new JsonResult();
		try {
			r.setResult(monitorPoolService.getListForWeb(code,
					StringUtils.isNotBlank(monitor) ? Integer.valueOf(monitor) : 0,
					StringUtils.isNotBlank(monitoreq) ? Integer.valueOf(monitoreq) : 0, querypage, aliasCode));
			r.setStatus(JsonResult.OK);
		} catch (Exception e) {
			r.setResult(e.getClass().getName() + ":" + e.getMessage());
			r.setStatus(JsonResult.ERROR);
			e.printStackTrace();
		}
		return ResponseEntity.ok(r);
	}

	@RequestMapping(value = "/byCode", method = RequestMethod.GET)
	public ResponseEntity<JsonResult> byCode(String code) {
		JsonResult r = new JsonResult();
		try {
			List<MonitorPoolResp> l = monitorPoolService.getListForWeb(code, 0, 0, EsQueryPageUtil.queryPage1, "");
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
	public void delMonit(String code, String remark) {
		monitorPoolService.delMonit(code, remark);
	}

	@RequestMapping(value = "/addMonitor")
	public ResponseEntity<JsonResult> addMonitor(String code, String monitor, String realtime, String offline,
			String upPrice, String downPrice, String upTodayChange, String downTodayChange, String remark, String ykb,
			String zfdone, String holderNum, String buyLowVol) {
		JsonResult r = new JsonResult();
		try {
			monitorPoolService.addMonitor(code, StringUtils.isNotBlank(monitor) ? Integer.valueOf(monitor) : 0,
					StringUtils.isNotBlank(realtime) ? Integer.valueOf(realtime) : 0,
					StringUtils.isNotBlank(offline) ? Integer.valueOf(offline) : 0,
					StringUtils.isNotBlank(upPrice) ? Double.valueOf(upPrice) : 0,
					StringUtils.isNotBlank(downPrice) ? Double.valueOf(downPrice) : 0,
					StringUtils.isNotBlank(upTodayChange) ? Double.valueOf(upTodayChange) : 0,
					StringUtils.isNotBlank(downTodayChange) ? Double.valueOf(downTodayChange) : 0, remark,
					StringUtils.isNotBlank(ykb) ? Integer.valueOf(ykb) : 0,
					StringUtils.isNotBlank(zfdone) ? Integer.valueOf(zfdone) : 0,
					StringUtils.isNotBlank(holderNum) ? DateUtil.getTodayIntYYYYMMDD() : 0,
					StringUtils.isNotBlank(buyLowVol) ? Integer.valueOf(buyLowVol) : 0);
			r.setStatus(JsonResult.OK);
		} catch (Exception e) {
			r.setStatus(JsonResult.FAIL);
			r.setResult(e.getMessage());
		}
		return ResponseEntity.ok(r);
	}
}
