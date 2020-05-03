package com.stable.utils;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.stable.constant.Constant;
import com.stable.vo.ImageData;

import lombok.extern.log4j.Log4j2;

@Service
@Log4j2
public class ImageGeneratingByPythonUtil {

	@Value("${python.file.image.generating}")
	private String pythoneFile;

	public synchronized void generateImages(String filePath, List<ImageData> data) {
		if (data == null || data.size() <= 10) {
			log.info("List<ImageData> data is Null,or size<=10");
			return;
		}
		StringBuffer sb = new StringBuffer();
		for (ImageData imageData : data) {
			sb.append(imageData.getNum()).append(Constant.DOU_HAO);
		}
		sb.deleteCharAt(sb.length() - 1);

		PythonCallUtil.callPythonScriptNoReturn(pythoneFile, sb.toString() + " " + filePath);
	}

}
