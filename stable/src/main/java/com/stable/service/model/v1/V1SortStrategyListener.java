package com.stable.service.model.v1;

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

import com.stable.config.SpringConfig;
import com.stable.service.model.StrategyListener;
import com.stable.utils.FileWriteUitl;
import com.stable.utils.SpringUtil;
import com.stable.vo.up.strategy.ModelV1;

import lombok.extern.log4j.Log4j2;

@Log4j2
public class V1SortStrategyListener implements StrategyListener {
	private String header = "<table><tr>";

	private List<ModelV1> set = new LinkedList<ModelV1>();

	public void condition(Object... obj) {
		ModelV1 mv1 = (ModelV1) obj[0];
		// 短期强势
		if (mv1.getSortStrong() > 0) {
			set.add(mv1);
		}
	}

	public V1SortStrategyListener() {
		String[] s = { "序号", "代码", "日期", "综合评分", "价格均线评分", "交易量评分", "图形匹配成功", "短期强势评分", "程序单评分", "买卖评分", "价格指数",
				"详情ID" };
		for (int i = 0; i < s.length; i++) {
			header += this.getHTML(s[i]);
		}
		header += "</tr>";
	}

	public void fulshToFile() {
		log.info("List<ModelV1> size:{}", set.size());
		if (set.size() > 0) {
			sort();
			SpringConfig efc = SpringUtil.getBean(SpringConfig.class);
			String filepath = efc.getModelV1SortFloder() + "sort_v1_" + set.get(0).getDate() + ".html";
			FileWriteUitl fw = new FileWriteUitl(filepath, true);
			StringBuffer sb = new StringBuffer(header);
			sb.append(FileWriteUitl.LINE_FILE);
			int index = 1;

			for (ModelV1 mv : set) {
				sb.append("<tr>").append(getHTML(index)).append(getHTML(mv.getCode())).append(getHTML(mv.getDate()))
						.append(getHTML(mv.getScore())).append(getHTML(mv.getAvgIndex()))
						.append(getHTML(mv.getVolIndex())).append(getHTML(mv.getImageIndex() == 1 ? "Y" : "N"))
						.append(getHTML(mv.getSortStrong())).append(getHTML(mv.getSortPgm()))
						.append(getHTML(mv.getSortWay())).append(getHTML(mv.getPriceIndex()))
						.append(getHTML(mv.getId())).append("</tr>").append(FileWriteUitl.LINE_FILE);
				index++;
			}
			sb.append("</table>");
			fw.writeLine(sb.toString());
			fw.close();
		}
	}

	private String getHTML(Object text) {
		return "<td>" + text + "</td>";
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
