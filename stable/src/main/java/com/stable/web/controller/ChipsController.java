package com.stable.web.controller;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.stable.service.ChipsService;
import com.stable.service.ChipsZfService;
import com.stable.spider.eastmoney.EmJiejinSpider;
import com.stable.spider.igoodstock.IgoodstockSpider;
import com.stable.spider.ths.ThsBonusSpider;
import com.stable.spider.ths.ThsJiejinSpider;
import com.stable.vo.bus.ZengFaDetail;
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
	private ChipsZfService chipsZfService;
	@Autowired
	private EmJiejinSpider emJiejinSpider;
	@Autowired
	private ThsJiejinSpider thsJiejinSpider;
	@Autowired
	private IgoodstockSpider igoodstockSpider;

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
			r.setResult(chipsZfService.getZengFaListForWeb(code, status, querypage));
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
	public Object lastZengfaDetail(String code, String date) {
		String s = "未找到记录";
		int d = 0;
		if (StringUtils.isNotBlank(date)) {
			try {
				d = Integer.valueOf(date);
			} catch (Exception e) {
			}
		}
		ZengFaDetail zf = chipsZfService.getLastZengFaDetail(code, d);
		if (zf.getDate() == 0 && d > 0) {
			zf = chipsZfService.getLastZengFaDetail(code, 0);
		}
		if (zf != null && StringUtils.isNotBlank(zf.getDetails())) {
			s = zf.getDetails().replaceAll("\\n", "</br>");
		}
		return s;
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
	 * 同花顺-增发
	 */
	@RequestMapping(value = "/fetchZengfa", method = RequestMethod.GET)
	public ResponseEntity<JsonResult> fetchZengfa() {
		JsonResult r = new JsonResult();
		try {
			thsBonusSpider.byWeb();
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
	 * 同花顺-增发-ext
	 */
	@RequestMapping(value = "/fetchZengfaExt", method = RequestMethod.GET)
	public ResponseEntity<JsonResult> fetchZengfaExt(String date) {
		JsonResult r = new JsonResult();
		try {
			new Thread(new Runnable() {
				@Override
				public void run() {
					int d = -360;
					if (StringUtils.isNotBlank(date)) {
						try {
							d = -Integer.valueOf(date);
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
					chipsZfService.jobZengFaExt(false, d);
				}
			}).start();
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
			thsJiejinSpider.byWeb();
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
	 * 外资持股
	 */
	@RequestMapping(value = "/fetchForeignCap", method = RequestMethod.GET)
	public ResponseEntity<JsonResult> fetchForeignCap() {
		JsonResult r = new JsonResult();
		try {
			igoodstockSpider.byWeb();
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
