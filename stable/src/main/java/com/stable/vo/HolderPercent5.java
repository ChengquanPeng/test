package com.stable.vo;

import java.util.LinkedList;
import java.util.List;

import lombok.Data;

@Data
public class HolderPercent5 {
	// 十大
	private List<String> list_a = new LinkedList<String>();
	// 流通
	private List<Double> list_l = new LinkedList<Double>();
}
