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
	// 营收净利level
	@Field(type = FieldType.Integer)
	private int baseLevel = 0;

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
	// 是否加入监听
	@Field(type = FieldType.Integer)
	private int midOk = 0;
	// 加入日期
	@Field(type = FieldType.Text)
	private String midRemark;

	// 是否满足中线要求
	@Field(type = FieldType.Integer)
	private int inMid = 0;

	// 检查日期
	@Field(type = FieldType.Integer)
	private int midChkDate = 0;
	// -----------------------------

	// --------------sortV4------
	@Field(type = FieldType.Integer)
	private int sortOk = 0;
	// 加入日期
	@Field(type = FieldType.Text)
	private String sortV4Remark;

	// ------------ manual--人工手动---
	@Field(type = FieldType.Integer)
	private int manualOk = 0;

	@Field(type = FieldType.Text)
	private String remark;

	@Transient
	public boolean isIsok() {
		return isok;
	}

	@Transient
	public void setIsok(boolean isok) {
		this.isok = isok;
	}

}
