package com.stable.web.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.stable.constant.RedisConstant;
import com.stable.service.DaliyTradeHistroyService;
import com.stable.utils.RedisUtil;
import com.stable.vo.http.JsonResult;

@RequestMapping("/trade/hist")
@RestController
public class TradeHistroyController {

	@Autowired
	private DaliyTradeHistroyService tradeHistroyService;
	@Autowired
	private RedisUtil redisUtil;

	/**
	 * 根据code重新获取历史记录（前复权）
	 */
	@RequestMapping(value = "/daliy/{code}", method = RequestMethod.GET)
	public ResponseEntity<JsonResult> daliycode(@PathVariable(value = "code") String code) {
		JsonResult r = new JsonResult();
		try {
			redisUtil.del(RedisConstant.RDS_TRADE_HIST_LAST_DAY_ + code);
			r.setResult(tradeHistroyService.spiderTodayDaliyTrade(code));
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
	@RequestMapping(value = "/daliyall", method = RequestMethod.GET)
	public ResponseEntity<JsonResult> daliyall() {
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
	@RequestMapping(value = "/daliyall/direct", method = RequestMethod.GET)
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
