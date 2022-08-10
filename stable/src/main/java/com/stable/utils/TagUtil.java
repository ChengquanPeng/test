package com.stable.utils;

import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.stable.constant.Constant;
import com.stable.service.model.WebModelService;
import com.stable.vo.bus.CodeBaseModel2;
import com.stable.vo.bus.CodeConcept;

public class TagUtil {

	public static boolean stockRange(boolean isSamll, CodeBaseModel2 newOne) {
		// 1.排除的,
		if (newOne.getPls() == 2) {
			return false;
		}
		// 2大票 直接false
		if (!TagUtil.isDibuSmall(isSamll, newOne)) {
			// 人工的需要check||底部优质大票||一些底部小涨-stable0有业绩的小票(热点票)
			if (newOne.getPls() == 1 || (newOne.getZfjjup() >= 4 && newOne.getFinOK() >= 1 && isSamll
					&& newOne.getHolderNumP5() > 45.0)) {
			} else {
				return false;
			}
		}
		return true;
	}

	// 底部2-1
	public static boolean isDibu21(CodeBaseModel2 cbm) {
		return cbm.getZfjjup() >= 2 && cbm.getZfjjupStable() >= 1;
	}

	// 底部1-1
	public static boolean isDibu11(CodeBaseModel2 cbm) {
		return cbm.getZfjjup() >= 1 && cbm.getZfjjupStable() >= 1;
	}

	// 底部-小票
	public static boolean isDibuSmall(boolean isSmallStock, CodeBaseModel2 cbm) {
		return isSmallStock && isDibu21(cbm);
	}

	// 底部-大票-基本面OK
	public static boolean isDibuOKBig(boolean isSmallStock, CodeBaseModel2 cbm) {
		return !isSmallStock && isDibu11(cbm) && isFinNomarl(cbm);
	}

	// 业绩正常/普通
	public static boolean isFinNomarl(CodeBaseModel2 cbm) {
		return cbm.getFinOK() >= 1 && cbm.getBousOK() >= 1;
	}

	// 业绩不错-连续增长或者暴涨
	public static boolean isFinPerfect(CodeBaseModel2 cbm) {
		return isFinNomarl(cbm) && (cbm.getFinDbl() > 0 || cbm.getFinanceInc() > 0);
	}

	// 概念
	public static String getGn(List<CodeConcept> l) {
		StringBuffer sb = new StringBuffer("");
		if (l != null) {
			for (CodeConcept cc : l) {
				if (StringUtils.isNotBlank(cc.getConceptName())) {
					if (!cc.getConceptName().equals(cc.getAliasCode())) {
						sb.append(cc.getConceptName()).append(",");
					}
				}
			}
		}
		return sb.toString();
	}

	// 标签
	public static String getTag(CodeBaseModel2 cbm) {
		String you = "";
		if (cbm.getShooting7() > 0) {
			you = "[小底]";
		} else {
			you = "[普]";
		}
		if (cbm.getShooting11() > 0) {
			you += "[大]";
		}
		if (isFinPerfect(cbm)) {
			you += "[绩]";
		}
		return you;
	}

	public static String getSystemPoint(CodeBaseModel2 dh, String splitor) {
		String s = "";
		// --中长--
		if (dh.getShooting6661() > 0 || dh.getShooting6662() > 0) {
			if (dh.getShooting6661() > 0) {
				s += "底部小票模式-大宗" + splitor;
			}
			if (dh.getShooting6662() > 0) {
				s += "底部小票模式-减持" + splitor;
			}

		} else if (dh.getShooting7() > 0) {
			s += "底部小票模式" + splitor;
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
			s += "底部融资余额飙升-主力融资买入?" + splitor;
		}
		if (dh.getShooting5() > 0) {
			s += "股价极速拉升:妖股?龙抬头?(短线2)" + splitor;
		}
		return s;
	}

	public static String getXiPan(CodeBaseModel2 p1) {
		String s = "";
		if (p1.getDibuQixing() > 0) {
			s = "大7";
		} else if (p1.getDibuQixing2() > 0) {
			s = "小7";
		}
		if (p1.getZyxing() > 0) {
			s += "10";
		}
		return s;
	}

	public static String baseInfo(CodeBaseModel2 dh) {
		StringBuffer sb5 = new StringBuffer("");
		if (dh.getZfjjup() > 0) {
			sb5.append(dh.getZfjjup());
			if (dh.getZfjjupStable() > 0) {
				sb5.append("<font color='red'>/stable").append(dh.getZfjjupStable()).append("</font>");
			}
			sb5.append("年未大涨");
		}
		if (dh.getBousOK() > 0) {
			sb5.append(",连续" + dh.getBousOK() + "年分红");
		}
		if (dh.getFinDbl() > 0) {
			sb5.append("<font color='red'>,业绩暴涨</font>");
		}
		if (dh.getFinOK() > 0) {
			sb5.append(",连续" + dh.getFinOK() + "年业绩盈利");
			if (dh.getFinanceInc() > 0) {
				sb5.append(",连续" + dh.getFinanceInc() + "年增长");
			}
			sb5.append(",市盈率ttm:").append(dh.getPettm());
		}
		return sb5.toString();
	}

	public static String gameInfo(CodeBaseModel2 dh, boolean trymsg) {
		// 博弈-行情指标
		StringBuffer sb5 = new StringBuffer();

		sb5.append("<font color='red'>");
		sb5.append(TagUtil.getSystemPoint(dh, Constant.HTML_LINE));
		sb5.append("</font>");

		if (dh.getCompnayType() == 1) {
			sb5.append("<font color='green'>");
			sb5.append("国资,");
			sb5.append("</font>");
		}
		// 基本面-筹码
		sb5.append("流通:").append(dh.getMkv()).append("亿,");
		sb5.append("除5%活筹:").append(dh.getActMkv()).append("亿,");
		sb5.append("%5股东:").append(dh.getHolderNumP5()).append("%");
		sb5.append(",股东人数:").append(CurrencyUitl.covertToString(dh.getLastNum()));
		sb5.append(",人均持股:").append(CurrencyUitl.covertToString(dh.getAvgNum()));
		sb5.append(",变化:").append(dh.getHolderNum()).append("%");
		sb5.append(Constant.HTML_LINE).append(Constant.HTML_LINE);
		// 行情-财务
		if (dh.getZfjjup() > 0 || dh.getBousOK() > 0 || dh.getFinOK() > 0) {
			sb5.append(TagUtil.baseInfo(dh));
			sb5.append(Constant.HTML_LINE).append(Constant.HTML_LINE);
		}
		// 增发
		if (dh.getZfStatus() == 1 || dh.getZfStatus() == 2 || dh.getZflastOkDate() > 0 || dh.getZfjj() == 1) {
			if (dh.getZfStatus() == 1 || dh.getZfStatus() == 2) {
				if (dh.getZfStatus() == 1) {
					sb5.append("<font color='red'>");
				} else {
					sb5.append("<font color='green'>");
				}
				sb5.append("增发进度" + ":" + dh.getZfStatusDesc());
				sb5.append("</font>");

				if (dh.getZfStatus() == 1) {
					if (dh.getZfYjAmt() > 0) {
						if (trymsg) {
							sb5.append(",预增发金额:xx亿");
						} else {
							sb5.append(",预增发金额:").append(CurrencyUitl.covertToString(dh.getZfYjAmt()));
						}
					}
				} else {

					if (trymsg) {
						if (StringUtils.isNotBlank(dh.getZfAmt())) {
							sb5.append(",实增发金额:xx亿");
						} else if (dh.getZfYjAmt() > 0) {
							sb5.append(",增发金额:xx亿");
						}
						sb5.append(",增发价格:xx元");
					} else {
						if (StringUtils.isNotBlank(dh.getZfAmt())) {
							sb5.append(",实增发金额:").append(dh.getZfAmt());
						} else if (dh.getZfYjAmt() > 0) {
							sb5.append(",增发金额:").append(CurrencyUitl.covertToString(dh.getZfYjAmt()));
						}
						sb5.append(",增发价格:").append(dh.getZfPrice());
					}
				}

			}
			// 最近一次增发
			if (dh.getZflastOkDate() > 0) {
				if (trymsg) {
					sb5.append(",实施日期:yyyy-mm-dd,");
				} else {
					sb5.append(",实施日期:").append(dh.getZflastOkDate()).append(",");
				}
				if (dh.getZfself() == 1) {
					sb5.append("<font color='green'>底部增发</font>,");
				}
				if (dh.getZfPriceLow() > 0) {
					sb5.append("<font color='red'>低于增发价:").append(dh.getZfPriceLow()).append("%</font>,");
				}
				if (dh.getGsz() == 1) {
					sb5.append("3年内有高送转,");
				}
				if (dh.getZfObjType() == 1) {
					sb5.append("6个月,");
				} else if (dh.getZfObjType() == 2) {
					sb5.append("混合(6月+大股东),");
				} else if (dh.getZfObjType() == 3) {
					sb5.append("大股东,");
				} else if (dh.getZfObjType() == 4) {
					sb5.append("其他,");
				}
			}
			// 解禁
			if (dh.getZfjj() == 1) {
				if (trymsg) {
					sb5.append("增发解禁(yyyy-mm-dd)");
				} else {
					sb5.append("增发解禁(" + dh.getZfjjDate() + ")");
				}

			}
			sb5.append(Constant.HTML_LINE).append(Constant.HTML_LINE);
		}
		// 大宗
		if (dh.getDzjy365d() > 0) {
			sb5.append("1年内大宗:").append(CurrencyUitl.covertToString(dh.getDzjy365d() * WebModelService.WAN))
					.append("(占比:").append(dh.getDzjyp365d()).append("%,均价:").append(dh.getDzjyAvgPrice()).append(")");
			if (dh.getTagDzPriceLow() > 0) {
				sb5.append(",低于均价:").append(dh.getTagDzPriceLow()).append("%");
			}
			if (dh.getDzjy60d() > 0) {
				sb5.append(",2月:").append(CurrencyUitl.covertToString(dh.getDzjy60d() * WebModelService.WAN))
						.append("(").append(dh.getDzjyp60d()).append("%)");
			}
		}
		// 减持
		sb5.append(Constant.HTML_LINE).append(Constant.HTML_LINE);
		if (dh.getReducZb() > 0 || dh.getReducYg() > 0) {
			sb5.append("1年内减持:").append(dh.getReduceTims()).append("次,").append(dh.getReducYg()).append("亿股,流通占比:")
					.append(dh.getReducZb()).append("%");
			if (dh.getReduceLastPlanDate() > 0) {
				sb5.append(",最新计划日期:").append(dh.getReduceLastPlanDate());
			}
		}
		return sb5.toString();
	}
}
