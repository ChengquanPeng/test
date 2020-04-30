package com.stable.service.model.v1;

import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.stable.config.SpringConfig;
import com.stable.service.model.StrategyListener;
import com.stable.utils.DateUtil;
import com.stable.utils.FileWriteUitl;
import com.stable.utils.SpringUtil;
import com.stable.vo.up.strategy.ModelV1;

import lombok.extern.log4j.Log4j2;

@Log4j2
public class ImageStrategyListener implements StrategyListener {

	private List<ModelV1> set = new LinkedList<ModelV1>();
	private List<String> ress = new LinkedList<String>();

	public void condition(Object... obj) {
		String imgResult = (String) obj[1];
		if (StringUtils.isNotBlank(imgResult)) {
			set.add((ModelV1) obj[0]);
			ress.add(imgResult);
		}
	}

	public void fulshToFile() {
		log.info("List<ModelV1> size:{}", set.size());
		if (set.size() > 0) {
			SpringConfig efc = SpringUtil.getBean(SpringConfig.class);
			String filepath = efc.getModelImageFloder() + "images_" + DateUtil.getTodayYYYYMMDDHHMMSS_NOspit() + ".txt";
			FileWriteUitl fw = new FileWriteUitl(filepath, true);
			StringBuffer sb = new StringBuffer();
			for (int i = 0; i < set.size(); i++) {
				ModelV1 mv = set.get(i);
				sb.append(String.format("代码:%s,日期:%s,匹配图形:%s", mv.getCode(), mv.getDate(), ress.get(i)))
						.append(FileWriteUitl.LINE);
			}
			fw.writeLine(sb.toString());
			fw.close();
		}
	}

}
