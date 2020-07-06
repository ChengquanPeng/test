package com.stable.vo.http.resp;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class ReportVo {

	private String all;// 数据范围
	private String fromDate;// 日期范围
	// 所有交易
	private int allCnt;// 所有总数
	private double allProfit;// 所有总盈亏
	private int ynowCnt;// 浮动盈利总数
	private double ynowProfit;// 浮动盈利总盈亏
	private double nowRate;// 当前成功率（含未卖的）
	// 已完成交易
	private int soldCnt;// 已卖总数
	private double soldProfit;// 已卖总盈亏
	private int ysoldCnt;// 已卖中的盈利总数
	private double ysoldProfit;// 已卖中的盈利总盈亏
	private double soldRate;// 已卖成功率

}
