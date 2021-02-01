package com.stable.vo.bus;

import java.beans.Transient;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Document(indexName = "code_base_model2")
public class CodeBaseModel2 extends EsBase {
	private static final long serialVersionUID = 1L;

	@Id
	private String id;
	@Field(type = FieldType.Text)
	private String code;
	@Field(type = FieldType.Integer)
	protected int date;

	@Field(type = FieldType.Integer)
	private int monitor;// 是否监听

	@Field(type = FieldType.Integer)
	protected int currYear;
	@Field(type = FieldType.Integer)
	protected int currQuarter;

	@Field(type = FieldType.Integer)
	protected int baseRed;// 基本面：红色
	@Field(type = FieldType.Text)
	private String baseRedDesc;
	@Field(type = FieldType.Integer)
	protected int baseYellow;// 基本面：黄色
	@Field(type = FieldType.Text)
	private String baseYellowDesc;
	@Field(type = FieldType.Integer)
	protected int baseBlue;// 基本面：蓝色
	@Field(type = FieldType.Text)
	private String baseBlueDesc;
	@Field(type = FieldType.Integer)
	protected int baseGreen;// 基本面：绿色
	@Field(type = FieldType.Text)
	private String baseGreenDesc;

	// 加权净资产收益率
	@Field(type = FieldType.Double)
	private double syl;// 加权净资产收益率
	@Field(type = FieldType.Double)
	private double syldjd;// 加权净资产收益率+单季度百分比
	@Field(type = FieldType.Double)
	private double sylttm;// 加权净资产收益率+单季度百分比
	@Field(type = FieldType.Integer)
	private int sylType;// 收益率类型:1:自身收益率增长,2: 年收益率超过5.0%*4=20%,4:同时包含12

	// 增发(最近的增发，包含正在增发）
	@Field(type = FieldType.Integer)
	private int zfStatus;// 增发状态（近1年）: 0无增发，1增发中，2增发完成，3，增发终止
	@Field(type = FieldType.Text)
	private String zfStatusDesc;// 增发进度

	// 已完成的增发
	@Field(type = FieldType.Integer)
	private int zfself = 0;// 自己人在增发
	@Field(type = FieldType.Integer)
	private int zfbuy = 0;// 是否购买资产
	@Field(type = FieldType.Integer)
	private int susZfBoss; // 增发博弈
	@Field(type = FieldType.Integer)
	private int zflastOkDate = 0;// 最后已实施时间
	// 增发解禁
	@Field(type = FieldType.Integer)
	private int zfjj; // 增发解禁
	@Field(type = FieldType.Integer)
	private int zfjjup; // 增发未大涨
	@Field(type = FieldType.Integer)
	private int zfjjDate = 0;// 最近的增发解禁时间

	// 交易面
	@Field(type = FieldType.Integer)
	private int susBigBoss; // 疑似大牛
	@Field(type = FieldType.Integer)
	private int susWhiteHors; // 疑似白马
	// 短线
	@Field(type = FieldType.Integer)
	private int sortMode6 = 0;// 符合短线 //短线模型6(前期3-50%吸筹，深度回踩突然涨停后再2-5个交易日回踩拉起,涨停日不放量，超过涨停价格后买入，买入2内未大幅拉升放弃
	@Field(type = FieldType.Integer)
	private int sortMode7 = 0;// 突破箱体震荡 --//0未知，1待确认，2确认，3，不符合

	// 人工确定
	@Field(type = FieldType.Integer)
	private int susZfBossSure;
	@Field(type = FieldType.Integer)
	private int susBigBossSure;
	@Field(type = FieldType.Integer)
	private int susWhiteHorsSure;
	@Field(type = FieldType.Integer)
	private int sortMode6Sure;
	@Field(type = FieldType.Integer)
	private int sortMode7Sure;

	// 股东人数
	@Field(type = FieldType.Double)
	private double holderNum;

	@Transient
	public String getKeyString() {
		StringBuffer sb = new StringBuffer();
		sb.append(baseRedDesc).append("|");
		sb.append(baseYellowDesc).append("|");
		sb.append(baseBlueDesc).append("|");
		sb.append(baseGreenDesc).append("|");
		sb.append(sylType).append("|");
		sb.append(zfStatus).append("|");
		return sb.toString();
	}
}
