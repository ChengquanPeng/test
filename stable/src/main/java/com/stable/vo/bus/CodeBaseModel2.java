package com.stable.vo.bus;

import java.beans.Transient;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Setter
@Getter
@ToString
@Document(indexName = "code_base_model2")
public class CodeBaseModel2 extends EsBase {
	private static final long serialVersionUID = 1L;
	@Field(type = FieldType.Integer)
	private int shooting1 = 0;// 行情指标1：小票，底部大宗超流动5%
	@Field(type = FieldType.Integer)
	private int shooting2 = 0;// 行情指标2：大票，底部定增超过50亿（越大越好），且证监会通过-之前有明显底部拿筹痕迹-涨停。
	@Field(type = FieldType.Integer)
	private int shooting3 = 0;// 行情指标3：融资票
	@Field(type = FieldType.Integer)
	private int shooting30 = 0;// 融资过期日期
	@Field(type = FieldType.Double)
	private double rzrqRate = 0;// 融资增长比例
	@Field(type = FieldType.Integer)
	private int shooting4 = 0;// 行情指标4：股东人数底部大幅减少
	@Field(type = FieldType.Integer)
	private int shooting5 = 0;// 行情指标5：短线拉升小平台。
	@Field(type = FieldType.Integer)
	private int shooting6 = 0;// 底部小票-减持
	@Field(type = FieldType.Integer)
	private int shooting7 = 0;// 底部优质小票
	@Field(type = FieldType.Integer)
	private int shooting8 = 0;// 行情指标8：底部横盘3-4年以上的增发-小票
	@Field(type = FieldType.Integer)
	private int shooting9 = 0;// 行情指标9：底部未涨小票-做短线用
	@Field(type = FieldType.Integer)
	private int shooting11 = 0;// 底部未涨大票
	@Field(type = FieldType.Integer)
	private int shooting6661 = 0;// 小底-大宗

	// 技术面
	@Field(type = FieldType.Integer)
	private int shooting10 = 0;// 行情指标10：PRE/一年新高
	@Field(type = FieldType.Integer)
	private int shootingw = 0;// 行情指标w:横盘w

	@Field(type = FieldType.Integer)
	private int qb = 0;// 处于起爆
	@Field(type = FieldType.Long)
	private int qixing = 0;// 旗形
	@Field(type = FieldType.Text)
	private String qixingStr;// 旗形恶心走势
	@Field(type = FieldType.Integer)
	private int dibuQixing = 0;// 旗形
	@Field(type = FieldType.Integer)
	private int dibuQixing2 = 0;// 旗形2
	@Field(type = FieldType.Long)
	private int zyxing = 0;// 中阳带星
	@Field(type = FieldType.Integer)
	private int zyxingt = 0;// 中阳带星(T)
	@Field(type = FieldType.Integer)
	private int dibuQixingV2 = 0;// 旗形
	@Field(type = FieldType.Integer)
	private int dibuQixingV22 = 0;// 旗形2
	@Field(type = FieldType.Integer)
	private int prd1 = 0;// 产品1
	@Field(type = FieldType.Text)
	private String jsHist;// 技术面历史

	// 财务业绩
	@Field(type = FieldType.Integer)
	private int bousOK;// 5年分红OK(至少4年分红)
	@Field(type = FieldType.Integer)
	private int finOK;// 5年业绩OK
	@Field(type = FieldType.Integer)
	private int financeInc;// 业绩连续增长
	@Field(type = FieldType.Integer)
	private int susBigBoss; // // 基本面疑似大牛
	@Field(type = FieldType.Double)
	private double finDbl;// 暴涨翻倍

	@Field(type = FieldType.Integer)
	private int moni = 0;// 是否监听

	@Field(type = FieldType.Double)
	private double reducZb;// 减持占比

	// Step.1.市值大小，股价K线形态是否横盘多年，且常年分红
	@Field(type = FieldType.Double)
	private double mkv;// 流通市值
	@Field(type = FieldType.Double)
	private double actMkv;// 除去5%以上股东以外的市值

	@Field(type = FieldType.Integer)
	private int zfjjup = 0; // 票未涨
	@Field(type = FieldType.Integer)
	private int zfjjupStable = 0; // 票未涨
	// Step.2.公司所处行业，大股股东股权结构和持股比例，增发和大宗情况，股东人数增长，基金是否参与。
	// 股东人数
	@Field(type = FieldType.Double)
	private double holderNumP5; // 十大股东：5%股东占比
	@Field(type = FieldType.Double)
	private double holderNumT3; // 十大股东：Top3%股东占比
	@Field(type = FieldType.Double)
	private double holderNum;// 股东人数变化
	@Field(type = FieldType.Long)
	private long lastNum;// 最新股东人数
	@Field(type = FieldType.Long)
	private long avgNum;// 除开5%股东的人均流通持股
	@Field(type = FieldType.Integer)
	private int holderDate;
	@Field(type = FieldType.Integer)
	private int dzjyRct = 0; // 最近1年大宗交易频繁
	@Field(type = FieldType.Double)
	private double dzjyAvgPrice = 0; // 最近1年大宗交易均价
	@Field(type = FieldType.Double)
	private double dzjy60d = 0.0;// 2个月大宗
	@Field(type = FieldType.Double)
	private double dzjy365d = 0.0;// 1年大宗
	@Field(type = FieldType.Double)
	private double dzjyp60d;// 占比-1年
	@Field(type = FieldType.Double)
	private double dzjyp365d;// 占比-60天
	@Field(type = FieldType.Integer)
	private int tagDzPriceLow = 0;// 低于大宗均价
	// 增发和大宗情况
	// 增发(最近的增发，包含正在增发）
	@Field(type = FieldType.Integer)
	private int zfStatus;// 增发状态（近1年）: 0无增发，1增发中，2增发完成，3，增发终止
	@Field(type = FieldType.Double)
	private double zfPrice = 0; // 增发价格
	@Field(type = FieldType.Long)
	private long zfYjAmt = 0;// 增发预计金额
	@Field(type = FieldType.Text)
	private String zfAmt;// 实际募资净额：3.31亿元
	@Field(type = FieldType.Text)
	private String zfStatusDesc;// 增发进度
	// 已完成的增发
	@Field(type = FieldType.Integer)
	private int zflastOkDate = 0;// 最后已实施时间
	@Field(type = FieldType.Integer)
	private int zfself = 0;// 自己人在增发
	@Field(type = FieldType.Integer)
	private int gsz = 0;// 三年高送转
	@Field(type = FieldType.Integer)
	private int zfObjType = 0;// 增发类型:1:6个月; 2:混合;3:大股东,4:其他
	@Field(type = FieldType.Integer)
	private int zfPriceLow = 0; // 低于增发价
	// 增发解禁
	@Field(type = FieldType.Integer)
	private int zfjjDate = 0;// 最近的增发解禁时间
	@Field(type = FieldType.Integer)
	private int zfjj = 0; // 增发解禁

	@Field(type = FieldType.Integer)
	private int compnayType;// 国资

	// 标签
	@Field(type = FieldType.Integer)
	private int tagSmallAndBeatf;// 小而美
	@Field(type = FieldType.Integer)
	protected int tagHighZyChance;// 高质押机会
	@Field(type = FieldType.Integer)
	private int sortChips = 0;// 吸筹-收集筹码短线
	@Field(type = FieldType.Integer)
	private int susWhiteHors; // // 交易面疑似白马

	@Id
	private String id;
	@Field(type = FieldType.Text)
	private String code;
	@Field(type = FieldType.Integer)
	protected int date;

	@Field(type = FieldType.Integer)
	protected int currYear;
	@Field(type = FieldType.Integer)
	protected int currQuarter;

	@Field(type = FieldType.Integer)
	protected int baseRed;// 基本面：红色
	@Field(type = FieldType.Text)
	private String baseRedDesc;
	@Field(type = FieldType.Integer)
	protected int baseYellow;// 基本面：黄色
	@Field(type = FieldType.Text)
	private String baseYellowDesc;
	@Field(type = FieldType.Integer)
	protected int baseBlue;// 基本面：蓝色
	@Field(type = FieldType.Text)
	private String baseBlueDesc;

	// ===人工===
	@Field(type = FieldType.Long)
	private int pls = 0;// 0不确定，1确定，2排除
	@Field(type = FieldType.Long)
	private long plst = 0;// 时间
	@Field(type = FieldType.Text)
	private String buyRea; // 买入理由

	// === 财务 ===
	@Field(type = FieldType.Long)
	private long gsjlr; // 归属净利润
	@Field(type = FieldType.Double)
	private double goodWill; // 商誉
	@Field(type = FieldType.Double)
	private double goodWillRatioGsjlr; // 商誉净利润占比（净利润）
	@Field(type = FieldType.Double)
	private double goodWillRatioNetAsset; // 商誉净资产占比（净利润）
	@Field(type = FieldType.Double)
	private double netAsset; // 净资产
	@Field(type = FieldType.Double)
	private double zcfzl; // 资产负债率
	@Field(type = FieldType.Double)
	private double pettm = 0.0;// 市盈率ttm

	@Transient
	public String getKeyString() {
		StringBuffer sb = new StringBuffer();
		sb.append(currYear).append("|");
		sb.append(currQuarter).append("|");
		return sb.toString();
	}
}
