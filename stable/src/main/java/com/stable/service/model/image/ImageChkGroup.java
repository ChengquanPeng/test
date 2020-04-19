package com.stable.service.model.image;

import lombok.Data;

@Data
public class ImageChkGroup {

	int id = 0;
	private String standardImgp;
	private String standardImgv;
	private double checklinep = 99.0;
	private double checklinev = 99.0;
	private int recordsSize = 0;

}
