package com.stable.vo.http.resp;

import com.stable.vo.bus.CodeBaseModel2;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class CodeBaseModelResp extends CodeBaseModel2 {

	private static final long serialVersionUID = 1L;
	private String codeName;
	private String incomeShow;
	private String profitShow;
}
