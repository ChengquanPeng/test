package com.stable.web.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.stable.service.ChipsService;
import com.stable.spider.eastmoney.EmJiejinSpider;
import com.stable.spider.ths.ThsBonusSpider;
import com.stable.spider.ths.ThsJiejinSpider;
import com.stable.vo.http.JsonResult;
import com.stable.vo.spi.req.EsQueryPageReq;

@RequestMapping("/chips")
@RestController
public class ChipsController {
	@Autowired
	private ThsBonusSpider thsBonusSpider;
	@Autowired
	private ChipsService chipsService;
	@Autowired
	private EmJiejinSpider emJiejinSpider;
	@Autowired
	private ThsJiejinSpider thsJiejinSpider;

	/**
	 * 根据code查询股东人数
	 */
	@RequestMapping(value = "/holdernum/list", method = RequestMethod.GET)
	public ResponseEntity<JsonResult> holdernumlist(String code) {
		JsonResult r = new JsonResult();
		try {
			r.setResult(chipsService.getHolderNumList45(code));
			r.setStatus(JsonResult.OK);
		} catch (Exception e) {
			r.setResult(e.getClass().getName() + ":" + e.getMessage());
			r.setStatus(JsonResult.ERROR);
			e.printStackTrace();
		}
		return ResponseEntity.ok(r);
	}

	/**
	 * 根据code查询增发
	 */
	@RequestMapping(value = "/zengfa/list", method = RequestMethod.GET)
	public ResponseEntity<JsonResult> zengfalist(String code, String status, EsQueryPageReq querypage) {
		JsonResult r = new JsonResult();
		try {
			r.setResult(chipsService.getAddIssueList(code, status, querypage));
			r.setStatus(JsonResult.OK);
		} catch (Exception e) {
			r.setResult(e.getClass().getName() + ":" + e.getMessage());
			r.setStatus(JsonResult.ERROR);
			e.printStackTrace();
		}
		return ResponseEntity.ok(r);
	}

	/**
	 * 最新的增发详情
	 */
	@RequestMapping(value = "/last/zengfadtl", method = RequestMethod.GET)
	public ResponseEntity<JsonResult> lastZengfaDetail(String code) {
		JsonResult r = new JsonResult();
		try {
			r.setResult(chipsService.getLastZengFaDetail(code));
			r.setStatus(JsonResult.OK);
		} catch (Exception e) {
			r.setResult(e.getClass().getName() + ":" + e.getMessage());
			r.setStatus(JsonResult.ERROR);
			e.printStackTrace();
		}
		return ResponseEntity.ok(r);
	}

	/**
	 * 根据code查询解禁
	 */
	@RequestMapping(value = "/jiejin/list", method = RequestMethod.GET)
	public ResponseEntity<JsonResult> AddJiejinlist(String code, EsQueryPageReq querypage) {
		JsonResult r = new JsonResult();
		try {
			r.setResult(chipsService.getAddJiejinList(code, querypage));
			r.setStatus(JsonResult.OK);
		} catch (Exception e) {
			r.setResult(e.getClass().getName() + ":" + e.getMessage());
			r.setStatus(JsonResult.ERROR);
			e.printStackTrace();
		}
		return ResponseEntity.ok(r);
	}

	/**
	 * 同花顺-公告-增发
	 */
	@RequestMapping(value = "/fetchAddIssue", method = RequestMethod.GET)
	public ResponseEntity<JsonResult> fetchAddIssue() {
		JsonResult r = new JsonResult();
		try {
			thsBonusSpider.dofetchBonus();
			r.setResult(JsonResult.OK);
			r.setStatus(JsonResult.OK);
		} catch (Exception e) {
			r.setResult(e.getClass().getName() + ":" + e.getMessage());
			r.setStatus(JsonResult.ERROR);
			e.printStackTrace();
		}
		return ResponseEntity.ok(r);
	}

	/**
	 * 东方财富-历史解禁
	 */
	@RequestMapping(value = "/fetchJiejinDf", method = RequestMethod.GET)
	public ResponseEntity<JsonResult> fetchJiejinDf() {
		JsonResult r = new JsonResult();
		try {
			emJiejinSpider.dofetch();
			r.setResult(JsonResult.OK);
			r.setStatus(JsonResult.OK);
		} catch (Exception e) {
			r.setResult(e.getClass().getName() + ":" + e.getMessage());
			r.setStatus(JsonResult.ERROR);
			e.printStackTrace();
		}
		return ResponseEntity.ok(r);
	}

	/**
	 * 同花顺-解禁
	 */
	@RequestMapping(value = "/fetchJiejinThs", method = RequestMethod.GET)
	public ResponseEntity<JsonResult> fetchJiejinThs() {
		JsonResult r = new JsonResult();
		try {
			thsJiejinSpider.dofetch();
			r.setResult(JsonResult.OK);
			r.setStatus(JsonResult.OK);
		} catch (Exception e) {
			r.setResult(e.getClass().getName() + ":" + e.getMessage());
			r.setStatus(JsonResult.ERROR);
			e.printStackTrace();
		}
		return ResponseEntity.ok(r);
	}
}
