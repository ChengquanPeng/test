package com.stable.spider.tick;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.stable.utils.FileReaderLineWorker;
import com.stable.utils.FileReaderUitl;
import com.stable.utils.FileWriteUitl;
import com.stable.utils.HttpUtil;

public class TencentTickHist {

	private static String url_base = "https://stock.gtimg.cn/data/index.php?appn=detail&action=data&c=%s&p=";
	private static String start = "v_detail_data_sh601857=";
	private static int start_len = start.length();

	public static void genTick(String code, String filepath, double yersterdayPrice, String tickDaliy) {
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
			FileWriteUitl f1 = new FileWriteUitl(filepath, true);
			FileWriteUitl f2 = new FileWriteUitl(filepath + tickDaliy, true);
			if (ticks.size() > 0) {
				Map<Integer, TickFz> map = TencentTick.getTickFzMap(ticks, yersterdayPrice);
				List<TickFz> tt = TencentTick.getTickFz(map);
				for (TickFz t : tt) {
					f1.writeLine(TencentTick.genTickfzToStr(t));
				}
				f2.writeLine(TencentTick.TD_vo_to_str(TencentTick.getTickTickDay(map)));
			}
			f1.close();
			f2.close();
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
		String filepath = "E:/ticks/t1.tick";
		// 生成
		genTick("000039", filepath, 13.54, ".td");
		// 读取
		List<TickFz> list = readFromFile(filepath);
		for (TickFz t : list) {
			System.err.println(t);
		}
		System.err.println("==done===");
	}

}
