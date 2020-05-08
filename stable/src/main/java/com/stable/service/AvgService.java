package com.stable.service;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.stable.spider.tushare.TushareSpider;
import com.stable.utils.CurrencyUitl;
import com.stable.utils.PythonCallUtil;
import com.stable.vo.AvgVo;
import com.stable.vo.up.strategy.ModelV1;

import lombok.extern.log4j.Log4j2;

@Service
@Log4j2
public class AvgService {

	@Value("${python.file.daily.avg}")
	private String pythonFileName;

	public void checkAvg(ModelV1 mv1, int startDate, AvgVo av) {
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
				mv1.setAvgIndex(0);
				av.setAvgIndex3(Double.valueOf(strs[2]));
				av.setAvgIndex5(Double.valueOf(strs[3]));
				av.setAvgIndex10(Double.valueOf(strs[4]));
				av.setAvgIndex20(Double.valueOf(strs[5]));
				av.setAvgIndex30(Double.valueOf(strs[6]));
				av.setAvgIndex120(Double.valueOf(strs[7]));
				av.setAvgIndex250(Double.valueOf(strs[8]));

				// 计算AvgIndex
				if (av.getAvgIndex250() > 0) {
					if (av.getAvgIndex3() >= av.getAvgIndex5() && av.getAvgIndex5() >= av.getAvgIndex10()
							&& av.getAvgIndex10() >= av.getAvgIndex20() && av.getAvgIndex20() >= av.getAvgIndex30()
							&& av.getAvgIndex30() >= av.getAvgIndex120()
							&& av.getAvgIndex120() >= av.getAvgIndex250()) {
						mv1.setAvgIndex(15);
						return;
					}
				}
				if (av.getAvgIndex3() >= av.getAvgIndex5() && av.getAvgIndex5() >= av.getAvgIndex10()
						&& av.getAvgIndex10() >= av.getAvgIndex20() && av.getAvgIndex20() >= av.getAvgIndex30()) {
					mv1.setAvgIndex(12);
					return;
				}
				if (av.getAvgIndex3() >= av.getAvgIndex5() && av.getAvgIndex5() >= av.getAvgIndex10()
						&& av.getAvgIndex10() >= av.getAvgIndex20()) {
					mv1.setAvgIndex(10);
					return;
				}

				List<Double> list = new LinkedList<Double>();
				list.add(av.getAvgIndex3());
				list.add(av.getAvgIndex5());
				list.add(av.getAvgIndex10());
				list.add(av.getAvgIndex20());
				list.add(av.getAvgIndex30());
				double max = Collections.max(list);
				double min = Collections.min(list);
				if (min >= CurrencyUitl.lowestPrice(max, true)) {// 最高价和最低价在5%以内的
					mv1.setAvgIndex(5);
					return;
				}
				if (min >= CurrencyUitl.lowestPrice(max, false)) {// 最高价和最低价在10%以内的
					mv1.setAvgIndex(4);
					return;
				}
			} catch (Exception e) {
				log.error("获取到的数据:" + lines.get(0));
				e.printStackTrace();
			}
		}
	}
}
