package com.stable.spider.tick;

import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.stable.utils.FileReaderLineWorker;
import com.stable.utils.FileReaderUitl;
import com.stable.utils.FileWriteUitl;
import com.stable.utils.HttpUtil;

import lombok.extern.log4j.Log4j2;

@Log4j2
public class ThsTick {

	// Nginx forbidden.

	private static String url_base = "https://d.10jqka.com.cn/v6/time/%s/last.js";
	private static String start = "quotebridge_v6_time_hs_000001_last({'hs_000001':";
	private static int start_len = start.length();

	public static void genTick(String code, String filepath) {
		List<Tick> ticks = new LinkedList<Tick>();
		String key = "hs_" + code;
		String url = String.format(url_base, key);
		System.err.println(url);
		String s = HttpUtil.doGet2(url);
		System.err.println(s);
		if (s != null && s.length() > start_len) {
			String jsonstr = s.substring(start_len, s.length() - 2);
			System.err.println(jsonstr);
//				System.err.println(json);
			try {
				JSONObject json = JSON.parseObject(jsonstr);
				String data = json.getString("data");
				String[] ls = data.split(";");
				for (String line : ls) {
					if (StringUtils.isNotBlank(line)) {
						ticks.add(getTick(line));
					}
				}
			} catch (Exception e) {
				if (s.contains("forbidden")) {
					log.info("Nginx forbidden");
					return;
				}
				e.printStackTrace();
			}
		} else {
			return;
		}

		if (StringUtils.isNotBlank(filepath)) {
			FileWriteUitl fu = new FileWriteUitl(filepath, true);
			for (Tick t : ticks) {
				fu.writeLine(t.getStandardLine());
			}
			fu.close();
		}

		for (Tick t : ticks) {
			System.err.println(t.getStandardLine());
		}

	}

	public static List<Tick> readFromFile(String filepath) {
		FileReaderUitl reader = new FileReaderUitl(filepath);
		List<Tick> list = new LinkedList<Tick>();
		reader.readLineAndClosed(new FileReaderLineWorker() {
			@Override
			public void doworker(String line) {
				Tick t = new Tick();
				t.setValByStdLine(line);
				list.add(t);
			}
		});
		return list;
	}

	// 1500,16.40,22856700,16.321,1393700
	public static Tick getTick(String line) {
		Tick tick = new Tick();
		String[] fs = line.split(",");
		tick.setId(fs[0]);
		tick.setTime(fs[0]);
		tick.setPrice(Double.valueOf(fs[1]));
		tick.setVol(Long.valueOf(fs[2]));
		tick.setAmt(Double.valueOf(fs[4]));
		tick.setFen(Integer.valueOf(tick.getTime()));
//		tick.setChange(Double.valueOf(fs[3]));
		return tick;
	}

	public static void main(String[] args) {
		String filepath = "E:/t1.tick";
		// 生成
		genTick("000001", filepath);
		// 读取
		List<Tick> list = readFromFile(filepath);
		for (Tick t : list) {
			System.err.println(t);
		}
		System.err.println("==done===");
	}
}
