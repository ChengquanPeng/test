package com.stable.vo.bus;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Setter
@Getter
@ToString
public class FinanceBaseInfoPage extends EsBase {
	private static final long serialVersionUID = 1L;
	private String id;
	private String code;

	private int date; // 报告日期
	private Integer annDate = 0; // 公告日期
	private int year;// 年
	private int quarter;// 季度
	private long yyzsr; // 营业总收入
	private long gsjlr; // 归属净利润
	private long kfjlr; // 扣非净利润
	private double yyzsrtbzz; // 营业总收入同比增长(%)
	private double gsjlrtbzz; // 归属净利润同比增长(%)
	private double kfjlrtbzz; // 扣非净利润同比增长(%)
	private double jyxjlce; // 经营现金流差额
	private double mgjyxjl; // 每股经营现金流
	private double jqjzcsyl; // 加权净资产收益率(%) -加权净资产收益率=当期净利润/当期加权平均净资产
	private double syldjd;// 加权净资产收益率+单季度百分比
//	private double tbjzcsyl; // 摊薄净资产收益率(%) -摊薄净资产收益率=报告期净利润/期末净资产
	private double mll; // 毛利率
	private double zcfzl; // 资产负债率

	// 资产负债表
	private double goodWill; // 商誉
	private double intangibleAsset; // 无形资产
	private double sumLasset; // 流动资产合计
	private double sumAsset; // 总资产
	private double inventory; // 存货资产
	private double sumDebt; // 负债合计
	private double sumDebtLd; // 流动负债
	private double netAsset; // 净资产
	private double monetaryFund;// 货币资金
	private double tradeFinassetNotfvtpl; // 交易性金融资产
	private double stborrow;// 短期借款
	private double ltborrow;// 长期借款
	private double accountrec;// 应收账款（是否自己贴钱在干活，同行业比较）
	private double accountPay;// 应付账款:欠供应/合作商的钱，如果现金流解决不了应付账款，净资产低于应付账款就会破产清算
	private double retaineDearning;// 未分配利润
	private double interestPay;// 应付利息:如果较高，公司在大量有息借钱，关联到货币资金和未分配利润。如果货币资金和未分配利润较高，明明有钱为什么借钱，
	private double djdKf;// 单季度扣非
	private double djdKfTbzz;// 单季度扣非增长
	// 计算字段
//	
//	private double goodWillRatioGsjlr; // 商誉净资产占比（净利润）
	private double goodWillRatioNetAsset; // 商誉净利润占比（净资产）
	private double inventoryRatio; // 存货净资产占比 // 存货资产占比（净资产）
	private int fundNotOk = 1;// 是否资金紧张:资金紧张: 货币资金-流动负债, <=0
	private int fundNotOk2 = 0;// interestPay
	private int fundNotOk3 = 0;// 财务疑似三高
	private int bustUpRisks = 1;// 是否有破产风险：应付账款:欠供应/合作商的钱，如果现金流解决不了应付账款，净资产低于应付账款就会破产清算
	private double taxPayable;// 应交税费
//	货币资金：在会计科目上，主要指库存现金、银行存款和其他货币资金三者。
//		库存现金：指存放于企业财会部门、由出纳人员经管的货币。
//		银行存款：指企业存入银行或其他金融机构的各种款项。
//		其他货币资金：指企业的银行汇票存款、银行本票存款、信用卡存款、信用证保证金存款、存出投资款、外埠存款等其他货币资金。
//	实务中主要核算各项保证金和存入支付宝等第三方支付平台的款项。
//	
//	现金及现金等价物对流动性要求更高，需是3个月内可以使用的，所以，需要在在货币资金的基础上剔除一些受限资产。

	private double accountrecRatio;// 应收款占比：accountrec应收账款/sumLasset流动资产合计
	private boolean isDataOk = false;

	public FinanceBaseInfoPage() {

	}

	public FinanceBaseInfoPage(String code, int date) {
		this.code = code;
		this.date = date;
		String datestr = String.valueOf(date);
		this.year = Integer.valueOf(datestr.substring(0, 4));
		int m = Integer.valueOf(datestr.substring(4, 6));
		if (m == 12) {
			this.quarter = 4;
		} else if (m == 9) {
			this.quarter = 3;
		} else if (m == 6) {
			this.quarter = 2;
		} else if (m == 3) {
			this.quarter = 1;
		} else {
			this.quarter = 0;
		}
		this.id = this.code + "_" + this.date;
	}

}
