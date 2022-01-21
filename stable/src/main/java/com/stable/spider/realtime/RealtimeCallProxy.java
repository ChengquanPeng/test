package com.stable.spider.realtime;

public class RealtimeCallProxy {

	public static RealTime get(String code) {
//		RealTime rt = RealtimeSina.get(code);
//		if (rt == null) {
//			rt = Realtime163.get(code);
//		}
		RealTime rt = Realtime163.get(code);
		if (rt == null) {
			rt = new RealTime();
		}
		return rt;
	}
}
