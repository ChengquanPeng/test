package com.stable.service.model.v1;

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

import com.stable.config.SpringConfig;
import com.stable.service.model.StrategyListener;
import com.stable.utils.DateUtil;
import com.stable.utils.FileWriteUitl;
import com.stable.utils.SpringUtil;
import com.stable.vo.up.strategy.ModelV1;

import lombok.extern.log4j.Log4j2;

@Log4j2
public class V1SortStrategyListener implements StrategyListener {

	private List<ModelV1> set = new LinkedList<ModelV1>();

	public void condition(Object... obj) {
		ModelV1 mv1 = (ModelV1) obj[0];
		// 短期强势
		if (mv1.getSortStrong() > 0) {
			set.add(mv1);
		}
	}

	public void fulshToFile() {
		log.info("List<ModelV1> size:{}", set.size());
		if (set.size() > 0) {
			SpringConfig efc = SpringUtil.getBean(SpringConfig.class);
			String filepath = efc.getModelV1SortFloder() + "sort_v1_" + DateUtil.getTodayYYYYMMDDHHMMSS_NOspit()
					+ ".txt";
			FileWriteUitl fw = new FileWriteUitl(filepath, true);
			StringBuffer sb = new StringBuffer();
			sort();
			for (ModelV1 mv : set) {
				sb.append(String.format(
						"代码:%s,日期:%s,综合评分:%s,均线评分:%s,图形匹配成功:%s,短期强势评分:%s,程序单评分:%s,买卖评分:%s,价格指数:%s,详情ID:%s",
						mv.getCode(), mv.getDate(), mv.getScore(), mv.getAvgIndex(),
						(mv.getImageIndex() == 1 ? "Y" : "N"), mv.getSortStrong(), mv.getSortPgm(), mv.getSortWay(),
						mv.getPriceIndex(), mv.getId())).append(FileWriteUitl.LINE);
			}
			fw.writeLine(sb.toString());
			fw.close();
		}
	}

	private void sort() {
		Collections.sort(set, new Comparator<ModelV1>() {
			@Override
			public int compare(ModelV1 o1, ModelV1 o2) {
				return o2.getScore() - o1.getScore();
			}
		});
	}

}
