package com.stable.service;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.stable.utils.CurrencyUitl;
import com.stable.vo.bus.Concept;
import com.stable.vo.bus.DaliyBasicInfo;
import com.stable.vo.bus.FinanceBaseInfo;
import com.stable.vo.http.resp.PlateResp;

import lombok.extern.log4j.Log4j2;

/**
 * 板块
 */
@Service
@Log4j2
public class PlateService {
	@Autowired
	private ConceptService conceptService;
	@Autowired
	private StockBasicService stockBasicService;
	@Autowired
	private DaliyBasicHistroyService daliyBasicHistroyService;
	@Autowired
	private FinanceService financeService;

	public List<PlateResp> plateAnalyse(String aliasCode, String codes) {
		List<PlateResp> rl = new LinkedList<PlateResp>();
		List<String> list = null;
		Concept cp = null;
		if (StringUtils.isNotBlank(codes)) {
			list = Arrays.asList(codes.split(","));
		} else if (StringUtils.isNotBlank(aliasCode)) {
			cp = conceptService.getConceptId(aliasCode);
			if (cp != null) {
				log.info(cp.getName());
			} else {
				log.warn("未获取到aliasCode={}", aliasCode);
			}
			list = conceptService.listCodesByAliasCode(aliasCode);
		}
		if (list != null) {
			double t1 = 0.0;
			int c1 = 1;
			double t2 = 0.0;
			int c2 = 1;
			double t3 = 0.0;
			int c3 = 1;

			for (String code : list) {
//				<option value="1">市赚率(股价)</option>
//				<option value="2">资产收益率(季报)</option>
//				<option value="3">毛利率(季报)</option>
				PlateResp r = new PlateResp();
				FinanceBaseInfo fbi = financeService.getLastFinaceReport(code);
				DaliyBasicInfo d = daliyBasicHistroyService.queryLastest(code);
				if (d != null && d.getSzl() != 0) {
					t1 += d.getSzl();
					c1++;
					r.setT1(d.getSzl());
				}
				if (fbi != null) {
					if (fbi.getSyldjd() != 0) {
						t2 += fbi.getSyldjd();
						c2++;
						r.setT2(fbi.getSyldjd());
					}
					if (fbi.getMll() != 0) {
						r.setT3(CurrencyUitl.roundHalfUp(fbi.getMll() / (double) fbi.getQuarter()));
						t3 += r.getT3();
						c3++;
					}
				}
				r.setCode(code);
				r.setCodeName(stockBasicService.getCodeName(code));
				rl.add(r);
			}
			if (c1 > 1) {
				c1--;
			}
			if (c2 > 1) {
				c2--;
			}
			if (c3 > 1) {
				c3--;
			}
			double avgt1 = CurrencyUitl.roundHalfUp(t1 / (double) c1);
			double avgt2 = CurrencyUitl.roundHalfUp(t2 / (double) c2);
			double avgt3 = CurrencyUitl.roundHalfUp(t3 / (double) c3);
			for (PlateResp r : rl) {
				r.setAvgt1(avgt1);
				r.setAvgt2(avgt2);
				r.setAvgt3(avgt3);
			}

			// 排序
			sort3(rl);
			for (int i = 0; i < rl.size(); i++) {
				PlateResp r = rl.get(i);
				r.setRanking3(i + 1);
			}
			sort2(rl);
			for (int i = 0; i < rl.size(); i++) {
				PlateResp r = rl.get(i);
				r.setRanking2(i + 1);
			}
			sort1(rl);
			for (int i = 0; i < rl.size(); i++) {
				PlateResp r = rl.get(i);
				r.setRanking1(i + 1);
			}
		}
		return rl;
	}

	public void sort1(List<PlateResp> rl) {
		Collections.sort(rl, new Comparator<PlateResp>() {
			@Override
			public int compare(PlateResp o1, PlateResp o2) {
				return o2.getT1() - o1.getT1() > 0 ? 1 : -1;
			}
		});
	}

	public void sort2(List<PlateResp> rl) {
		Collections.sort(rl, new Comparator<PlateResp>() {
			@Override
			public int compare(PlateResp o1, PlateResp o2) {
				return o2.getT2() - o1.getT2() > 0 ? 1 : -1;
			}
		});
	}

	public void sort3(List<PlateResp> rl) {
		Collections.sort(rl, new Comparator<PlateResp>() {
			@Override
			public int compare(PlateResp o1, PlateResp o2) {
				return o2.getT3() - o1.getT3() > 0 ? 1 : -1;
			}
		});
	}
}
