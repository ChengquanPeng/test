package com.stable.service;

import java.io.File;
import java.util.LinkedList;
import java.util.List;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSONArray;
import com.stable.constant.Constant;
import com.stable.spider.tick.TencentTick;
import com.stable.spider.tushare.TushareSpider;
import com.stable.utils.WxPushUtil;
import com.stable.vo.bus.DaliyBasicInfo2;
import com.stable.vo.bus.StockBaseInfo;

import lombok.extern.log4j.Log4j2;

@Service
@Log4j2
public class TickService {
	@Autowired
	private StockBasicService stockBasicService;
	@Autowired
	private TushareSpider tushareSpider;
	@Autowired
	private TradeCalService tradeCalService;
	@Value("${tick.folder}")
	private String tickFolder;

	public void genTickEveryDay(List<DaliyBasicInfo2> daliybasicList) {
		new java.lang.Thread(new Runnable() {
			public void run() {
				try {
					for (DaliyBasicInfo2 d : daliybasicList) {
						String code = d.getCode();
						if (stockBasicService.isHuShenCode(code)) {
							File cf = new File(tickFolder + code);
							if (!cf.exists()) {
								cf.mkdir();
							}
							TencentTick.genTick(code, tickFolder + code + File.separator + d.getDate());
						}
					}
				} catch (Exception e) {
					e.printStackTrace();
					WxPushUtil.pushSystem1("每日Tick 数据文件生成异常");
				}
			}
		}).start();
	}

	public void regen(int today) {
		JSONArray array = tushareSpider.getStockDaliyTrade(null, today + "", null, null);
		if (array == null || array.size() <= 0) {
			log.warn("未获取到日交易记录,tushare,code={}");
			if (tradeCalService.isOpen(today)) {
				log.warn("未获取到日交易记录,tushare,日期=" + today);
				WxPushUtil.pushSystem1("未获取到日交易记录,tushare,日期=" + today);
			}
			return;
		}
		log.info("获取到日交易记录条数={}", array.size());
		List<DaliyBasicInfo2> daliybasicList = new LinkedList<DaliyBasicInfo2>();
		for (int i = 0; i < array.size(); i++) {
			DaliyBasicInfo2 dalyb = new DaliyBasicInfo2(array.getJSONArray(i));
			daliybasicList.add(dalyb);
		}
		genTickEveryDay(daliybasicList);
	}

	@PostConstruct
	private void test() {
		regen(20220401);
	}

	public void cleanFiles() {
		List<StockBaseInfo> list = stockBasicService.getAllList();
		for (StockBaseInfo s : list) {
			if (Constant.CODE_ON_STATUS.equals(s.getList_status())) {
				// 删除小于
			} else {
				// 删除全部
			}
		}
	}
}
