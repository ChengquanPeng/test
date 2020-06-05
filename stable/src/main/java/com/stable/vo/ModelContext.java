package com.stable.vo;

import java.util.List;
import java.util.Map;

import com.stable.service.ConceptService.ConceptInfo;
import com.stable.vo.bus.DaliyBasicInfo;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class ModelContext {
	// 1强势:短中长期买入=>次数和差值:3/5/10/20/120/250天
	private String code;
	private int date;
	DaliyBasicInfo today;
	private String baseDataOk = "";
	private int priceIndex = 0;
	private Map<String, List<ConceptInfo>> gnDaliy;

	private int score;
	private boolean base30Avg;
	private int sortWay;
	private int sortPgm;

	private int wayTimes3;
	private Long wayDef3;
	private int wayTimes5;
	private Long wayDef5;

	private int pgmTimes3;
	private int pgmTimes5;

	private String imgResult;

}
