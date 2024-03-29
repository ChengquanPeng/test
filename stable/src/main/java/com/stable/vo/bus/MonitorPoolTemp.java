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
@Document(indexName = "monitor_pool_user")
public class MonitorPoolTemp extends EsBase {
	private static final long serialVersionUID = -1111366231674941738L;
	@Id
	private String id;

	@Field(type = FieldType.Keyword)
	private String code;

	@Field(type = FieldType.Long)
	private long userId = 0;
	// 监听-公告
	@Field(type = FieldType.Double)
	private int listenerGg; // 监听-公告
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
	@Field(type = FieldType.Double)
	private double downPrice = 0;// 低于某价格
	@Field(type = FieldType.Double)
	private double upTodayChange = 0;// 高于某涨幅
	@Field(type = FieldType.Double)
	private double downTodayChange = 0;// 低于某涨幅
	@Field(type = FieldType.Integer)
	private int ykb = 0;// 快预报(0不预警，1不亏，2亏损）
	@Field(type = FieldType.Integer)
	private int zfdone = 0;// 定增预警(0不预警，1预警）
	@Field(type = FieldType.Integer)
	private int zfdoneZjh = 0;// 证监会核准
	@Field(type = FieldType.Integer)
	private int holderNum = 0;// 股东人数(0不预警，date:截止日期）
	@Field(type = FieldType.Integer)
	private int buyLowVol = 0;// 地量(0不预警，1多少个交易日）
//	@Field(type = FieldType.Integer)
//	private int xjl = 0;// 现金流净额转正(0不预警，1预警）
	@Field(type = FieldType.Integer)
	private int dzjy = 0;// 大宗交易(0不预警，date:截止日期）
//	@Field(type = FieldType.Double)
//	private double yearHigh1 = 0;// 1年历史新高
//	@Field(type = FieldType.Integer)
//	private int shotPointCheck = 0;// 起爆点

	@Field(type = FieldType.Integer)
	private int shotPointDate = 0;// 底部旗形-时间
	@Field(type = FieldType.Double)
	private double shotPointPrice = 0;// 底部旗形-股价预警
	@Field(type = FieldType.Double)
	private double shotPointPriceLow5 = 0;// 底部旗形-股价预警-5%
	@Field(type = FieldType.Double)
	private double shotPointPriceLow = 0;// 底部旗形-股价预警-1%
	@Field(type = FieldType.Double)
	private double shotPointPriceSzx = 0;// 十字星-股价预警
	@Field(type = FieldType.Double)
	private double rgqbPrice = 0;// 人工起爆预警
	@Field(type = FieldType.Double)
	private double xpPrice = 0;// 洗盘起爆预警

	@Field(type = FieldType.Integer)
	private int updatedate;
	@Field(type = FieldType.Double)
	private double prd1 = 0;// 产品1

	public String getMsg() {
		StringBuffer sb = new StringBuffer();
//		if (realtime == 1) {
//			sb.append("在线监听,");
//		}
//		if (offline == 1) {
//			sb.append("离线监听,");
//		}
		if (upPrice > 0) {
			sb.append("监听股价上涨到" + upPrice + ",");
		}
		if (upTodayChange > 0) {
			sb.append("监听涨幅" + upTodayChange + "%,");
		}
		if (downPrice > 0) {
			sb.append("监听股价下跌到" + downPrice + ",");
		}
		if (downTodayChange > 0) {
			sb.append("监听跌幅" + downTodayChange + "%,");
		}
		return sb.toString();
	}

	public void reset0() {
		if (this.monitor == 0) {
			this.ykb = 0;
			this.zfdone = 0;
			this.holderNum = 0;
			this.buyLowVol = 0;
			this.dzjy = 0;
			this.listenerGg = 0;
		}
	}
}
