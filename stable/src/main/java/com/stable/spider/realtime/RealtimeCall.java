package com.stable.spider.realtime;

import java.util.LinkedList;
import java.util.List;

public class RealtimeCall {

	public static List<RealtimeProxy> list = new LinkedList<RealtimeProxy>();
	static {
		list.add(new RealtimeSina());
//		list.add(new Realtime163());
	}
	private static int index = 0;

	public static int max = Integer.MAX_VALUE - 100000;

	private static int getIndex() {
		int i = index % 2;
		index++;
		if (index >= max) {// 并发问题会导致在index++越界
			index = 0;
		}
		return i;
	}

	public static int source = 1;// 0：全部 | 1：新浪 | 2：163

	public static RealTime get(String code) {
//		RealTime rt = null;
//		if (source > 0) {
//			if (source == 1) {// 指定sina
//				rt = list.get(0).get(code);
//			} else if (source == 2) {// 指定163
//				rt = list.get(1).get(code);
//			}
//		} else {
//			// 随机： 轮选调用
//			rt = list.get(getIndex()).get(code);
//		}
//		if (rt == null) {
//			return getRealTimeCycle(code);// 异常的时候，顺序调用
//		}
		return list.get(0).get(code);
	}

	private static RealTime getRealTimeCycle(String code) {
		for (int i = 0; i < list.size(); i++) {
			RealTime rt = list.get(i).get(code);
			if (rt != null) {
				return rt;
			}
		}
		return new RealTime();
	}
}
