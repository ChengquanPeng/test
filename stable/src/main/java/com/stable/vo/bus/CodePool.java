package com.stable.vo.bus;

import java.beans.Transient;

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
@Document(indexName = "code_pool")
public class CodePool {
	@Id
	private String code;
	@Field(type = FieldType.Integer)
	private int updateDate = 0;

	@Field(type = FieldType.Double)
	private double kbygys; // 营业总收入同比增长(%)
	@Field(type = FieldType.Double)
	private double kbygjl; // 归属净利润同比增长(%)

	@Field(type = FieldType.Double)
	private double pe;// float 市盈率（总市值/净利润）
	@Field(type = FieldType.Double)
	private double pe_ttm;// float 市盈率（TTM）
	@Field(type = FieldType.Double)
	private double pb;// float 市净率（总市值/净资产）

	private boolean isok = false; // 疑似大牛
	@Field(type = FieldType.Integer)
	private int suspectBigBoss; // 疑似大牛
	@Field(type = FieldType.Integer)
	private int continYj1;// 连续不间断业绩季度
	@Field(type = FieldType.Integer)
	private int continYj2;// 允许间段1个季度业绩
	// ---------------------------
	// 监听:0不监听，1大牛，2中线，3人工，4短线，5.增发监听
	@Field(type = FieldType.Integer)
	private int monitor = 0;
	@Field(type = FieldType.Text)
	private String remark;
	@Field(type = FieldType.Integer)
	private int inmid = 0;// 符合中线
	@Field(type = FieldType.Integer)
	private int inzf = 0;// 增发

	@Field(type = FieldType.Integer)
	private int sortMode6 = 0;// 符合短线 //短线模型6(前期3-50%吸筹，深度回踩突然涨停后再2-5个交易日回踩拉起,涨停日不放量，超过涨停价格后买入，买入2内未大幅拉升放弃
	@Field(type = FieldType.Integer)
	private int sortMode7 = 0;// 突破箱体震荡 --//0未知，1待确认，2确认，3，不符合
	@Field(type = FieldType.Text)
	private String sortMode6Remark;
	@Field(type = FieldType.Text)
	private String sortMode7Remark;

	@Field(type = FieldType.Integer)
	private int score = 0;// 分数

	@Field(type = FieldType.Integer)
	private int zfStatus;// 增发状态（近2年）: 0无增发，1增发中，2增发完成，3，增发终止

	@Transient
	public boolean isIsok() {
		return isok;
	}

	@Transient
	public void setIsok(boolean isok) {
		this.isok = isok;
	}

}
