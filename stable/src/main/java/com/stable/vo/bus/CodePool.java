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
@Document(indexName = "code_pool")
public class CodePool {
	@Id
	private String code;

	// ---------------------------
	// 是否加入监听
	@Field(type = FieldType.Integer)
	private int addPoolmid = 0;
	// 加入日期
	@Field(type = FieldType.Text)
	private String midRemark;

	// 是否满足中线要求
	@Field(type = FieldType.Integer)
	private int midOk = 0;

	// 检查日期
	@Field(type = FieldType.Integer)
	private int midChkDate = 0;
	// -----------------------------

	// --------------sortV4------
	@Field(type = FieldType.Integer)
	private int addPoolSortV4 = 0;
	// 加入日期
	@Field(type = FieldType.Text)
	private String sortV4Remark;

	@Field(type = FieldType.Integer)
	private int sortV4Ok = 0;

	@Field(type = FieldType.Integer)
	private int sortV4ChkDate = 0;

	// ------------ manual--人工手动---
	@Field(type = FieldType.Integer)
	private int addPoolManual = 0;

	@Field(type = FieldType.Text)
	private String remark;
}
