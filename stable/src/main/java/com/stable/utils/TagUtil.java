package com.stable.utils;

import java.util.List;

import com.stable.vo.bus.CodeBaseModel2;
import com.stable.vo.bus.CodeConcept;

public class TagUtil {

	// 底部
	public static boolean isDibu(CodeBaseModel2 cbm) {
		return cbm.getZfjjup() >= 2 && cbm.getZfjjupStable() >= 1;
	}

	// 底部-小票
	public static boolean isDibuSmall(boolean isSmallStock, CodeBaseModel2 cbm) {
		return isSmallStock && isDibu(cbm);
	}

	// 底部-大票-基本面OK
	public static boolean isDibuOKBig(boolean isSmallStock, CodeBaseModel2 cbm) {
		return !isSmallStock && cbm.getZfjjup() >= 1 && cbm.getZfjjupStable() >= 1 && cbm.getFinOK() >= 1
				&& cbm.getBousOK() >= 1;
	}

	// 业绩不错
	public static boolean isFinPerfect(CodeBaseModel2 cbm) {
		return cbm.getFinOK() >= 1 && cbm.getBousOK() >= 1 && (cbm.getFinDbl() > 0 || cbm.getFinanceInc() > 0);
	}

	// 概念
	public static String getGn(List<CodeConcept> l) {
		StringBuffer sb = new StringBuffer("");
		if (l != null) {
			for (CodeConcept cc : l) {
				sb.append(cc.getConceptName()).append(",");
			}
		}
		return sb.toString();
	}

	// 标签
	public static String getTag(CodeBaseModel2 cbm) {
		String you = "";
		if (cbm.getShooting7() > 0) {
			you = "[优]";
		} else {
			you = "[普]";
		}
		if (cbm.getShooting11() > 0) {
			you += "[大]" + you;
		}
		if (isFinPerfect(cbm)) {
			you += "[绩]" + you;
		}
		return you;
	}

	public static String getSystemPoint(CodeBaseModel2 dh, String splitor) {
		String s = "";
		// --中长--
		if (dh.getShooting7() > 0) {
			s += "底部优质小票" + splitor;
		} else if (dh.getShooting9() > 0) {
			s += "底部小票" + splitor;
		}
		if (dh.getShooting2() > 0) {
			s += "底部大票定增-涨停吸筹?" + splitor;
		}
		if (dh.getShooting1() > 0) {
			s += "底部小票大宗-占流通筹码超5%" + splitor;
		}
		if (dh.getShooting6() > 0) {
			s += "底部小票减持" + splitor;
		}
		if (dh.getShooting8() > 0) {
			s += "底部小票定增-" + dh.getZfjjupStable() + "年未涨" + splitor;
		}
		if (dh.getShooting4() > 0) {
			s += "底部股东人数大幅减少" + dh.getHolderNum() + "%" + splitor;
		}
		// --短线--
		if (dh.getShooting3() > 0) {
			s += "底部融资余额飙升-确认是主力在融资买入?(短线1)" + splitor;
		}
		if (dh.getShooting5() > 0) {
			s += "股价极速拉升:妖股?龙抬头?(短线2)" + splitor;
		}
		return s;
	}

	public static String getQif(CodeBaseModel2 p1) {
		String s = "";
		if (p1.getDibuQixing() > 0) {
			s = "大旗形";
		} else if (p1.getDibuQixing2() > 0) {
			s = "小旗形";
		}
		if (p1.getZyxing() > 0) {
			s += "-中阳十字星";
		}
		return s;
	}
}
