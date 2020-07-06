package com.stable.vo.http.resp;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class ViewVo {

	private int index;
	private String id;
	private String code;
	private String name;
	private int buyDate;
	private double buyPrice;
	private int soldDate;
	private double soldPrice;
	private double profit;
	private int program;// 市场:1程序，2无程序
}
