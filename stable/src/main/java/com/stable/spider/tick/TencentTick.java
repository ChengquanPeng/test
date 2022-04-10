package com.stable.spider.tick;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.stable.utils.CurrencyUitl;

public class TencentTick {
	private static int T_925 = 925;
	private static int T_930 = 930;
	private static int T_1500 = 1500;
	private static int fens = 239;// 09:25-09:30->15:00, 4个小时，4*60=240分钟，+ 09:25=241,

	// 写文件的时候，最好排序
	public static List<TickFz> getTickFz(Map<Integer, TickFz> map) {
		return map.values().stream().sorted(Comparator.comparing(TickFz::getFen)).collect(Collectors.toList());
	}

	//
	public static TickDay getTickTickDay(Map<Integer, TickFz> map) {
		map.remove(T_925);
		map.remove(T_930);
		map.remove(T_1500);
		BigDecimal bi = new BigDecimal(0);
		TickDay td = new TickDay();
		for (TickFz fz : map.values()) {
			if (fz.sx == 1) {
				if (fz.getVol() > td.getUpVol()) {
					td.setUpVol(fz.getVol());
				}
			} else if (fz.sx == -1) {
				if (fz.getVol() > td.getDownVol()) {
					td.setDownVol(fz.getVol());
				}
			}
			if (fz.getVol() > td.getTop()) {
				td.setTop(fz.getVol());
			}
			bi = CurrencyUitl.addDecimal(bi, fz.getVol());
		}
		td.setAvg(CurrencyUitl.divideDecimal(bi, fens).longValue());
		return td;
	}

	public static Map<Integer, TickFz> getTickFzMap(List<TickFb> list, double yersterdayPrice) {
		Map<Integer, TickFz> m = new HashMap<Integer, TickFz>();
		for (TickFb fb : list) {
			TickFz l = m.get(fb.getFen());
			if (l == null) {
				l = new TickFz();
				m.put(fb.getFen(), l);
			}
			mergFbToFz(l, fb, yersterdayPrice);
		}
		return m;
	}

	public static void mergFbToFz(TickFz f, TickFb fb, double yesterday) {
		f.fen = fb.getFen();
		f.vol += fb.getVol();
		if (fb.getPrice() > f.hprice) {
			f.hprice = fb.getPrice();
		}
		if (fb.getPrice() < f.lprice || f.lprice == 0) {
			f.lprice = fb.getPrice();
		}
		if (fb.getPrice() > yesterday) {
			f.sx = 1;
		} else if (fb.getPrice() < yesterday) {
			f.sx = -1;
		}
	}

	// 分钟级别:Str to Vo
	public static TickFz strToTickfz(String line) {
		String[] ss = line.split(",");
		TickFz fz = new TickFz();
		fz.setFen(Integer.valueOf(ss[0]));
		fz.setSx(Integer.valueOf(ss[1]));
		fz.setVol(Long.valueOf(ss[2]));
		fz.setHprice(Double.valueOf(ss[3]));
		fz.setLprice(Double.valueOf(ss[4]));
		return fz;
	}

	// 分钟级别:vo to str
	public static String genTickfzToStr(TickFz fz) {
		return fz.getFen() + "," + fz.getSx() + "," + fz.getVol() + "," + fz.getHprice() + "," + fz.getLprice();
	}

	public static String TD_vo_to_str(TickDay td) {
		return td.getTop() + "," + td.getUpVol() + "," + td.getDownVol() + "," + td.getAvg();
	}

	// 分笔交易-腾讯
	public static TickFb getTick(String line) {
		TickFb tick = new TickFb();
		String[] fs = line.split("/");
		tick.setId(fs[0]);
		String time = fs[1];// 腾讯时间
		tick.setTime(time);
		String[] s = time.split(":");
		tick.setFen(Integer.valueOf(s[0] + s[1]));
		tick.setPrice(Double.valueOf(fs[2]));
		tick.setChange(Double.valueOf(fs[3]));
		tick.setVol(Long.valueOf(fs[4]));
		tick.setAmt(Double.valueOf(fs[5]));
		if ("S".equals(fs[6])) {
			tick.setBs(1);
		}
		return tick;
	}

	public static String getCode(String code) {
		if (code.startsWith("6")) {
			return "sh" + code;
		}
		return "sz" + code;
	}
}
