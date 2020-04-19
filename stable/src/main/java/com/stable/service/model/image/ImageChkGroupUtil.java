package com.stable.service.model.image;

import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import com.stable.utils.PropertiesUtil;

import lombok.extern.log4j.Log4j2;

@Log4j2
public class ImageChkGroupUtil {

	private static final String ip = "image.standard.p";
	private static final String iv = "image.standard.v";
	private static final String ipk = "image.chk.p";
	private static final String ivk = "image.chk.v";
	private static final String is = "image.records.size";

	public static List<ImageChkGroup> getList(){
		Properties env = PropertiesUtil.getProperties();
		int i = 1;
		List<ImageChkGroup> list = new LinkedList<ImageChkGroup>();
		while (true) {
			try {
				ImageChkGroup icg = new ImageChkGroup();

				icg.setId(i);
				icg.setStandardImgp(env.getProperty(ip + i));
				icg.setStandardImgv(env.getProperty(iv + i));
				icg.setChecklinep(Double.valueOf(env.getProperty(ipk + i)));
				icg.setChecklinev(Double.valueOf(env.getProperty(ivk + i)));
				icg.setRecordsSize(Integer.valueOf(env.getProperty(is + i)));
				log.info("图像参数:{}", icg);
				list.add(icg);
				i++;
			} catch (Exception e) {
				return list;
			}
		}
	}

}
