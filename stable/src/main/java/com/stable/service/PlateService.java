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

	public List<PlateResp> plateAnalyse(String aliasCode, String codes, int sort) {
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
				List<FinanceBaseInfo> l2 = financeService.getLastFinaceReport4Quarter(code);
				FinanceBaseInfo fbi = l2.get(0);
				DaliyBasicInfo d = daliyBasicHistroyService.queryLastest(code);
				if (d != null && d.getSzl() > 0) {// 排除负数
					log.info("{},szl:{}", d.getCode(), d.getSzl());
					r.setT1(d.getSzl());
					t1 += d.getSzl();
					c1++;
				}
				if (fbi != null) {
//					log.info(fbi);
					r.setT2(getSylTtm(l2));
					r.setT2s(getSyldjd(fbi));
					t2 += r.getT2();
					c2++;
					if (fbi.getMll() > 0) {// 排除负数
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
			sort11(rl);// 先正序排序
			sort12(rl);// 在把负数放最后
			for (int i = 0; i < rl.size(); i++) {
				PlateResp r = rl.get(i);
				r.setRanking1(i + 1);
			}
			// 默认是1
			if (sort == 2) {
				sort2(rl);
			} else if (sort == 3) {
				sort3(rl);
			}
		}
		return rl;
	}

	/**
	 * 资产收益率TTM-季度平均
	 */
	public double getSylTtm(List<FinanceBaseInfo> fbis) {
		int end = 4;
		if (fbis.size() < 4) {
			end = fbis.size();
		}
		double t = 0.0;
		int tc = 0;
		for (int i = 0; i < end; i++) {
			FinanceBaseInfo f = fbis.get(i);
			if (f.getJqjzcsyl() != 0.0) {
				t += f.getJqjzcsyl();
				tc += f.getQuarter();
			}
		}
		if (tc > 0) {
			return CurrencyUitl.roundHalfUp(t / (double) tc);
		} else {
			return -9999.0;
		}
	}

	public double getSyldjd(FinanceBaseInfo fbi) {
		return CurrencyUitl.roundHalfUp(fbi.getJqjzcsyl() / (double) fbi.getQuarter());
	}

	public static void main(String[] args) {
		List<PlateResp> rl = new LinkedList<PlateResp>();
		for (int i = 10; i >= -2; i--) {
			PlateResp r = new PlateResp();
			r.setT1((double) i);
			rl.add(r);
		}
		sort11(rl);
		sort12(rl);
		for (PlateResp r : rl) {
			System.err.println(r.getT1());
		}
	}

	// 先正序排序
	public static void sort11(List<PlateResp> rl) {
		Collections.sort(rl, new Comparator<PlateResp>() {
			@Override
			public int compare(PlateResp o1, PlateResp o2) {
				if (o1.getT1() == o2.getT1()) {
					return 0;
				}
				return o1.getT1() - o2.getT1() > 0 ? -1 : 1;
			}
		});
	}

	// 在把负数放最后
	public static void sort12(List<PlateResp> rl) {
		Collections.sort(rl, new Comparator<PlateResp>() {
			@Override
			public int compare(PlateResp o1, PlateResp o2) {
				if (o1.getT1() <= 0) {// 最后
					return 1;
				}
				return -1;
			}
		});
	}

	public static void sort2(List<PlateResp> rl) {
		Collections.sort(rl, new Comparator<PlateResp>() {
			@Override
			public int compare(PlateResp o1, PlateResp o2) {
				if (o1.getT2() == o2.getT2()) {
					return 0;
				}
				return o2.getT2() - o1.getT2() > 0 ? 1 : -1;
			}
		});
	}

	public static void sort3(List<PlateResp> rl) {
		Collections.sort(rl, new Comparator<PlateResp>() {
			@Override
			public int compare(PlateResp o1, PlateResp o2) {
				if (o1.getT3() == o2.getT3()) {
					return 0;
				}
				return o2.getT3() - o1.getT3() > 0 ? 1 : -1;
			}
		});
	}
}
