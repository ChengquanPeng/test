package com.stable.web.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import com.stable.service.ConceptService;
import com.stable.service.StockBasicService;
import com.stable.service.model.CodeModelService;
import com.stable.service.realtime.MonitoringService;
import com.stable.vo.bus.CodeBaseModel;
import com.stable.vo.bus.CodeBaseModelHist;
import com.stable.vo.http.JsonResult;
import com.stable.vo.http.resp.ReportVo;
import com.stable.vo.http.resp.ViewVo;
import com.stable.vo.spi.req.EsQueryPageReq;

@Controller
public class CodeController {

	@Autowired
	private MonitoringService monitoringService;
	@Autowired
	private CodeModelService codeModelService;
	@Autowired
	private StockBasicService stockBasicService;
	@Autowired
	private ConceptService conceptService;
	private EsQueryPageReq querypage = new EsQueryPageReq(10);

	/**
	 * 个股当前状态
	 */
	@RequestMapping(value = "/code/{code}", method = RequestMethod.GET)
	public String detail(@PathVariable(value = "code") String code, Model model) {
		try {
			CodeBaseModel cbm = codeModelService.getLastOneByCode(code);
			model.addAttribute("codedetail", cbm);
			model.addAttribute("code", code);
			model.addAttribute("codeName", stockBasicService.getCodeName(code));
			model.addAttribute("histList", codeModelService.getListByCode(code, querypage));
			model.addAttribute("concepts", conceptService.getCodeConcept(code));

			String kb = "";
			if (cbm.getForestallQuarter() > 0) {
				kb = cbm.getForestallYear() + "年" + cbm.getForestallQuarter() + "季度";
				if (cbm.getForestallIncomeTbzz() > 0) {
					kb += ",营收同比:" + cbm.getForestallIncomeTbzz();
				}
				if (cbm.getForestallProfitTbzz() > 0) {
					kb += ",净利同比:" + cbm.getForestallProfitTbzz();
				}
			}
			model.addAttribute("kb", kb);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return "code";
	}

	/**
	 * 个股历史状态
	 */
	@RequestMapping(value = "/codehist/{id}", method = RequestMethod.GET)
	public String id(@PathVariable(value = "id") String id, Model model) {
		try {
			CodeBaseModelHist cbm = codeModelService.getHistOneById(id);
			String code = cbm.getCode();
			model.addAttribute("code", code);
			model.addAttribute("codeName", stockBasicService.getCodeName(code));
			model.addAttribute("codedetail", cbm);
			String kb = "";
			if (cbm.getForestallQuarter() > 0) {
				kb = cbm.getForestallYear() + "年" + cbm.getForestallQuarter() + "季度";
				if (cbm.getForestallIncomeTbzz() > 0) {
					kb += ",营收同比:" + cbm.getForestallIncomeTbzz();
				}
				if (cbm.getForestallProfitTbzz() > 0) {
					kb += ",净利同比:" + cbm.getForestallProfitTbzz();
				}
			}
			model.addAttribute("kb", kb);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return "code";
	}

	/**
	 * 测试页面列表
	 */
	@RequestMapping(value = "/realtime/view", method = RequestMethod.GET)
	public String view(String all, String type, String buyDate, Model model) {
		try {
			ReportVo pv = monitoringService.getVeiw(all, type, buyDate);
			List<ViewVo> l = pv.getList();
			model.addAttribute("vvs", l);
			model.addAttribute("stat", pv);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return "realtimelist";
	}

	@RequestMapping(value = "/codemodel/list", method = RequestMethod.GET)
	@ResponseBody
	public ResponseEntity<JsonResult> codemodellist(String code, int orderBy, int asc, EsQueryPageReq page) {
		JsonResult r = new JsonResult();
		try {

			r.setResult(codeModelService.getListForWeb(code, orderBy, asc, page));
			r.setStatus(JsonResult.OK);
		} catch (Exception e) {
			r.setResult(e.getClass().getName() + ":" + e.getMessage());
			r.setStatus(JsonResult.ERROR);
			e.printStackTrace();
		}
		return ResponseEntity.ok(r);
	}

	@RequestMapping(value = "/showsorce/{code}", method = RequestMethod.GET)
	@ResponseBody
	public ResponseEntity<JsonResult> showsorce(String code) {
		JsonResult r = new JsonResult();
		try {
			r.setResult(codeModelService.run(code));
			r.setStatus(JsonResult.OK);
		} catch (Exception e) {
			r.setResult(e.getClass().getName() + ":" + e.getMessage());
			r.setStatus(JsonResult.ERROR);
			e.printStackTrace();
		}
		return ResponseEntity.ok(r);
	}
}
