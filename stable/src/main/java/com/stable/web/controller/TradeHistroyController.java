package com.stable.web.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.stable.service.DaliyBasicHistroyService;
import com.stable.service.DaliyTradeHistroyService;
import com.stable.service.TradeCalService;
import com.stable.utils.DateUtil;
import com.stable.vo.http.JsonResult;
import com.stable.vo.spi.req.EsQueryPageReq;

@RequestMapping("/trade/hist")
@RestController
public class TradeHistroyController {

	@Autowired
	private DaliyTradeHistroyService tradeHistroyService;
	@Autowired
	private DaliyBasicHistroyService daliyBasicHistroyService;
	@Autowired
	private TradeCalService tradeCalService;

	/**
	 * 根据code重新获取历史记录-每日指标
	 */
	@RequestMapping(value = "/dailybasic/list", method = RequestMethod.GET)
	public ResponseEntity<JsonResult> dailybasic(String code, EsQueryPageReq page) {
		JsonResult r = new JsonResult();
		try {
			r.setStatus(JsonResult.OK);
			r.setResult(daliyBasicHistroyService.queryListByCodeByWebPage(code, page));
		} catch (Exception e) {
			r.setResult(e.getClass().getName() + ":" + e.getMessage());
			r.setStatus(JsonResult.ERROR);
			e.printStackTrace();
		}
		return ResponseEntity.ok(r);
	}

	/**
	 * 根据code重新获取历史记录（前复权）
	 */
	@RequestMapping(value = "/qfq/list", method = RequestMethod.GET)
	public ResponseEntity<JsonResult> qfqlsit(String code, EsQueryPageReq page) {
		JsonResult r = new JsonResult();
		try {
			r.setResult(tradeHistroyService.queryListByCodeByWebPage(code, page));
			r.setStatus(JsonResult.OK);
		} catch (Exception e) {
			r.setResult(e.getClass().getName() + ":" + e.getMessage());
			r.setStatus(JsonResult.ERROR);
			e.printStackTrace();
		}
		return ResponseEntity.ok(r);
	}

	/**
	 * 根据code重新获取历史记录（前复权）
	 */
	@RequestMapping(value = "/qfq/fetch/{code}", method = RequestMethod.GET)
	public ResponseEntity<JsonResult> fetch(@PathVariable(value = "code") String code) {
		JsonResult r = new JsonResult();
		try {
			tradeHistroyService.manualSpiderDaliyTrade(code);
			r.setStatus(JsonResult.OK);
		} catch (Exception e) {
			r.setResult(e.getClass().getName() + ":" + e.getMessage());
			r.setStatus(JsonResult.ERROR);
			e.printStackTrace();
		}
		return ResponseEntity.ok(r);
	}

	/**
	 * 获取（前复权）日交易(任务job失败)
	 */
	@RequestMapping(value = "/qfq/fetchall", method = RequestMethod.GET)
	public ResponseEntity<JsonResult> fetchall() {
		JsonResult r = new JsonResult();
		try {
			int date = DateUtil.getTodayIntYYYYMMDD();
			if (!tradeCalService.isOpen(date)) {
				date = tradeCalService.getPretradeDate(date);
			}
			tradeHistroyService.spiderTodayDaliyTrade(false, date + "");
			r.setStatus(JsonResult.OK);
		} catch (Exception e) {
			r.setResult(e.getClass().getName() + ":" + e.getMessage());
			r.setStatus(JsonResult.ERROR);
			e.printStackTrace();
		}
		return ResponseEntity.ok(r);
	}

	/**
	 * 重新获取（前复权）日交易
	 */
	@RequestMapping(value = "/qfq/fetchallDirect", method = RequestMethod.GET)
	public ResponseEntity<JsonResult> fetchallDirect() {
		JsonResult r = new JsonResult();
		try {
			tradeHistroyService.spiderAllDirect();
			r.setStatus(JsonResult.OK);
		} catch (Exception e) {
			r.setResult(e.getClass().getName() + ":" + e.getMessage());
			r.setStatus(JsonResult.ERROR);
			e.printStackTrace();
		}
		return ResponseEntity.ok(r);
	}
}
