package com.stable.spider.realtime;

import java.util.LinkedList;
import java.util.List;

public class RealtimeCall {

	public static List<RealtimeProxy> list = new LinkedList<RealtimeProxy>();
	static {
		list.add(new RealtimeSina());
		list.add(new Realtime163());
	}
	private static int index = 0;

	public static int max = Integer.MAX_VALUE - 100000;

	public static int getIndex() {
		int i = index % 2;
		index++;
		if (index >= max) {// 并发问题会导致在index++越界
			index = 0;
		}
		return i;
	}

	public static RealTime get(String code) {
		RealTime rt = list.get(getIndex()).get(code);// 轮选调用
		if (rt == null) {
			return getRealTimeCycle(code);// 异常的时候，顺序调用
		}
		return rt;
	}

	public static RealTime getRealTimeCycle(String code) {
		for (int i = 0; i < list.size(); i++) {
			RealTime rt = list.get(i).get(code);
			if (rt != null) {
				return rt;
			}
		}
		return new RealTime();
	}
}
