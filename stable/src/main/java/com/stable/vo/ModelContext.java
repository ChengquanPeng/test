package com.stable.vo;

import java.util.List;
import java.util.Map;

import com.stable.constant.Constant;
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
	private boolean baseDataOk = true;
	private int priceIndex = 0;
	private Map<String, List<ConceptInfo>> gnDaliy;

	private int score;
	private boolean base30Avg;
	private int sortWay;
	private int sortPgm;
	private StringBuffer detailDesc;

	private int wayTimes3;
	private Long wayDef3;
	private int wayTimes5;
	private Long wayDef5;

	private int pgmTimes3;
	private int pgmTimes5;

	private StringBuffer gn;

	private String dropOutMsg;

	private String imgResult;

	public void addDetailDesc(String desc) {
		if (detailDesc == null) {
			detailDesc = new StringBuffer();
		}
		detailDesc.append(desc).append(Constant.DOU_HAO);
	}

	public String getDetailDescStr() {
		if (detailDesc == null) {
			return "";
		}
		return detailDesc.toString();
	}

	public void addGnStr(String str) {
		if (gn == null) {
			gn = new StringBuffer();
		}
		gn.append(Constant.FEN_HAO).append(str);
	}

	public String getGnStr() {
		if (gn == null) {
			return "";
		}
		return gn.toString().replaceFirst(Constant.FEN_HAO, "");
	}

	public StringBuffer getDetailDesc() {
		return detailDesc;
	}

	public void setDetailDesc(StringBuffer detailDesc) {
		this.detailDesc = detailDesc;
	}

	public StringBuffer getGn() {
		return gn;
	}

	public void setGn(StringBuffer gn) {
		this.gn = gn;
	}
}