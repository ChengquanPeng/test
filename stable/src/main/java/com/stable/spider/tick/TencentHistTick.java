package com.stable.spider.tick;

import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.stable.utils.FileReaderLineWorker;
import com.stable.utils.FileReaderUitl;
import com.stable.utils.FileWriteUitl;
import com.stable.utils.HttpUtil;

public class TencentHistTick {

	private static String url_base = "https://stock.gtimg.cn/data/index.php?appn=detail&action=data&c=%s&p=";
	private static String start = "v_detail_data_sh601857=";
	private static int start_len = start.length();

	public static void genTick(String code, String filepath, double yersterdayPrice) {
		List<TickFb> ticks = new LinkedList<TickFb>();
		String url = String.format(url_base, TencentTick.getCode(code));
		int p = 0;
		while (true) {
			String s = HttpUtil.doGet2(url + p);
			if (s != null && s.length() > start_len) {
				String json = s.substring((start + "[" + p + ",'").length(), s.length() - 2);
//				System.err.println(json);
				String[] ls = json.split("\\|");
				for (String line : ls) {
					if (StringUtils.isNotBlank(line)) {
						ticks.add(TencentTick.getTick(line));
					}
				}
			} else {
				break;
			}
			p++;
		}

		// 写入文件
		if (StringUtils.isNotBlank(filepath)) {
			FileWriteUitl fu = new FileWriteUitl(filepath, true);
			if (ticks.size() > 0) {
				List<TickFz> tt = TencentTick.getTickFz(TencentTick.getTickFzMap(ticks, yersterdayPrice));
				for (TickFz t : tt) {
					fu.writeLine(TencentTick.genTickfzToStr(t));
				}
			}
			fu.close();
		}
//		for (Tick t : ticks) {
//			System.err.println(t.getStandardLine());
//		}
	}

	public static List<TickFz> readFromFile(String filepath) {
		FileReaderUitl reader = new FileReaderUitl(filepath);
		List<TickFz> list = new LinkedList<TickFz>();
		reader.readLineAndClosed(new FileReaderLineWorker() {
			@Override
			public void doworker(String line) {
				if (StringUtils.isNotBlank(line)) {
					list.add(TencentTick.strToTickfz(line));
				}
			}
		});
		return list;
	}

	public static void main(String[] args) {
		String filepath = "E:/t1.tick";
		// 生成
		genTick("000039", filepath, 13.54);
		// 读取
		List<TickFz> list = readFromFile(filepath);
		for (TickFz t : list) {
			System.err.println(t);
		}
		System.err.println("==done===");
	}

}
