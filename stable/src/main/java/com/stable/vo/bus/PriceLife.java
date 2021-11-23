package com.stable.vo.bus;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Setter
@Getter
@ToString
public class PriceLife {

	private String code;
	private double highest;
	private int highDate;
	private double lowest;
	private int lowDate;
}
