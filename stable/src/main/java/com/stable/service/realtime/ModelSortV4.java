package com.stable.service.realtime;

import com.stable.vo.up.strategy.ModelV1;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ModelSortV4 extends ModelV1 {

	private double buyPirce;
	private double todayChange;
	private String gn;
}
