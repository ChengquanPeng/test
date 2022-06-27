package com.stable.service;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.stable.constant.EsQueryPageUtil;
import com.stable.service.model.WebModelService;
import com.stable.utils.CurrencyUitl;
import com.stable.vo.bus.CodeBaseModel2;
import com.stable.vo.bus.Concept;
import com.stable.vo.bus.FinanceBaseInfo;
import com.stable.vo.http.req.ModelReq;
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
	private FinanceService financeService;
	@Autowired
	private WebModelService webModelService;

	public List<PlateResp> klinelist() {
		List<PlateResp> res = new LinkedList<PlateResp>();
		int limit = 200;
		List<Concept> list = conceptService.getConceptList(limit);
		for (Concept cp : list) {
			if (cp.getCnt() > 0 && !cp.getAliasCode2().equals(cp.getName())
					&& !"885869|885582|885905|885907|885906".contains(cp.getAliasCode2())) {
				ModelReq mr = new ModelReq();
				mr.setConceptId(cp.getAliasCode2());
				List<CodeBaseModel2> listr = webModelService.getList(mr, EsQueryPageUtil.queryPage9999);
				if (listr != null) {
					int ck = 0;
					for (CodeBaseModel2 cm : listr) {
						if (cm.getShootingw() == 1) {
							ck++;
						}
					}
					PlateResp pr = new PlateResp();
					pr.setCode(cp.getAliasCode2());
					pr.setCodeName(cp.getName());
					pr.setT4(ck / Double.valueOf(listr.size()));
					pr.setRanking1(ck);
					pr.setRanking2(listr.size());
					res.add(pr);
				}
			}
		}
		arSort(res);
		return res;
	}

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
			list = conceptService.listCodesByAliasCode(aliasCode, EsQueryPageUtil.queryPage9999);
		}
		if (list != null) {
			double t2 = 0.0;// 收益率ttm
			int c2 = 1;
			double t3 = 0.0;// 毛利率
			int c3 = 1;
			double t4 = 0.0;// 应收账款
			int c4 = 1;

			for (String code : list) {
//				<option value="1">市赚率(股价)</option>
//				<option value="2">资产收益率(季报)</option>
//				<option value="3">毛利率(季报)</option>
				PlateResp r = new PlateResp();
				List<FinanceBaseInfo> l2 = financeService.getLastFinaceReport4Quarter(code);
				FinanceBaseInfo fbi = l2.get(0);
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
					if (fbi.getAccountrecRatio() != 0) {
						t4 += fbi.getAccountrecRatio();
						c4++;
						r.setT4(fbi.getAccountrecRatio());
						r.setT4e(CurrencyUitl.covertToString(Double.valueOf(fbi.getAccountrec()).longValue()));
					}
				}
				r.setCode(code);
				r.setCodeName(stockBasicService.getCodeName(code));
				rl.add(r);
			}
			if (c2 > 1) {
				c2--;
			}
			if (c3 > 1) {
				c3--;
			}
			if (c4 > 1) {
				c4--;
			}
			double avgt2 = CurrencyUitl.roundHalfUp(t2 / (double) c2);
			double avgt3 = CurrencyUitl.roundHalfUp(t3 / (double) c3);
			double avgt4 = CurrencyUitl.roundHalfUp(t4 / (double) c4);
			for (PlateResp r : rl) {
				r.setAvgt1(0);
				r.setAvgt2(avgt2);
				r.setAvgt3(avgt3);
				r.setAvgt4(avgt4);
			}
			// 排序
			arSort(rl);
			for (int i = 0; i < rl.size(); i++) {
				PlateResp r = rl.get(i);
				r.setRanking4(i + 1);
			}

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
			} else if (sort == 4) {
				arSort(rl);
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
			return 0.0;
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

	private void arSort(List<PlateResp> rl) {
		Collections.sort(rl, new Comparator<PlateResp>() {
			@Override
			public int compare(PlateResp o1, PlateResp o2) {
				if (o1.getT4() == o2.getT4()) {
					return 0;
				}
				return o2.getT4() - o1.getT4() > 0 ? 1 : -1;
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
