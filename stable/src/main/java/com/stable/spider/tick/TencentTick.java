package com.stable.spider.tick;

import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.stable.utils.HttpUtil;

public class TencentTick {

	public static void main(String[] args) {
		List<Tick> ticks = new LinkedList<Tick>();
		String start = "v_detail_data_sh601857=";
		int start_len = start.length();
		String url = "https://stock.gtimg.cn/data/index.php?appn=detail&action=data&c=sh601857&p=";
		int p = 0;
		while (true) {
			String s = HttpUtil.doGet2(url + p);
			if (s != null && s.length() > start_len) {
				String json = s.substring((start + "[" + p + ",'").length(), s.length() - 2);
//				System.err.println(json);
				String[] ls = json.split("\\|");
				for (String line : ls) {
					if (StringUtils.isNotBlank(line)) {
						ticks.add(new Tick(line));
					}
				}
			} else {
				break;
			}
			p++;
		}

		System.err.println("==done===");
		for (Tick t : ticks) {
			System.err.println(t);
		}
	}
}
