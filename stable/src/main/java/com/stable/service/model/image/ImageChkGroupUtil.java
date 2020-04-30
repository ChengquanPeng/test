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

	public static List<ImageChkGroup> getList() {
		Properties env = PropertiesUtil.getProperties();
		List<ImageChkGroup> list = new LinkedList<ImageChkGroup>();
		Integer indexEnd = Integer.valueOf(env.getProperty("image.index.end"));
		if (indexEnd <= 0) {
			log.info("indexEnd<=0,return null list");
			return list;
		}
		int i = 1;
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

				if (i >= indexEnd) {
					log.info("indexEnd:{},return list", indexEnd);
					return list;
				}
				i++;
			} catch (Exception e) {
				return list;
			}
		}
	}

}
