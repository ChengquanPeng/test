package com.stable.web.controller;

import java.io.PrintWriter;
import java.util.List;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.stable.service.ChipsDzjyService;
import com.stable.service.ChipsService;
import com.stable.service.ChipsZfService;
import com.stable.service.ReducingHoldingSharesService;
import com.stable.spider.eastmoney.JiejinSpider;
import com.stable.spider.igoodstock.IgoodstockSpider;
import com.stable.spider.ths.ThsBonusSpider;
import com.stable.spider.ths.ThsJiejinSpider;
import com.stable.vo.bus.ReducingHoldingShares;
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
	private JiejinSpider emJiejinSpider;
	@Autowired
	private ThsJiejinSpider thsJiejinSpider;
	@Autowired
	private IgoodstockSpider igoodstockSpider;
	@Autowired
	private ChipsDzjyService chipsDzjyService;
	@Autowired
	private ReducingHoldingSharesService reducingHoldingSharesService;

	@RequestMapping(value = "/reduce/list", method = RequestMethod.GET)
	public void holdernumlist(String code, HttpServletResponse response) {
		response.setCharacterEncoding("UTF-8");
		response.setContentType("text/html;charset=utf-8");
		try {
			PrintWriter w = response.getWriter();
			List<ReducingHoldingShares> list = reducingHoldingSharesService.getLastStat(code);
			if (list != null && list.size() > 0) {
				w.write(code + "<table><tr><td width='6%'>日期</td><td width='6%'>股数(万股)</td><td>描述</td><tr/>");
				for (ReducingHoldingShares row : list) {
					w.write("<tr><td>" + row.getDate() + "</td><td>" + row.getWg() + "</td><td>" + row.getDesc()
							+ "</td><tr/>");
				}
				w.write("</table>");
			} else {
				w.write(code + "无数据");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@RequestMapping(value = "/reduce/list2", method = RequestMethod.GET)
	public void holdernumlist2(String code, HttpServletResponse response) {

		try {
			PrintWriter w = response.getWriter();
			w.write(code + "无数据");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

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
	public ResponseEntity<JsonResult> zengfalist(String code, String status, String minVal, String maxVal,
			EsQueryPageReq querypage) {
		JsonResult r = new JsonResult();
		try {
			int min = 0;
			if (StringUtils.isNotBlank(minVal)) {
				min = Integer.valueOf(minVal);
			}
			int max = 0;
			if (StringUtils.isNotBlank(maxVal)) {
				max = Integer.valueOf(maxVal);
			}
			r.setResult(chipsZfService.getZengFaListForWeb(code, status, min, max, querypage));
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

	/**
	 * 根据code查询增发
	 */
	@RequestMapping(value = "/dzjy/list", method = RequestMethod.GET)
	public ResponseEntity<JsonResult> dzjylist(String code, String date, String totalAmt, String totalAmt60d,
			EsQueryPageReq querypage) {
		JsonResult r = new JsonResult();
		try {
			int d = 0;
			if (StringUtils.isNotBlank(date)) {
				d = Integer.valueOf(date);
			}
			int ta = 0;
			if (StringUtils.isNotBlank(totalAmt)) {
				ta = Integer.valueOf(totalAmt);
			}
			int ta6 = 0;
			if (StringUtils.isNotBlank(totalAmt60d)) {
				ta6 = Integer.valueOf(totalAmt60d);
			}
			r.setResult(chipsDzjyService.getDzjyTimeListForWeb(code, d, ta, ta6, querypage));
			r.setStatus(JsonResult.OK);
		} catch (Exception e) {
			r.setResult(e.getClass().getName() + ":" + e.getMessage());
			r.setStatus(JsonResult.ERROR);
			e.printStackTrace();
		}
		return ResponseEntity.ok(r);
	}
}
