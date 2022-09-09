package com.stable.service.datamv;

import java.util.List;

import lombok.Data;

@Data
public class Dw<T> {

	public String tableName;
	public long tableSize;
	public int batchSize;
	public List<T> tableData;
}
