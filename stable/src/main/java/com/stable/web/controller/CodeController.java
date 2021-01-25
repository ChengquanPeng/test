package com.stable.web.controller;

import java.util.Collections;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import com.stable.constant.EsQueryPageUtil;
import com.stable.service.ChipsService;
import com.stable.service.ConceptService;
import com.stable.service.FinanceService;
import com.stable.service.StockBasicService;
import com.stable.service.model.CodeModelService;
import com.stable.utils.CurrencyUitl;
import com.stable.vo.bus.CodeBaseModel2;
import com.stable.vo.bus.CodeBaseModelHist;
import com.stable.vo.bus.FinanceBaseInfo;
import com.stable.vo.bus.Jiejin;
import com.stable.vo.bus.ZengFa;

@Controller
public class CodeController {

	@Autowired
	private CodeModelService codeModelService;
	@Autowired
	private StockBasicService stockBasicService;
	@Autowired
	private ConceptService conceptService;
	@Autowired
	private ChipsService chipsService;
	@Autowired
	private FinanceService financeService;

	/**
	 * 个股当前状态
	 */
	@RequestMapping(value = "/code/{code}", method = RequestMethod.GET)
	public String detail(@PathVariable(value = "code") String code, Model model) {
		try {
			CodeBaseModel2 cbm = codeModelService.getLastOneByCodeResp(code);
			model.addAttribute("codedetail", cbm);
			model.addAttribute("code", code);
			model.addAttribute("histList", codeModelService.getListByCode(code, EsQueryPageUtil.queryPage5));
			model.addAttribute("concepts", conceptService.getCodeConcept(code));
			model.addAttribute("codeBasic", stockBasicService.getCode(code));
			model.addAttribute("topThree", chipsService.getLastHolderPercent(code));
			FinanceBaseInfo fbi = financeService.getLastFinaceReport(code);
			try {
				model.addAttribute("yyzsr", CurrencyUitl.covertToString(fbi.getYyzsr()));
			} catch (Exception e) {
				model.addAttribute("yyzsr", "--");
			}
			try {
				model.addAttribute("gsjlr", CurrencyUitl.covertToString(fbi.getGsjlr()));
			} catch (Exception e) {
				model.addAttribute("gsjlr", "--");
			}
			try {
				model.addAttribute("kfjlr", CurrencyUitl.covertToString(fbi.getKfjlr()));
			} catch (Exception e) {
				model.addAttribute("kfjlr", "--");
			}
			try {
				model.addAttribute("goodWill", CurrencyUitl.covertToString(fbi.getGoodWill()));
			} catch (Exception e) {
				model.addAttribute("goodWill", "--");
			}
			model.addAttribute("finance", fbi);
			
			// 是否有增发
			ZengFa iss = chipsService.getLastZengFa(code);
			StringBuffer lastZf = new StringBuffer();
			if (iss.getStartDate() > 0) {
				lastZf.append("开始日期:").append(iss.getStartDate());
				if (iss.getEndDate() > 0) {
					lastZf.append(" 开始日期:").append(iss.getEndDate());
				}
				lastZf.append(" 状态:").append(iss.getStatusDesc());
				lastZf.append(" 类别:").append(iss.getIssueClz() + iss.getIssueType());
				lastZf.append(" 金额:").append(iss.getPrice() + "/" + iss.getAmt());
			}
			model.addAttribute("lastZf", lastZf.toString());
			// 前后1年解禁记录
			List<Jiejin> jj = chipsService.getBf2yearJiejin(code);
			if (jj == null) {
				jj = Collections.emptyList();
			}
			model.addAttribute("jmsg", jj);

			model.addAttribute("zfgk", chipsService.getZengFaSummary(code).getDesc());
			model.addAttribute("fhgk", chipsService.getFenHong(code).getDetails());
			// 快预报
			String kb = "未实现";
//			if (cbm.getForestallQuarter() > 0) {
//				kb = cbm.getForestallYear() + "年" + cbm.getForestallQuarter() + "季度";
//				if (cbm.getForestallIncomeTbzz() > 0) {
//					kb += ",营收同比:" + cbm.getForestallIncomeTbzz();
//				}
//				if (cbm.getForestallProfitTbzz() > 0) {
//					kb += ",净利同比:" + cbm.getForestallProfitTbzz();
//				}
//			}
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
//			if (cbm.getForestallQuarter() > 0) {
//				kb = cbm.getForestallYear() + "年" + cbm.getForestallQuarter() + "季度";
//				if (cbm.getForestallIncomeTbzz() > 0) {
//					kb += ",营收同比:" + cbm.getForestallIncomeTbzz();
//				}
//				if (cbm.getForestallProfitTbzz() > 0) {
//					kb += ",净利同比:" + cbm.getForestallProfitTbzz();
//				}
//			}
			model.addAttribute("kb", kb);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return "code";
	}

}
