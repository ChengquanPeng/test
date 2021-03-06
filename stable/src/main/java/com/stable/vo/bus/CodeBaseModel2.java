package com.stable.vo.bus;

import java.beans.Transient;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Document(indexName = "code_base_model2")
public class CodeBaseModel2 extends EsBase {
	private static final long serialVersionUID = 1L;

	@Id
	private String id;
	@Field(type = FieldType.Text)
	private String code;
	@Field(type = FieldType.Integer)
	protected int date;

	@Field(type = FieldType.Integer)
	private int monitor;// 是否监听

	@Field(type = FieldType.Integer)
	protected int currYear;
	@Field(type = FieldType.Integer)
	protected int currQuarter;

	@Field(type = FieldType.Integer)
	private int compnayType;// 国资
	@Field(type = FieldType.Integer)
	private int finOK;// 5年业绩OK
	@Field(type = FieldType.Integer)
	private int bousOK;// 5年分红OK(至少4年分红)

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
	@Field(type = FieldType.Integer)
	protected int baseGreen;// 基本面：绿色
	@Field(type = FieldType.Text)
	private String baseGreenDesc;

	// 加权净资产收益率
	@Field(type = FieldType.Double)
	private double syl;// 加权净资产收益率
	@Field(type = FieldType.Double)
	private double syldjd;// 加权净资产收益率+单季度百分比
	@Field(type = FieldType.Double)
	private double sylttm;// 加权净资产收益率+单季度百分比
	@Field(type = FieldType.Integer)
	private int sylType;// 收益率类型:1:自身收益率增长,2: 年收益率超过5.0%*4=20%,4:同时包含12

	// 增发(最近的增发，包含正在增发）
	@Field(type = FieldType.Integer)
	private int zfStatus;// 增发状态（近1年）: 0无增发，1增发中，2增发完成，3，增发终止
	@Field(type = FieldType.Text)
	private String zfStatusDesc;// 增发进度

	// 已完成的增发
	@Field(type = FieldType.Integer)
	private int zflastOkDate = 0;// 最后已实施时间
	@Field(type = FieldType.Integer)
	private int zfself = 0;// 自己人在增发
	@Field(type = FieldType.Integer)
	private int zfbuy = 0;// 是否购买资产
	@Field(type = FieldType.Integer)
	private int susZfBoss = 0; // 增发博弈(自己人+未涨)
	@Field(type = FieldType.Integer)
	private int gsz = 0;// 三年高送转
	@Field(type = FieldType.Integer)
	private int zfObjType = 0;// 增发类型:1：6个月，2：混合，3大股东

	// 增发解禁
	@Field(type = FieldType.Integer)
	private int zfjjDate = 0;// 最近的增发解禁时间
	@Field(type = FieldType.Integer)
	private int zfjj = 0; // 增发解禁
	@Field(type = FieldType.Integer)
	private int zfjjup = 0; // 小票未大涨
	@Field(type = FieldType.Integer)
	private int zfPriceLow = 0; // 低于增发价
	@Field(type = FieldType.Integer)
	private int zfPriceLowNotice = 0; // 低于增发价
	@Field(type = FieldType.Integer)
	private int dzjyRct = 0; // 最近半年大宗交易频繁

	// 交易面
	@Field(type = FieldType.Integer)
	private int susBigBoss; // 疑似大牛
	@Field(type = FieldType.Integer)
	private int susWhiteHors; // 疑似白马
	// 短线
	@Field(type = FieldType.Integer)
	private int sortMode6 = 0;// 符合短线 //短线模型6(前期3-50%吸筹，深度回踩突然涨停后再2-5个交易日回踩拉起,涨停日不放量，超过涨停价格后买入，买入2内未大幅拉升放弃
	@Field(type = FieldType.Integer)
	private int sortMode7 = 0;// 突破箱体震荡 --//0未知，1待确认，2确认，3，不符合
	@Field(type = FieldType.Integer)
	private int smallModel = 0;// 0:无，1：普通，2，增发，3，大宗，4，增发+大宗
	@Field(type = FieldType.Integer)
	private int sortChips = 0;// 收集筹码的短线
	@Field(type = FieldType.Integer)
	private int sortChipsNotice = 0;// 收集筹码的短线-通知
	// 人工确定

	// 股东人数
	@Field(type = FieldType.Double)
	private double holderNumP5; // 十大股东：5%股东占比
	@Field(type = FieldType.Double)
	private double holderNumT3; // 十大股东：Top3%股东占比
	@Field(type = FieldType.Double)
	private double holderNum;
	@Field(type = FieldType.Integer)
	private int holderDate;

	// ==== 筹码博弈 ====
	// 1.是否可买
	@Field(type = FieldType.Long)
	private int pls = 0;// 0不确定，1在池子，2不在池子
	@Field(type = FieldType.Long)
	private long plst = 0;// 时间
	@Field(type = FieldType.Integer)
	private int lstmt = 0;// 上次更新日期
	@Field(type = FieldType.Integer)
	private int recentPriceYear = 0;// 是否横盘多年

	@Field(type = FieldType.Text)
	private String buyRea; // 买入理由
	@Field(type = FieldType.Text)
	private String soldRea;// 卖出理由
	@Field(type = FieldType.Integer)
	private int profit = 0;// 利润空间
	@Field(type = FieldType.Integer)
	private int dzz = 0;// 是否在定增中
	@Field(type = FieldType.Integer)
	private int buyType; // 1.增发：纯大股东，2.自己人：大股东混合，3，自己人：外部，4.不是自己人，
	// 5.重组（重组:// 公告后一般会拉升一波，确定拉升那波人数是否减少（减少说明主力在进货），增发完成会立即拉升,主力筹码无锁定期，且增发对象都是大股东）
	// 6.大宗交易异动

	@Field(type = FieldType.Integer)
	private int dzOK = 0;// 定增情况，0差，1一般，2好

	// 3.基本面排雷清单/优选
	@Field(type = FieldType.Double)
	private double mkv;// 流通市值
	@Field(type = FieldType.Integer)
	private int yj = 0; // 1.持续增长-扣非(主营）,2.持续增长-归属,3.波动不大，较平稳,4.下降趋势，但是不亏,5.扣非亏损，至少归属不亏
	@Field(type = FieldType.Integer)
	private int hybk = 0; // 1.熟悉的行业（自己了解的），2.民生行业，3.主流/热点行业（国家政策:新能源，芯片，光伏等），4.主流行业产业链/上下游，5.高大上科技行业（无人xx，区块链），看不见摸不着的行业，科技技术更新太快且烧钱,公司容易死,
							// 长线不推荐
	@Field(type = FieldType.Integer)
	private int mainBusi = 0;// 主营业务 集中优先（拳头产品），0差，1一般，2好
	@Field(type = FieldType.Integer)
	private int dgdjc = 0;// 是否有大股东减持
	@Field(type = FieldType.Integer)
	private int qujz = 0;// 股权是否集中，0差，1一般，2好

	@Field(type = FieldType.Integer)
	private int legalOk = 0;// 是否有违法情况，0差，1一般，2好
	@Field(type = FieldType.Integer)
	private int glprojeckOk = 0;// 项目情况（概念），0差，1一般，2好
	@Field(type = FieldType.Integer)
	private int mainChipOk = 0;// 增发筹码情况，0差，1一般，2好

	@Field(type = FieldType.Integer)
	private int bonus = 0;// 分红情况，0差，1一般，2好
	@Field(type = FieldType.Integer)
	private int jyxjlceOk = 0;// 现金流情况:0差，1一般，2好
	@Field(type = FieldType.Integer)
	private int acRec = 0;// 应收账款情况，0差，1一般，2好
	@Field(type = FieldType.Integer)
	private int acPay = 0;// 应付账款情况，0差，1一般，2好

	@Field(type = FieldType.Integer)
	private int dzjyOk = 0;// 大宗交易情况，0差，1一般，2好
	@Field(type = FieldType.Integer)
	private int gdrsOk = 0; // 股东人数OK
	@Field(type = FieldType.Integer)
	private int jjOk = 0;// 解禁情况，0差，1一般，2好
	@Field(type = FieldType.Integer)
	private int goodWillOk = 0; // 商誉情况

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

	// 监听
	@Field(type = FieldType.Double)
	private int listenerGg; // 监听-公告

	@Transient
	public String getKeyString() {
		StringBuffer sb = new StringBuffer();
		sb.append(currYear).append("|");
		sb.append(currQuarter).append("|");
		return sb.toString();
	}
}
