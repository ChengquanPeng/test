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

	String zfPriceLow;

	int compnayType;

	int zfObjType;
	String holderNumT3;
	String pettm;

	String totalAmt;
	String totalAmt60d;
	String kline;
	String pre1Year;

	int shooting1;
	int shooting2;
	int shooting3;
	int shooting4;
	int shooting6;
	int shooting7;
	int shooting8;
	int shooting9;
	int whiteHors = 0;
	int shooting6661;
	int shooting6662;
	String rzrq1;
	String rzrq2;

	boolean trymsg;

	String gdrs;
	String gdrsp;

	int zyxing = 0;// 中阳带星
	int dibuqixing = 0;
	int dibuqixing2 = 0;
	int xipan = 0;
	int nxipan = 0;

	// 财务
	int bousOK;
	int finOK;
	int finDbl = 0;
	int finBoss = 0;
	int finSusBoss = 0;
	int financeInc = 0;
	int bossInc;
	double bossVal = 0;
	int dzjyBreaks = 0;
}
