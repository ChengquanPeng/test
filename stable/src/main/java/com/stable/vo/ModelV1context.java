package com.stable.vo;

import com.stable.constant.Constant;

import lombok.Data;

@Data
public class ModelV1context {
	// 1强势:短中长期买入=>次数和差值:3/5/10/20/120/250天
	private StringBuffer detailDesc;

	private int wayTimes3;
	private Long wayDef3;
	private int wayTimes5;
	private Long wayDef5;
	private int wayTimes10;
	private Long wayDef10;
	private int wayTimes20;
	private Long wayDef20;
	private int wayTimes120;
	private Long wayDef120;
	private int wayTimes250;
	private Long wayDef250;

	private int pgmTimes3;
	private int pgmTimes5;
	private int pgmTimes10;
	private int pgmTimes20;
	private int pgmTimes120;
	private int pgmTimes250;

	private StringBuffer gn;

	public void addDetailDesc(String desc) {
		if (detailDesc == null) {
			detailDesc = new StringBuffer();
		}
		detailDesc.append(desc).append(Constant.DOU_HAO);
	}

	public String getDetailDescStr() {
		if (gn == null) {
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
}
