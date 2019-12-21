package com.stable.web.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.stable.service.DaliyBasicHistroyService;
import com.stable.service.DaliyTradeHistroyService;
import com.stable.vo.http.JsonResult;
import com.stable.vo.spi.req.EsQueryPageReq;

@RequestMapping("/trade/hist")
@RestController
public class TradeHistroyController {

	@Autowired
	private DaliyTradeHistroyService tradeHistroyService;
	@Autowired
	private DaliyBasicHistroyService daliyBasicHistroyService;
	
	/**
	 * 根据code重新获取历史记录（前复权）
	 */
	@RequestMapping(value = "/dailybasic/{code}", method = RequestMethod.GET)
	public ResponseEntity<JsonResult> dailybasic(@PathVariable(value = "code") String code,EsQueryPageReq page) {
		JsonResult r = new JsonResult();
		try {
			r.setResult(daliyBasicHistroyService.queryListByCode(code, page));
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
	@RequestMapping(value = "/qfq/list/{code}", method = RequestMethod.GET)
	public ResponseEntity<JsonResult> qfqlsit(@PathVariable(value = "code") String code,EsQueryPageReq page) {
		JsonResult r = new JsonResult();
		try {
			r.setResult(tradeHistroyService.queryListByCode(code, page));
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
			r.setResult(JsonResult.OK);
		} catch (Exception e) {
			r.setResult(e.getClass().getName() + ":" + e.getMessage());
			r.setStatus(JsonResult.ERROR);
			e.printStackTrace();
		}
		return ResponseEntity.ok(r);
	}

	/**
	 * 获取（前复权）日交易(任务job缓存)
	 */
	@RequestMapping(value = "/qfq/fetchall", method = RequestMethod.GET)
	public ResponseEntity<JsonResult> fetchall() {
		JsonResult r = new JsonResult();
		try {
			tradeHistroyService.jobSpiderAll();
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
	public ResponseEntity<JsonResult> daliyalldirect() {
		JsonResult r = new JsonResult();
		try {
			tradeHistroyService.spiderAllDirect();
		} catch (Exception e) {
			r.setResult(e.getClass().getName() + ":" + e.getMessage());
			r.setStatus(JsonResult.ERROR);
			e.printStackTrace();
		}
		return ResponseEntity.ok(r);
	}
}
