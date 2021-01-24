package com.stable.vo;

import java.util.LinkedList;
import java.util.List;

public class AnnMentParamUtil {
	public static final AnnMentParam zhengchi = new AnnMentParam(1, "增持", "%E5%A2%9E%E6%8C%81");
	public static final AnnMentParam jianchi = new AnnMentParam(2, "减持", "%E5%87%8F%E6%8C%81");
	public static final AnnMentParam huigou = new AnnMentParam(3, "回购", "%E5%9B%9E%E8%B4%AD");

	public static final List<AnnMentParam> types = new LinkedList<AnnMentParam>();
	static {
//		公告类型（1增持，2减持，3回购）
		// String fileName = URLEncoder.encode("减持", "utf-8");
		types.add(zhengchi);
		types.add(jianchi);
		types.add(huigou);
	}
}