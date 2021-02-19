package com.stable.vo.bus;

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
@Document(indexName = "finance_base_info2")
public class FinanceBaseInfo extends EsBase {
	private static final long serialVersionUID = 2305594280230790359L;
	@Id
	private String id;
	@Field(type = FieldType.Text)
	private String code;
	@Field(type = FieldType.Integer)
	private int date; // 报告日期
	@Field(type = FieldType.Integer)
	private Integer annDate = 0; // 公告日期
	@Field(type = FieldType.Integer)
	private int year;// 年
	@Field(type = FieldType.Integer)
	private int quarter;// 季度

	@Field(type = FieldType.Long)
	private long yyzsr; // 营业总收入
	@Field(type = FieldType.Long)
	private long gsjlr; // 归属净利润
	@Field(type = FieldType.Long)
	private long kfjlr; // 扣非净利润

	@Field(type = FieldType.Double)
	private double yyzsrtbzz; // 营业总收入同比增长(%)
	@Field(type = FieldType.Double)
	private double gsjlrtbzz; // 归属净利润同比增长(%)
	@Field(type = FieldType.Double)
	private double kfjlrtbzz; // 扣非净利润同比增长(%)
	@Field(type = FieldType.Double)
	private double jyxjlce; // 经营现金流差额
	@Field(type = FieldType.Double)
	private double mgjyxjl; // 每股经营现金流

	@Field(type = FieldType.Double)
	private double jqjzcsyl; // 加权净资产收益率(%) -加权净资产收益率=当期净利润/当期加权平均净资产
	@Field(type = FieldType.Double)
	private double syldjd;// 加权净资产收益率+单季度百分比
	@Field(type = FieldType.Double)
	private double tbjzcsyl; // 摊薄净资产收益率(%) -摊薄净资产收益率=报告期净利润/期末净资产

	@Field(type = FieldType.Double)
	private double mll; // 毛利率
	@Field(type = FieldType.Double)
	private double zcfzl; // 资产负债率

	// 资产负债表
	@Field(type = FieldType.Double)
	private double goodWill; // 商誉
	@Field(type = FieldType.Double)
	private double sumLasset; // 流动资产合计
	@Field(type = FieldType.Double)
	private double sumAsset; // 总资产
	@Field(type = FieldType.Double)
	private double inventory; // 存货资产
	@Field(type = FieldType.Double)
	private double sumDebt; // 负债合计
	@Field(type = FieldType.Double)
	private double sumDebtLd; // 流动负债总计
	@Field(type = FieldType.Double)
	private double netAsset; // 净资产
	@Field(type = FieldType.Double)
	private double monetaryFund;// 货币资金
	@Field(type = FieldType.Double)
	private double tradeFinassetNotfvtpl; // 交易性金融资产
	@Field(type = FieldType.Double)
	private double stborrow;// 短期借款
	@Field(type = FieldType.Double)
	private double ltborrow;// 长期借款
	@Field(type = FieldType.Double)
	private double accountrec;// 应收账款（是否自己贴钱在干活，同行业比较）
	@Field(type = FieldType.Double)
	private double accountPay;// 应付账款:欠供应/合作商的钱，如果现金流解决不了应付账款，净资产低于应付账款就会破产清算
	@Field(type = FieldType.Double)
	private double retaineDearning;// 未分配利润
	@Field(type = FieldType.Double)
	private double interestPay;// 应付利息:如果较高，公司在大量有息借钱，关联到货币资金和未分配利润。如果货币资金和未分配利润较高，明明有钱为什么借钱，

	// 计算字段
//	@Field(type = FieldType.Double)
//	private double goodWillRatioGsjlr; // 商誉净资产占比（净利润）
	@Field(type = FieldType.Double)
	private double goodWillRatioNetAsset; // 商誉净利润占比（净资产）
	@Field(type = FieldType.Double)
	private double inventoryRatio; // 存货净资产占比 // 存货资产占比（净资产）
	@Field(type = FieldType.Double)
	private int fundNotOk = 1;// 是否资金紧张:资金紧张: 货币资金-流动负债, <=0
	@Field(type = FieldType.Double)
	private int fundNotOk2 = 0;// interestPay
	@Field(type = FieldType.Double)
	private int fundNotOk3 = 0;// 疑似财务三高
	@Field(type = FieldType.Double)
	private int bustUpRisks = 1;// 是否有破产风险：应付账款:欠供应/合作商的钱，如果现金流解决不了应付账款，净资产低于应付账款就会破产清算

//	货币资金：在会计科目上，主要指库存现金、银行存款和其他货币资金三者。
//		库存现金：指存放于企业财会部门、由出纳人员经管的货币。
//		银行存款：指企业存入银行或其他金融机构的各种款项。
//		其他货币资金：指企业的银行汇票存款、银行本票存款、信用卡存款、信用证保证金存款、存出投资款、外埠存款等其他货币资金。
//	实务中主要核算各项保证金和存入支付宝等第三方支付平台的款项。
//	
//	现金及现金等价物对流动性要求更高，需是3个月内可以使用的，所以，需要在在货币资金的基础上剔除一些受限资产。

	@Field(type = FieldType.Double)
	private double accountrecRatio;// 应收款占比：accountrec应收账款/sumLasset流动资产合计

	// 第二计算字段
//	1.净资产小于0
//	2.流动负债高于流动资产
//	3.资金紧张:流动负债高于货币资金
//	4.资金紧张:应付利息较高
//	5.资产负债率超高:
//	6.经营现金流入不敷出
//	7.季度经常现金流为负却有扣非净利
//	8.应收账款超高:

	public FinanceBaseInfo() {

	}

	public FinanceBaseInfo(String code, int date) {
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
