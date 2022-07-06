package com.stable.vo.http.resp;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Setter
@Getter
@ToString
public class PlateStat {

	private String code;
	private String codeName;

	private int ct = 0;
	private int cw = 0;
	private int cd1 = 0;
	private int cd2 = 0;
	private int cd3 = 0;

	private double rw = 0;
	private double rd = 0;
}
