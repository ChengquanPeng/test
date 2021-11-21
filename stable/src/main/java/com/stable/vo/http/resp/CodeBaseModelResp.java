package com.stable.vo.http.resp;

import com.stable.vo.bus.CodeBaseModel2;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class CodeBaseModelResp extends CodeBaseModel2 {

	private static final long serialVersionUID = 1L;
	private String codeName;
	private double circZb;
	private String baseInfo;
	private String sylDesc;
	private String codeType;
	private String zfjjInfo;
	private String sortInfo;
	private String monitorDesc;
	private String zfAmtInfo;
	private String tagInfo;
}
