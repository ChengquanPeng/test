package com.stable.vo.bus;

import java.beans.Transient;
import java.util.UUID;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import lombok.Data;

@Data
@Document(indexName = "run_log")
public class RunLog extends EsBase {
	@Id
	private String id;
	@Field(type = FieldType.Integer)
	private int date;
	@Field(type = FieldType.Integer)
	private int btype;
	@Field(type = FieldType.Integer)
	private int status;
	@Field(type = FieldType.Integer)
	private int runCycle;
	@Field(type = FieldType.Text)
	private String startTime;
	@Field(type = FieldType.Text)
	private String endTime;
	@Field(type = FieldType.Text)
	private String remark;
	
	private String btypeName;
	private String cycleName;
	private String statusName;
	

	@Transient
	public void setBtypeName(String btypeName) {
		this.btypeName = btypeName;
	}
	@Transient
	public void setCycleName(String cycleName) {
		this.cycleName = cycleName;
	}
	@Transient
	public void setStatusName(String statusName) {
		this.statusName = statusName;
	}

	public RunLog() {
		id = UUID.randomUUID().toString();
	}
}
