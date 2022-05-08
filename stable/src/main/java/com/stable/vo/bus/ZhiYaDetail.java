package com.stable.vo.bus;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Setter
@Getter
@ToString
public class ZhiYaDetail extends EsBase {

	private static final long serialVersionUID = 1L;

	private String id;// code+hash;
	private String code;
	private String holderName;// 质押股东
	private String pfOrg;// 质押方
	private long num;// 质押数量
	private int state;// 状态-1已解压,2,未达预警线,3已达预警线，4.其他
	private String stateDesc;// 状态-1已解压,2,未达预警线,3已达预警线，4.其他
	private double selfRatio;// 占自己所持比例
	private double totalRatio;// 占总股本比例
	private double closePrice;// 质押收盘价
	private double openline;// 平仓线
	private double warningLine;// 预警线
	private int noticeDate;// 公告日期
	private int startDate = 0;// 开始日期
	private int unfreezeDate;// 结束日期
	private String purpose;// 目的

	public ZhiYaDetail() {

	}

}
