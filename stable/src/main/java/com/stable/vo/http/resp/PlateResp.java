package com.stable.vo.http.resp;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class PlateResp {

	private String code;
	private String codeName;
	double avgt1 = 0.0;
	double avgt2 = 0.0;
	double avgt3 = 0.0;

	double t1 = 0.0;
	double t2 = 0.0;
	double t2s = 0.0;
	double t3 = 0.0;

	int ranking1 = 0;
	int ranking2 = 0;
	int ranking3 = 0;
}
