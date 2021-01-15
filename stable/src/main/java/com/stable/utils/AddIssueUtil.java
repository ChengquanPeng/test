package com.stable.utils;

import com.stable.constant.Constant;
import com.stable.vo.bus.AddIssue;

import lombok.Data;

@Data
public class AddIssueUtil {

	private String code;
	private int startting;
	private int endding;
	private String stitles = null;
	private String etitles = null;
	private int die = 0;
	private int process = 0;

	public boolean addLine(int index, String date, String title) {
		if (index == 1) {// 增发
			startting = DateUtil.convertDate2(date);
			stitles = date + " " + title;
		} else {
			if (etitles == null) {
				if (index == 2) {// 完成
					endding = DateUtil.convertDate2(date);
					etitles = date + " " + title;
				} else {// 终止
					die = 1;
					endding = DateUtil.convertDate2(date);
					etitles = date + " " + title;
				}
			} else {
				if (startting > 0) {// 下一个轮回
					return false;
				}
			}
		}
		return true;
	}

	public AddIssue getAddIssue() {
		AddIssue iss = new AddIssue();
		iss.setCode(code);
		iss.setStartDate(startting);
		iss.setEndDate(endding);
		if (stitles != null && etitles != null) {
			iss.setTitles(stitles + Constant.HTML_LINE + etitles);
		} else if (stitles != null) {
			iss.setTitles(stitles);
		}

		iss.setId(code + startting);
		if (die == 1) {
			iss.setStatus(3);
		} else {
			if (endding > 0) {
				iss.setStatus(2);
			} else {
				iss.setStatus(1);
			}
		}
		return iss;
	}
}
