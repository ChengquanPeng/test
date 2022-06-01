package com.stable.vo.http.req;

import lombok.Data;

@Data
public class ModelReq {
	String code;
	int orderBy;
	int asc;
	String conceptId;
	String conceptName;
	String zfStatus;
	String monitor;
	String bred;
	String byellow;
	String bsyl;
	int sort6;
	int sort7;
	int zfbuy;
	int zfjj;
	String zfjjup;
	String zfjjupStable;
	int zfself;
	String zfYjAmt;
	String zfYjAmt2;
	String mkv;
	String mkv2;
	int pls;
	int tagIndex;
	int shooting;
	String zfPriceLow;
	int dzjyRct;

	int bousOK;
	int finOK;
	int compnayType;

	int zfObjType;
	String holderNumT3;
	String pettm;

	String totalAmt;
	String totalAmt60d;
	String kline;
	String pre1Year;

	int shooting51 = 0;
	int shooting52 = 0;
	int shooting53 = 0;
	int whiteHors = 0;
	boolean trymsg;
}
