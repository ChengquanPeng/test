package com.stable.web.controller;

import java.io.PrintWriter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.stable.constant.Constant;
import com.stable.service.StockBasicService;
import com.stable.service.TradeCalService;
import com.stable.service.model.RunModelService;
import com.stable.service.model.WebModelService;
import com.stable.utils.DateUtil;
import com.stable.vo.bus.CodeBaseModel2;
import com.stable.vo.bus.UserInfo;
import com.stable.vo.http.JsonResult;
import com.stable.vo.http.req.ModelManulReq;
import com.stable.vo.http.req.ModelReq;
import com.stable.vo.http.resp.CodeBaseModelResp;
import com.stable.vo.spi.req.EsQueryPageReq;

import lombok.extern.log4j.Log4j2;

@RequestMapping("/model")
@RestController
@Log4j2
public class CodeModelController {
	@Autowired
	private WebModelService modelWebService;
	@Autowired
	private RunModelService runModelService;
	@Autowired
	private TradeCalService tradeCalService;
	@Autowired
	private StockBasicService stockBasicService;

	@RequestMapping(value = "/list", method = RequestMethod.GET)
	public ResponseEntity<JsonResult> codemodellist(ModelReq mr, EsQueryPageReq querypage, HttpServletRequest req) {
		JsonResult r = new JsonResult();
		try {
			UserInfo l = (UserInfo) req.getSession().getAttribute(Constant.SESSION_USER);
			mr.setCode(mr.getCode().trim());
			r.setResult(modelWebService.getListForWeb(mr, querypage, l.getId()));
			r.setStatus(JsonResult.OK);
		} catch (Exception e) {
			r.setResult(e.getClass().getName() + ":" + e.getMessage());
			r.setStatus(JsonResult.ERROR);
			e.printStackTrace();
		}
		return ResponseEntity.ok(r);
	}

	/**
	 * 执行模型（基本面)
	 */
	@RequestMapping(value = "/runModel", method = RequestMethod.GET)
	public ResponseEntity<JsonResult> runModel() {
		JsonResult r = new JsonResult();
		try {
			int date = DateUtil.getTodayIntYYYYMMDD();
			date = tradeCalService.getPretradeDate(date);
			// codeModelService.reset();
			runModelService.runModel(date, false);
			r.setStatus(JsonResult.OK);
		} catch (Exception e) {
			r.setResult(e.getClass().getName() + ":" + e.getMessage());
			r.setStatus(JsonResult.ERROR);
			e.printStackTrace();
		}
		return ResponseEntity.ok(r);
	}

	@RequestMapping(value = "/runCode", method = RequestMethod.GET)
	public void runModel(String code, String date, HttpServletResponse response) {
		log.info("RunModelService code:" + code + ",date=" + date);
		CodeBaseModel2 cbm = runModelService.runModelForCode(code, date);
		String msg = stockBasicService.getCodeName2(code);
		msg += Constant.HTML_LINE + "大7: " + (cbm.getDibuQixing() > 0 ? cbm.getDibuQixing() : "否");
		msg += Constant.HTML_LINE + "小7: " + (cbm.getDibuQixing2() > 0 ? cbm.getDibuQixing2() : "否");
		msg += Constant.HTML_LINE + "N型: " + (cbm.getNxipan() > 0 ? cbm.getNxipan() : "否");
		msg += Constant.HTML_LINE + "v1洗盘: " + (cbm.getXipan() > 0 ? cbm.getXipan() : "否");
		msg += Constant.HTML_LINE + "十字星: " + (cbm.getZyxing() > 0 ? cbm.getZyxing() : "否");
		try {
			response.setCharacterEncoding("UTF-8");
			response.setContentType("text/html;charset=utf-8");
			PrintWriter w = response.getWriter();
			w.write(msg);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * 人工
	 */
	@RequestMapping(value = "/addManual")
	public ResponseEntity<JsonResult> addManual(ModelManulReq r1, HttpServletRequest req) {
		JsonResult r = new JsonResult();
		try {
			r.setStatus(JsonResult.FAIL);
			UserInfo l = (UserInfo) req.getSession().getAttribute(Constant.SESSION_USER);
			if (l != null && l.getId() == Constant.MY_ID) {
				modelWebService.addPlsManual(Constant.MY_ID, r1);
				r.setStatus(JsonResult.OK);
			}
		} catch (Exception e) {
			r.setStatus(JsonResult.FAIL);
			r.setResult(e.getMessage());
		}
		return ResponseEntity.ok(r);
	}

	/**
	 * chips
	 */
	@RequestMapping(value = "/chips")
	public ResponseEntity<JsonResult> chips(String code, HttpServletRequest req) {
		JsonResult r = new JsonResult();
		try {
			CodeBaseModelResp cbm = modelWebService.getLastOneByCodeResp(code, true);
			r.setResult(cbm);
			r.setStatus(JsonResult.OK);
		} catch (Exception e) {
			r.setStatus(JsonResult.FAIL);
			r.setResult(e.getMessage());
		}
		return ResponseEntity.ok(r);
	}

	/**
	 * chips
	 */
	@RequestMapping(value = "/tushi")
	public ResponseEntity<JsonResult> tushi(String code, String tushi) {
		JsonResult r = new JsonResult();
		try {
			r.setResult("失败了哦");
			r.setStatus(JsonResult.FAIL);

			if (!"1".equals(tushi)) {
				r.setResult("tushi=1 ?");
			} else {
				String name = stockBasicService.getCode(code).getName();
				if (!stockBasicService.isTuiShi(name)) {// 同步数据显示已经退市，但本系统不是退市，则更新
					stockBasicService.synName(code, "手动退");
					r.setResult("成功");
					r.setStatus(JsonResult.OK);
				} else {
					r.setResult(name);
				}
			}
		} catch (Exception e) {
			r.setStatus(JsonResult.FAIL);
			r.setResult(e.getMessage());
		}
		return ResponseEntity.ok(r);
	}

	/**
	 * 人工-融资融券
	 */
	@RequestMapping(value = "/rzrqm")
	public ResponseEntity<JsonResult> rzrqm(ModelManulReq r1) {
		JsonResult r = new JsonResult();
		try {
			modelWebService.rzrqm(r1.getCode(), r1.getTimemonth());
			r.setStatus(JsonResult.OK);
		} catch (Exception e) {
			r.setStatus(JsonResult.FAIL);
			r.setResult(e.getMessage());
		}
		return ResponseEntity.ok(r);
	}

	/**
	 * 人工-小票突然大宗
	 */
	@RequestMapping(value = "/dzpc")
	public ResponseEntity<JsonResult> dzjyBreaks(ModelManulReq r1) {
		JsonResult r = new JsonResult();
		try {
			modelWebService.dapc(r1.getCode(), r1.getDzjyBreaks());
			r.setStatus(JsonResult.OK);
		} catch (Exception e) {
			r.setStatus(JsonResult.FAIL);
			r.setResult(e.getMessage());
		}
		return ResponseEntity.ok(r);
	}
}
