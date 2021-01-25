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
@Document(indexName = "annment_hist")
public class AnnouncementHist extends EsBase {

	private static final long serialVersionUID = 1L;
	@Id
	private String id;
	@Field(type = FieldType.Keyword)
	private String code;
	@Field(type = FieldType.Integer)
	private int rptDate;// 公告日
	@Field(type = FieldType.Integer)
	private int type;// 公告类型（1增持，2减持，3回购）
	@Field(type = FieldType.Text)
	private String title;// 标题
	@Field(type = FieldType.Integer)
	private int update;
	@Field(type = FieldType.Text)
	private String soureId;// 源id
	@Field(type = FieldType.Text)
	private String url;// URL
	@Field(type = FieldType.Integer)
	private int mkt;// 1深圳，2上海

	public AnnouncementHist() {

	}

}
