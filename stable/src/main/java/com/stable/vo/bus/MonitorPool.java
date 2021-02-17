package com.stable.vo.bus;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Setter
@Getter
@ToString
@Document(indexName = "monitor_pool")
public class MonitorPool {
	@Id
	private String code;
	@Field(type = FieldType.Integer)
	private int updateDate = 0;
	// ---------------------------
	// 监听:0不监听，1大牛，2中线，3人工，4短线，5.增发监听,6系统自动监听
	@Field(type = FieldType.Integer)
	private int monitor = 0;
	@Field(type = FieldType.Text)
	private String remark;

	@Field(type = FieldType.Integer)
	private int realtime = 0;// 实时
	@Field(type = FieldType.Integer)
	private int offline = 0;// 收盘
	@Field(type = FieldType.Double)
	private double upPrice = 0;// 高于某价格
	@Field(type = FieldType.Integer)
	private double downPrice = 0;// 低于某价格
	@Field(type = FieldType.Double)
	private double upTodayChange = 0;// 高于某涨幅
	@Field(type = FieldType.Integer)
	private double downTodayChange = 0;// 低于某涨幅
	@Field(type = FieldType.Integer)
	private int ykb = 0;// 快预报(0不预警，1不亏，2亏损）
	@Field(type = FieldType.Integer)
	private int zfdone = 0;// 定增预警(0不预警，1预警）
	@Field(type = FieldType.Integer)
	private int holderNum = 0;// 股东人数(0不预警，date:截止日期）
	@Field(type = FieldType.Integer)
	private int buyLowVol = 0;// 地量(0不预警，1多少个交易日）

	// 增发

	// 短线

	//

	public String getMsg() {
		StringBuffer sb = new StringBuffer();
		if (realtime == 1) {
			sb.append("在线监听,");
		}
		if (offline == 1) {
			sb.append("离线监听,");
		}
		if (upPrice > 0) {
			sb.append("股价上涨到" + upPrice + ",");
		}
		if (upTodayChange > 0) {
			sb.append("涨幅" + upTodayChange + ",");
		}
		if (downPrice > 0) {
			sb.append("股价下跌到" + downPrice + ",");
		}
		if (downTodayChange > 0) {
			sb.append("跌幅" + downTodayChange + ",");
		}
		return sb.toString();
	}
}
