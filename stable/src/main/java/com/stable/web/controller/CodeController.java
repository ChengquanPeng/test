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

import com.stable.constant.Constant;
import com.stable.constant.EsQueryPageUtil;
import com.stable.service.ChipsService;
import com.stable.service.ConceptService;
import com.stable.service.StockBasicService;
import com.stable.service.model.CodeModelService;
import com.stable.service.realtime.MonitoringService;
import com.stable.vo.bus.AddIssue;
import com.stable.vo.bus.CodeBaseModel;
import com.stable.vo.bus.CodeBaseModelHist;
import com.stable.vo.bus.Jiejin;
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
	@Autowired
	private ChipsService chipsService;

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
			model.addAttribute("histList", codeModelService.getListByCode(code, EsQueryPageUtil.queryPage5));
			model.addAttribute("concepts", conceptService.getCodeConcept(code));
			model.addAttribute("codeBasic", stockBasicService.getCode(code));
			model.addAttribute("topThree", chipsService.getLastHolderPercent(code));

			// 是否有增发
			AddIssue iss = chipsService.getLastAddIssue(code);
			StringBuffer addIssue = new StringBuffer();
			if (iss.getStartDate() > 0) {
				addIssue.append("开始日期:").append(iss.getStartDate());
				if (iss.getEndDate() > 0) {
					addIssue.append(" 开始日期:").append(iss.getEndDate());
				}
				addIssue.append(" 状态:").append(getStatusDesc(iss.getStatus()));
				addIssue.append(" 公告:").append(iss.getTitles());
			}
			model.addAttribute("addIssue", addIssue.toString());
			// 前后1年解禁记录
			List<Jiejin> jj = chipsService.getBf2yearJiejin(code);
			StringBuffer jmsg = new StringBuffer();
			if (jj != null) {
				for (Jiejin j : jj) {
					jmsg.append("解禁日期:").append(j.getDate());
					jmsg.append(" 解禁类型:").append(j.getType());
					jmsg.append(" 解禁占比:").append(j.getZzb());
					jmsg.append(" 解禁成本:").append(j.getCost());
					jmsg.append(Constant.HTML_LINE);
				}
			}
			model.addAttribute("jmsg", jmsg.toString());
			// 快预报
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

	private String getStatusDesc(int s) {
		if (s == 1) {
			return "已开始";
		}
		if (s == 2) {
			return "已完成";
		}
		if (s == 3) {
			return "已终止";
		}
		return s + "";
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
	public ResponseEntity<JsonResult> codemodellist(String code, int orderBy, int asc, String conceptId,
			String conceptName, EsQueryPageReq page) {
		JsonResult r = new JsonResult();
		try {
			r.setResult(codeModelService.getListForWeb(code, orderBy, conceptId, conceptName, asc, page));
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
	public ResponseEntity<JsonResult> showsorce(@PathVariable(value = "code") String code) {
		JsonResult r = new JsonResult();
		try {
			r.setResult(codeModelService.runByCode(code));
			r.setStatus(JsonResult.OK);
		} catch (Exception e) {
			r.setResult(e.getClass().getName() + ":" + e.getMessage());
			r.setStatus(JsonResult.ERROR);
			e.printStackTrace();
		}
		return ResponseEntity.ok(r);
	}
}
