package com.stable.vo;

import lombok.Data;

@Data
public class AnnMentParam {
	public AnnMentParam(int t, String p1, String p2) {
		this.type = t;
		this.paramCn = p1;
		this.paramGet = p2;
	}

	int type;
	String paramCn;
	String paramGet;
}