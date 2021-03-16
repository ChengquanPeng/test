package com.stable.vo.http.req;

import lombok.Data;

@Data
public class ModelManulReq {
	String code;
	int pls;
	int timemonth;

	private int lstmt = 0;// 上次更新日期
	private String buyRea; // 买入理由
	private String soldRea;// 卖出理由
	private int profit = 0;// 利润空间
	private int dzz = 0;// 是否在定增中
	private int buyType;
	private int dzOK = 0;// 定增情况，0差，1一般，2好

	private int yj = 0; // 1.持续增长-扣非(主营）,2.持续增长-归属,3.波动不大，较平稳,4.下降趋势，但是不亏,5.扣非亏损，至少归属不亏
	private int hybk = 0; // 1.熟悉的行业（自己了解的），2.民生行业，3.主流/热点行业（国家政策:新能源，芯片，光伏等），4.主流行业产业链/上下游，5.高大上科技行业（无人xx，区块链），看不见摸不着的行业，科技技术更新太快且烧钱,公司容易死,
	private int mainBusi = 0;// 主营业务 集中优先（拳头产品），0差，1一般，2好
	private int dgdjc = 0;// 是否有大股东减持
	private int qujz = 0;// 股权是否集中，0差，1一般，2好
	private int legalOk = 0;// 是否有违法情况，0差，1一般，2好
	private int glprojeckOk = 0;// 项目情况（概念），0差，1一般，2好
	private int mainChipOk = 0;// 增发筹码情况，0差，1一般，2好
	private int bonus = 0;// 分红情况，0差，1一般，2好
	private int jyxjlceOk = 0;// 现金流情况:0差，1一般，2好
	private int acRec = 0;// 应收账款情况，0差，1一般，2好
	private int acPay = 0;// 应付账款情况，0差，1一般，2好

	private int dzjyOk = 0;// 大宗交易情况，0差，1一般，2好
	private int gdrsOk = 0; // 股东人数OK
	private int goodWillOk = 0; // 商誉情况
}
