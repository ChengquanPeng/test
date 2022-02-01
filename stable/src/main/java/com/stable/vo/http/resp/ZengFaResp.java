package com.stable.vo.http.resp;

import com.stable.vo.bus.ZengFa;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Setter
@Getter
@ToString
public class ZengFaResp extends ZengFa {
	private static final long serialVersionUID = 1L;
	private String codeName;
	private String buy;// 0无，1购买资产 
	private String selfzf;//自己人在增发， 0无，1确定，2不确定 
	private String compType;//企业类型， 0未知，1国企，2民企 
	private String yujiVal;//预计金额
}
