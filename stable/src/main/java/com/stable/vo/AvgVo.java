package com.stable.vo;

import lombok.Data;

@Data
public class AvgVo {
	// 1强势:短中长期强势=>次数和差值:3/5/10/20/120/250天

	private double avgIndex3;
	private double avgIndex5;
	private double avgIndex10;
	private double avgIndex20;
	private double avgIndex30;
	private double avgIndex120;
	private double avgIndex250;
}
