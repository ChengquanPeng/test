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
}
