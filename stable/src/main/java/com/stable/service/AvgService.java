package com.stable.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.stable.spider.tushare.TushareSpider;
import com.stable.utils.PythonCallUtil;
import com.stable.vo.up.strategy.ModelV1;

import lombok.extern.log4j.Log4j2;

@Service
@Log4j2
public class AvgService {

	@Value("${python.file.daily.avg}")
	private String pythonFileName;

	public void checkAvg(ModelV1 mv1, int startDate) {
		String code = mv1.getCode();
		String params = TushareSpider.formatCode(code) + " " + startDate + " " + mv1.getDate() + " qfq D";
		List<String> lines = PythonCallUtil.callPythonScript(pythonFileName, params);
		if (lines == null || lines.isEmpty() || lines.get(0).startsWith(PythonCallUtil.EXCEPT)) {
			log.warn("pythonFileName：{}，未获取到数据 params：{}", pythonFileName, code, params);
			if (lines != null && !lines.isEmpty()) {
				log.error("Python 错误：code：{}，PythonCallUtil.EXCEPT：{}", code, lines.get(0));
			}
			return;
		}
		// code,date,3,5,10,20,30,120,250
		// 600408.SH,20200403,2.2933,2.302,2.282,2.3255,2.297,2.2712,2.4559
		String[] strs = lines.get(0).split(",");
		if (strs[1].equals(String.valueOf(mv1.getDate()))) {
			try {
				mv1.setAvgIndex3(Double.valueOf(strs[2]));
				mv1.setAvgIndex5(Double.valueOf(strs[3]));
				mv1.setAvgIndex10(Double.valueOf(strs[4]));
				mv1.setAvgIndex20(Double.valueOf(strs[5]));
				mv1.setAvgIndex30(Double.valueOf(strs[6]));
				mv1.setAvgIndex120(Double.valueOf(strs[7]));
				mv1.setAvgIndex250(Double.valueOf(strs[8]));
			} catch (Exception e) {
				log.error("获取到的数据:" + lines.get(0));
				e.printStackTrace();
			}
		}
	}
}
