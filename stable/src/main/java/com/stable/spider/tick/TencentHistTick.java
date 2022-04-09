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

	public static void genTick(String code, String filepath) {
		List<Tick> ticks = new LinkedList<Tick>();
		String url = String.format(url_base, getCode(code));
		int p = 0;
		while (true) {
			String s = HttpUtil.doGet2(url + p);
			if (s != null && s.length() > start_len) {
				String json = s.substring((start + "[" + p + ",'").length(), s.length() - 2);
//				System.err.println(json);
				String[] ls = json.split("\\|");
				for (String line : ls) {
					if (StringUtils.isNotBlank(line)) {
						ticks.add(getTick(line));
					}
				}
			} else {
				break;
			}
			p++;
		}
		if (StringUtils.isNotBlank(filepath)) {
			FileWriteUitl fu = new FileWriteUitl(filepath, true);
			for (Tick t : ticks) {
				fu.writeLine(t.getStandardLine());
			}
			fu.close();
		}
//		for (Tick t : ticks) {
//			System.err.println(t.getStandardLine());
//		}
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

	private static Tick getTick(String line) {
		Tick tick = new Tick();
		String[] fs = line.split("/");
		tick.setId(fs[0]);
		tick.setTencentTime(fs[1]);
		tick.setPrice(Double.valueOf(fs[2]));
		tick.setChange(Double.valueOf(fs[3]));
		tick.setVol(Long.valueOf(fs[4]));
		tick.setAmt(Double.valueOf(fs[5]));
		if ("S".equals(fs[6])) {
			tick.setBs(1);
		}
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

	public static String getCode(String code) {
		if (code.startsWith("6")) {
			return "sh" + code;
		}
		return "sz" + code;
	}
}
