package com.stable.vo.http.resp;

import com.stable.vo.bus.CodeBaseModel2;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Setter
@Getter
@ToString
public class CodeBaseModelResp extends CodeBaseModel2 {

	private static final long serialVersionUID = 1L;
	private String codeName;
	private double circZb;
	private String baseInfo;
	private String zfjjInfo;
	private String tagInfo;
	private String bankuai;
	private String rengong;
	private String gnstr;
}
