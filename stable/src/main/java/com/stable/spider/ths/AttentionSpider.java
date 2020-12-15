package com.stable.spider.ths;

import java.time.Duration;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.stable.constant.RedisConstant;
import com.stable.es.dao.base.CodeAttentionHishDao;
import com.stable.service.StockBasicService;
import com.stable.service.TradeCalService;
import com.stable.utils.DateUtil;
import com.stable.utils.HttpUtil;
import com.stable.utils.RedisUtil;
import com.stable.utils.ThreadsUtil;
import com.stable.utils.WxPushUtil;
import com.stable.vo.bus.CodeAttentionHish;
import com.stable.vo.bus.StockBaseInfo;

import lombok.extern.log4j.Log4j2;

/**
 * 关注度
 *
 */
@Component
@Log4j2
public class AttentionSpider {

	private static final String DONE = "Done";
	@Autowired
	private StockBasicService stockBasicService;
	@Autowired
	private RedisUtil redisUtil;
	@Autowired
	private TradeCalService tradeCalService;
	@Autowired
	private CodeAttentionHishDao codeAttentionHishDao;
	// @Autowired
	// private CodeAttentionHishDao codeAttentionHishDao;

	private String baseUrl = "https://basic.10jqka.com.cn/api/stockph/focusday.php?code=%s&t=%s";

	private String getDate() {
		String date = redisUtil.get(RedisConstant.RDS_ATTENTION_DATE);
		if (StringUtils.isBlank(date)) {
			date = DateUtil.getTodayYYYYMMDD();
		}
		return date;
	}

	private void setDate(String date, boolean isDone) {
		redisUtil.set(RedisConstant.RDS_ATTENTION_DATE, date);
		if (isDone) {
			redisUtil.set(RedisConstant.RDS_ATTENTION_DATE_DONE_ + date, DONE, Duration.ofDays(15));
			redisUtil.set(RedisConstant.RDS_ATTENTION_DATE_CODE, DONE);
		} else {
			redisUtil.set(RedisConstant.RDS_ATTENTION_DATE_DONE_ + date, "No", Duration.ofDays(15));
		}
	}

	private String getDateCode() {
		return redisUtil.get(RedisConstant.RDS_ATTENTION_DATE_CODE);
	}

	private String getDateDone(int date) {
		return redisUtil.get(RedisConstant.RDS_ATTENTION_DATE_DONE_ + date);
	}

	private void setDateCode(String code) {
		redisUtil.set(RedisConstant.RDS_ATTENTION_DATE_CODE, code);
	}

	public void start() {
		try {
			int today = DateUtil.getTodayIntYYYYMMDD();
			int date = Integer.valueOf(getDate());// 上次日期;

			boolean starting = true;
			String lastCode = getDateCode();// 上次代码

			int doneDate = 0;

			if (tradeCalService.isOpen(today)) {
				doneDate = today;
				log.info("日期:{} 是工作日,最后抓包日期{}", today, date);
				if (today == date) {
					boolean isDateDone = DONE.equals(getDateDone(date));
					if (isDateDone) {
						log.info("当天已完成");
						return;// 当天已完成
					} else {
						long now = DateUtil.getTodayYYYYMMDDHHMMSS_NOspit(new Date());
						long d220000 = DateUtil.getTodayYYYYMMDDHHMMSS_NOspit(
								DateUtil.parseDate(today + "230000", DateUtil.YYYY_MM_DD_HH_MM_SS_NO_SPIT));
						if (now < d220000) {
							log.info("当天未完成-但时间未到,return");
							return;
						}
						log.info("当天未完成");
						starting = false;// 当天未完成
					}
				} else {
					int predate = tradeCalService.getPretradeDate(today);
					if (predate == date) {// 最后日期
						boolean isDateDone = DONE.equals(getDateDone(date));
						if (isDateDone) {
							long now = DateUtil.getTodayYYYYMMDDHHMMSS_NOspit(new Date());
							long d220000 = DateUtil.getTodayYYYYMMDDHHMMSS_NOspit(
									DateUtil.parseDate(today + "220000", DateUtil.YYYY_MM_DD_HH_MM_SS_NO_SPIT));
							if (now < d220000) {// 上个交易日已完成
								log.info("最后一个交易日已完成,当天未完成-但时间未到,return");
								return;
							}
							log.info("每周一收盘开始");
						} else {
							log.info("最后一个交易日未完成");
							starting = false;// 上个交易日未完成
							doneDate = predate;
						}
					}
					setDate(today + "", false);// 新的开始
					log.info("非当天,重新开始");
				}
			} else {
				log.info("日期:{} 是周末,最后抓包日期{}", today, date);
				int predate = tradeCalService.getPretradeDate(today);
				if (predate == date) {// 是最后日期
					boolean isDateDone = DONE.equals(getDateDone(date));
					if (isDateDone) {
						log.info("最后一个交易日已完成");
						return;// 上个交易日已完成
					} else {
						log.info("最后一个交易日未完成");
						starting = false;// 上个交易日未完成
					}
				} else {
					setDate(predate + "", false);// 新的开始
					log.info("非最后一个交易日,重新开始");
				}
				doneDate = predate;
			}
			if (lastCode == null || DONE.equals(lastCode)) {
				starting = true;
			}
			List<CodeAttentionHish> list = new LinkedList<CodeAttentionHish>();
			List<StockBaseInfo> listcode = stockBasicService.getAllOnStatusList();// 顺序列表
			for (StockBaseInfo s : listcode) {
				String code = s.getCode();
				if (!stockBasicService.online1YearChk(code, today)) {// 最后日需要满足1年
					continue;
				}
				if (starting) {
					// 开始抓包。。。
					if (fetchWapper(code, list)) {
						setDateCode(code);
						if (list.size() > 1000) {
							codeAttentionHishDao.saveAll(list);
							list.clear();
						}
					}
				} else {
					if (code.equals(lastCode)) {
						starting = true;
					}
				}
			}
			setDate(doneDate + "", true);// 已完成
			if (list.size() > 0) {
				codeAttentionHishDao.saveAll(list);
				list.clear();
			}
			WxPushUtil.pushSystem1("关注度正常完成");
		} catch (Exception e) {
			e.printStackTrace();
			WxPushUtil.pushSystem1("关注度出错:" + e.getMessage());
		}
	}

	private boolean fetchWapper(String code, List<CodeAttentionHish> list) {
		int trytime = 0;
		log.info("抓包:{}", code);
		do {
			trytime++;
			ThreadsUtil.sleepRandomSecBetween1And5(trytime);
			try {
				fetch(code, list);
				return true;
			} catch (Exception e) {
				e.printStackTrace();
			}
		} while (trytime <= 10);
		WxPushUtil.pushSystem1("同花顺关注度-抓包出错,code=" + code);
		return false;
	}

	public int getCurrent(String code) {
		try {
			String url = String.format(baseUrl, code, System.currentTimeMillis());
			JSONObject json = HttpUtil.doGet(url);
			if (json.getString("status_msg").equalsIgnoreCase("ok")) {
				JSONObject data = json.getJSONObject("data");
				return data.getIntValue("rank");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return 0;
	}

	private void fetch(String code, List<CodeAttentionHish> list) {
		String url = String.format(baseUrl, code, System.currentTimeMillis());
		JSONObject json = HttpUtil.doGet(url);
		if (json.getString("status_msg").equalsIgnoreCase("ok")) {
			JSONObject data = json.getJSONObject("data");
			JSONObject history = data.getJSONObject("history");
			JSONArray date = history.getJSONArray("date");
			JSONArray rank = history.getJSONArray("rank");
			for (int i = 0; i < date.size(); i++) {
				CodeAttentionHish ch = new CodeAttentionHish();
				ch.setCode(code);
				ch.setDate(date.getIntValue(i));
				ch.setRank(rank.getIntValue(i));
				ch.setId();
//				System.err.println(ch);
				list.add(ch);
//				System.err.println(date.getString(i) + " " + rank.getString(i));
			}
		}
	}

	public static void main(String[] args) {
//		String baseUrl = "https://basic.10jqka.com.cn/api/stockph/focusday.php?code=%s&t=%s";
//		Map<String, String> header = new HashMap<String, String>();
//		header.put("Host", "basic.10jqka.com.cn");
//		header.put("User-Agent",
//				"User-Agent: Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/87.0.4280.66 Safari/537.36");
//		String url = String.format(baseUrl, "600109", System.currentTimeMillis());
//		System.err.println(url);
//		System.err.println(HttpUtil.doGet(url));

		AttentionSpider as = new AttentionSpider();
		as.fetch("600109", new LinkedList<CodeAttentionHish>());
	}
}
