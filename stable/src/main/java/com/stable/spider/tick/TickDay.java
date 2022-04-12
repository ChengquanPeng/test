package com.stable.spider.tick;

import lombok.Data;

@Data
public class TickDay {

	private long top;// 全天最高
	private long upVol;// 水上最高
	private long downVol;// 水下最高
	// (以上除去0925，0930，1500)
	private long avg;// 平均

	private double tp;// -价格
	private double up;// -价格
	private double dp;// -价格
}
