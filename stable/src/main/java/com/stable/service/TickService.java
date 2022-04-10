package com.stable.service;

import java.io.File;
import java.io.FilenameFilter;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSONArray;
import com.stable.constant.Constant;
import com.stable.constant.EsQueryPageUtil;
import com.stable.spider.tick.TencentTickHist;
import com.stable.spider.tushare.TushareSpider;
import com.stable.utils.ThreadsUtil;
import com.stable.utils.WxPushUtil;
import com.stable.vo.bus.DaliyBasicInfo2;
import com.stable.vo.bus.StockBaseInfo;
import com.stable.vo.bus.TradeHistInfoDaliyNofq;

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
	@Autowired
	private DaliyTradeHistroyService daliyTradeHistroyService;
	public static String tickDaliy = ".td";

	public void genTickEveryDay(List<DaliyBasicInfo2> daliybasicList, int date) {
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
							TencentTickHist.genTick(code, tickFolder + code + File.separator + d.getDate(),
									d.getYesterdayPrice(), tickDaliy);
						}
					}
					ThreadsUtil.sleepRandomSecBetween15And30();
					// 删除文件
					if (date > 0) {
						cleanFiles(date);
					}
				} catch (Exception e) {
					e.printStackTrace();
					WxPushUtil.pushSystem1("每日Tick 数据文件生成异常或删除异常");
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
		genTickEveryDay(daliybasicList, 0);
	}

//	@PostConstruct
	public void test() {
		regen(20220401);
	}

	public void cleanFiles(int date) {
		List<StockBaseInfo> list = stockBasicService.getAllList();
		for (StockBaseInfo s : list) {
			String code = s.getCode();
			File cf = new File(tickFolder + code);
			File[] ff = cf.listFiles(new FilenameFilter() {
				public boolean accept(File dir, String name) {
					return !name.endsWith(tickDaliy);
				}
			});
			if (ff != null && ff.length > 0) {
				if (Constant.CODE_ON_STATUS.equals(s.getList_status())) {
					// 删除小于天以前的
					List<TradeHistInfoDaliyNofq> l2 = daliyTradeHistroyService.queryListByCodeWithLastNofq(code, 0,
							date, EsQueryPageUtil.queryPage10, SortOrder.DESC);
					int minDate = l2.stream().min(Comparator.comparingDouble(TradeHistInfoDaliyNofq::getDate)).get()
							.getDate();
					for (File f : ff) {
						if (Integer.valueOf(f.getName()) < minDate) {// 最小日期删除
							f.delete();
							File f2 = new File(f.getAbsoluteFile() + tickDaliy);// 删除td文件
							f2.delete();
							log.info("delete:" + f.getAbsolutePath());
						}
					}
				} else {
					// 删除全部
					for (File f : ff) {
						f.delete();
						File f2 = new File(f.getAbsoluteFile() + tickDaliy);
						f2.delete();
					}
					cf.delete();
					log.info(code + " delete all");
				}
			}
		}
		log.info("delete tick file done");
	}

	public static void main(String[] args) {
		String tickFolder = "E:/ticks/000002";
		File cf = new File(tickFolder);
		File[] ff = cf.listFiles(new FilenameFilter() {
			public boolean accept(File dir, String name) {
				return !name.endsWith(tickDaliy);
			}
		});
		int minDate = 20220507;
		for (File f : ff) {
			if (Integer.valueOf(f.getName()) < minDate) {// 最小日期删除
				f.delete();
				File f2 = new File(f.getAbsoluteFile() + tickDaliy);// 删除td文件
				f2.delete();
				log.info("delete:" + f.getAbsolutePath());
			}
		}
	}
}
