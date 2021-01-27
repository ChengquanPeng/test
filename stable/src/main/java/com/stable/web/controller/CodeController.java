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
import com.stable.service.ChipsZfService;
import com.stable.service.ConceptService;
import com.stable.service.FinanceService;
import com.stable.service.StockBasicService;
import com.stable.service.model.CodeModelService;
import com.stable.utils.CurrencyUitl;
import com.stable.vo.bus.CodeBaseModel2;
import com.stable.vo.bus.FinanceBaseInfo;
import com.stable.vo.bus.Jiejin;
import com.stable.vo.bus.ZengFa;
import com.stable.vo.http.resp.CodeBaseModelResp;

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
	private ChipsZfService chipsZfService;
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
			prepare(model, code);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return "code";
	}

	private void prepare(Model model, String code) {
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
		try {
			model.addAttribute("sumAsset", CurrencyUitl.covertToString(fbi.getSumAsset()));
		} catch (Exception e) {
			model.addAttribute("sumAsset", "--");
		}
		try {
			model.addAttribute("inventory", CurrencyUitl.covertToString(fbi.getInventory()));
		} catch (Exception e) {
			model.addAttribute("inventory", "--");
		}
		try {
			model.addAttribute("sumLasset", CurrencyUitl.covertToString(fbi.getSumLasset()));
		} catch (Exception e) {
			model.addAttribute("sumLasset", "--");
		}
		try {
			model.addAttribute("monetaryFund", CurrencyUitl.covertToString(fbi.getMonetaryFund()));
		} catch (Exception e) {
			model.addAttribute("monetaryFund", "--");
		}
		try {
			model.addAttribute("tradeFinassetNotfvtpl", CurrencyUitl.covertToString(fbi.getTradeFinassetNotfvtpl()));
		} catch (Exception e) {
			model.addAttribute("tradeFinassetNotfvtpl", "--");
		}
		try {
			model.addAttribute("sumDebtLd", CurrencyUitl.covertToString(fbi.getSumDebtLd()));
		} catch (Exception e) {
			model.addAttribute("sumDebtLd", "--");
		}
		try {
			model.addAttribute("sumDebt", CurrencyUitl.covertToString(fbi.getSumDebt()));
		} catch (Exception e) {
			model.addAttribute("sumDebt", "--");
		}
		try {
			model.addAttribute("netAsset", CurrencyUitl.covertToString(fbi.getNetAsset()));
		} catch (Exception e) {
			model.addAttribute("netAsset", "--");
		}
		try {
			model.addAttribute("accountPay", CurrencyUitl.covertToString(fbi.getAccountPay()));
		} catch (Exception e) {
			model.addAttribute("accountPay", "--");
		}
		try {
			model.addAttribute("retaineDearning", CurrencyUitl.covertToString(fbi.getRetaineDearning()));
		} catch (Exception e) {
			model.addAttribute("retaineDearning", "--");
		}
		try {
			model.addAttribute("interestPay", CurrencyUitl.covertToString(fbi.getInterestPay()));
		} catch (Exception e) {
			model.addAttribute("interestPay", "--");
		}
		try {
			model.addAttribute("accountrec", CurrencyUitl.covertToString(fbi.getAccountrec()));
		} catch (Exception e) {
			model.addAttribute("accountrec", "--");
		}
		model.addAttribute("finance", fbi);

		// 是否有增发
		ZengFa iss = chipsZfService.getLastZengFa(code);
		StringBuffer lastZf = new StringBuffer();
		if (iss.getStartDate() > 0) {
			lastZf.append("开始日期:").append(iss.getStartDate());
			if (iss.getEndDate() > 0) {
				lastZf.append(" 结束日期:").append(iss.getEndDate());
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

		model.addAttribute("zfgk", chipsZfService.getZengFaSummary(code).getDesc());
		model.addAttribute("fhgk", chipsService.getFenHong(code).getDetails());
		// 快预报
		String kb = financeService.getyjkb(code, fbi.getYear(), fbi.getQuarter());
		model.addAttribute("kb", kb);
	}

	/**
	 * 历史状态
	 */
	@RequestMapping(value = "/codehist/pre/{code}/{year}/{quarter}", method = RequestMethod.GET)
	public String pre(@PathVariable(value = "code") String code, int year, int quarter, Model model) {
		try {
			CodeBaseModelResp cbm = codeModelService.getHistOneByCodeYearQuarter(code, year, quarter);
			model.addAttribute("codedetail", cbm);
			prepare(model, code);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return "code";
	}

	/**
	 * 历史状态
	 */
	@RequestMapping(value = "/codehist/{id}", method = RequestMethod.GET)
	public String id(@PathVariable(value = "id") String id, Model model) {
		try {
			CodeBaseModelResp cbm = codeModelService.getHistOneById(id);
			String code = cbm.getCode();
			model.addAttribute("codedetail", cbm);
			prepare(model, code);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return "code";
	}

}
