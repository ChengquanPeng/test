package com.stable.vo;

import java.util.Date;

import lombok.Data;

@Data
public class ImageData {

	Date date;
	Number num;

	public ImageData(Date d, Number n) {
		this.date = d;
		this.num = n;
	}
}
