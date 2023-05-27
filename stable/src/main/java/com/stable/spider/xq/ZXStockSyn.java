package com.stable.spider.xq;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.stable.constant.Constant;
import com.stable.service.StockBasicService;
import com.stable.service.model.prd.msg.MsgPushServer;
import com.stable.utils.ErrorLogFileUitl;
import com.stable.utils.HttpUtil;
import com.stable.utils.ThreadsUtil;
import com.stable.vo.bus.StockBaseInfo;

import lombok.extern.log4j.Log4j2;

@Log4j2
@Service
public class ZXStockSyn implements InitializingBean {
	// https://weixin.citicsinfo.com/tztweb/hq/index.html
	public static String url = "https://weixin.citicsinfo.com/reqxml?action=1230";

	@Autowired
	private StockBasicService stockBasicService;
	@Value("${excodes.list}")
	public String excodeslist;
	private Set<String> exlist = new HashSet<String>();

	@Override
	public void afterPropertiesSet() throws Exception {
		String[] cods = excodeslist.split(Constant.DOU_HAO);
		for (String c : cods) {
			exlist.add(c);
		}
	}

	@PostConstruct
	public void stockListChk() {
		try {
			List<StockBaseInfo> list = new LinkedList<StockBaseInfo>();
			boolean fetchAll = false;
			int curPage = 1;
			int pageSize = 5000;
			int MAXCOUNT = 0;
			int end = 0;
			do {
				Map<String, String> p = new HashMap<String, String>();
				p.put("c.funcno", "21000");
				p.put("c.version", "1");
				p.put("c.sort", "1");
				p.put("c.order", "1");
				p.put("c.type", "0:2:9:18");
				p.put("c.field", "1:2:22:23:24:3:8:16:21:31");
				p.put("c.rowOfPage", pageSize + "");
				p.put("c.curPage", curPage + "");
				String rlt1 = HttpUtil.doPost3(url, p);
				log.info("page =" + curPage);

				JSONObject arlt = JSON.parseObject(rlt1);
				MAXCOUNT = Integer.valueOf(arlt.getString("MAXCOUNT"));
				String rlst2 = arlt.getString("BINDATA");

				JSONArray data = JSON.parseObject(rlst2).getJSONArray("results");
				String code = null;
				String name = null;
				StockBaseInfo t = null;
				for (int i = 0; i < data.size(); i++) {
					JSONArray row = data.getJSONArray(i);
					name = row.getString(2);
					code = row.getString(4);
					// 是否退市股票
					if (stockBasicService.isTuiShi(name)) {
						if (!stockBasicService.isTuiShi(t.getName())) {// 同步数据显示已经退市，但本系统不是退市，则更新
							stockBasicService.synName(code, name);
						}
					} else {
						t = stockBasicService.getCode(code);
						if (t.getCode().equals(StockBasicService.NO)) {// 非退市股票，但是本系统不存在
							// MsgPushServer.pushSystem1("发现新未同步的股票：" + code + name);
							// log.info("发现新未同步的股票：" + code + name);
							// System.err.println("发现新未同步的股票：" + code + name);

							if (!exlist.contains(code)) {
								StockBaseInfo base = new StockBaseInfo();
								base.setCode(code);
								base.setName(name);
//								base.setList_date(date);
//								base.setMarket(stockBasicService.getMaketcode2(row.getString(3)));
//								base.setList_status(Constant.CODE_ON_STATUS);
								list.add(base);
							}
						}
					}
				}

				end = MAXCOUNT / pageSize;
				if (MAXCOUNT % pageSize != 0) {
					end++;
				}
				log.info("end=" + end + "当前page=" + curPage + ",pageSize=" + pageSize + ",MAXCOUNT=" + MAXCOUNT);
				if (curPage >= end) {
					fetchAll = true;
					log.info("已完成");
					break;
				}
				curPage++;
				ThreadsUtil.sleepRandomSecBetween1And5();
			} while (!fetchAll);
//			stockBasicService.saveAll(list);
			StringBuffer sb = new StringBuffer();
			for (StockBaseInfo base : list) {
				sb.append(base.getCode()).append(Constant.DOU_HAO);
			}
			if (sb.length() > 0) {
				log.info("发现新未同步的股票: " + sb);
				MsgPushServer.pushSystem1("发现新未同步的股票: " + sb);
			} else {
				log.info("股票池check正常");
			}

		} catch (Exception e) {
			e.printStackTrace();
			ErrorLogFileUitl.writeError(e, "所有同步异常", "", "");
			MsgPushServer.pushSystem1("所有同步异常");
		}
	}

	public static void main(String[] args) {
		new ZXStockSyn().stockListChk();
	}
}
