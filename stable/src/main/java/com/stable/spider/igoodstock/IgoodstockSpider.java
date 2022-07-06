package com.stable.spider.igoodstock;

import java.util.LinkedList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.stable.es.dao.base.ForeignCapitalSumDao;
import com.stable.msg.MsgPushServer;
import com.stable.service.StockBasicService;
import com.stable.utils.CurrencyUitl;
import com.stable.utils.DateUtil;
import com.stable.utils.ErrorLogFileUitl;
import com.stable.utils.HttpUtil;
import com.stable.utils.ThreadsUtil;
import com.stable.vo.bus.ForeignCapitalSum;
import com.stable.vo.bus.StockBaseInfo;

import lombok.extern.log4j.Log4j2;

/**
 * 外资持股
 */
@Component
@Log4j2
public class IgoodstockSpider {
	@Autowired
	private StockBasicService stockBasicService;
	@Autowired
	private ForeignCapitalSumDao foreignCapitalSumDao;

	private String urlbaes = "http://www.igoodstock.com/goodstock/getHsgtInstiHoldTop5?stockCode=%s&days=1&t=%s";

	public void byWeb() {
		new Thread(new Runnable() {
			@Override
			public void run() {
				getwz();
			}
		}).start();
	}

	private void getwz() {
		try {
			List<StockBaseInfo> codelist = stockBasicService.getAllOnStatusListWithSort();
			List<ForeignCapitalSum> upd = new LinkedList<ForeignCapitalSum>();
			for (StockBaseInfo s : codelist) {
				try {
					ForeignCapitalSum fcs = getWz(s.getCode());
					if (fcs != null) {
						if (s.getFloatShare() > 0) {
							double fs = s.getFloatShare() * CurrencyUitl.YI_N.doubleValue();
//						System.err.println(CurrencyUitl.YI_N.doubleValue());
//						System.err.println(String.valueOf(fs));
							fcs.setHoldRatio(CurrencyUitl.roundHalfUpWhithPercent(fcs.getHoldVol() / fs));
//						System.err.println(fcs);
						}
						upd.add(fcs);
					}
				} catch (Exception e) {
					ErrorLogFileUitl.writeError(e, "", "", "");
				}
			}
			if (upd.size() > 0) {
				foreignCapitalSumDao.saveAll(upd);
			}
			log.info("igoodstock-外资:done,size:{}", upd.size());
		} catch (Exception e) {
			e.printStackTrace();
			MsgPushServer.pushSystem1("igoodstock-外资-抓包异常");
		}
	}

	public ForeignCapitalSum getWz(String code) {
		int trytime = 0;
		do {
			trytime++;
			try {
				ThreadsUtil.sleepRandomSecBetween1And5();
				log.info("igoodstock-外资:code:{}", code);
				String url = String.format(urlbaes, code, System.currentTimeMillis());
				String result = HttpUtil.doGet2(url);

				JSONObject r = JSON.parseObject(result);
				JSONArray objects = r.getJSONArray("list");
				if (objects.size() > 0) {
					JSONObject last = objects.getJSONObject(0);
					ForeignCapitalSum fcs = new ForeignCapitalSum();
					fcs.setCode(code);
					fcs.setHoldAmount(last.getDouble("holdAmount"));
					fcs.setHoldVol(last.getDouble("holdVol").longValue());
					fcs.setDate(DateUtil.convertDate2(last.getString("date")));
					return fcs;
				}
				return null;
			} catch (Exception e) {
				e.printStackTrace();
			}
			ThreadsUtil.sleepRandomSecBetween15And30(trytime);
		} while (trytime <= 10);
		MsgPushServer.pushSystem1("igoodstock-外资-抓包出错,code=" + code);
		return null;
	}

	public static void main(String[] args) {
		IgoodstockSpider is = new IgoodstockSpider();
		ForeignCapitalSum fcs = is.getWz("002405");
		if (fcs != null) {
			double fs = 19.16 * CurrencyUitl.YI_N.doubleValue();
			System.err.println(CurrencyUitl.YI_N.doubleValue());
			System.err.println(String.valueOf(fs));
			fcs.setHoldRatio(CurrencyUitl.roundHalfUpWhithPercent(fcs.getHoldVol() / fs));
			System.err.println(fcs);
		}
	}
}
