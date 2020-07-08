package com.stable.vo.bus;

import java.util.Date;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import com.alibaba.fastjson.JSONArray;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Setter
@Getter
@ToString
@Document(indexName = "finance_base_info")
public class FinanceBaseInfo extends EsBase {

	/**
	 * 
	 */
	private static final long serialVersionUID = 2763858886536567582L;
	@Id
	private String id;
	@Field(type = FieldType.Text)
	private String code;
	@Field(type = FieldType.Text)
	private String ts_code;// TS代码
	@Field(type = FieldType.Integer)
	private int ann_date;// 公告日期
	@Field(type = FieldType.Integer)
	private int f_ann_date; // 实际公告日期
	@Field(type = FieldType.Integer)
	private int end_date; // 报告期
	@Field(type = FieldType.Integer)
	private int report_type; // 报告类型 1合并报表 2单季合并 3调整单季合并表 4调整合并报表 5调整前合并报表 6母公司报表 7母公司单季表 8 母公司调整单季表 9母公司调整表
								// 10母公司调整前报表 11调整前合并报表 12母公司调整前报表

	@Field(type = FieldType.Integer)
	private int comp_type; // 公司类型(1一般工商业2银行3保险4证券)
	@Field(type = FieldType.Double)
	private double basic_eps; // 基本每股收益
	@Field(type = FieldType.Double)
	private double diluted_eps; // 稀释每股收益
	@Field(type = FieldType.Double)
	private double total_revenue; // 营业总收入
	@Field(type = FieldType.Double)
	private double revenue; // 营业收入
	@Field(type = FieldType.Double)
	private double int_income; // 利息收入
	@Field(type = FieldType.Double)
	private double oth_b_income; // 其他业务收入
	@Field(type = FieldType.Double)
	private double total_cogs; // 营业总成本
	@Field(type = FieldType.Double)
	private double oper_cost; // 减:营业成本
	@Field(type = FieldType.Double)
	private double int_exp; // 减:利息支出
	@Field(type = FieldType.Double)
	private double comm_exp; // 减:手续费及佣金支出
	@Field(type = FieldType.Double)
	private double biz_tax_surchg; // 减:营业税金及附加
	@Field(type = FieldType.Double)
	private double sell_exp; // 减:销售费用
	@Field(type = FieldType.Double)
	private double admin_exp; // 减:管理费用
	@Field(type = FieldType.Double)
	private double fin_exp; // 减:财务费用
	@Field(type = FieldType.Double)
	private double assets_impair_loss; // 减:资产减值损失
	@Field(type = FieldType.Double)
	private double other_bus_cost; // 其他业务成本
	@Field(type = FieldType.Double)
	private double operate_profit; // 营业利润
	@Field(type = FieldType.Double)
	private double non_oper_income; // 加:营业外收入
	@Field(type = FieldType.Double)
	private double non_oper_exp; // 减:营业外支出
	@Field(type = FieldType.Double)
	private double nca_disploss; // 其中:减:非流动资产处置净损失
	@Field(type = FieldType.Double)
	private double total_profit; // 利润总额
	@Field(type = FieldType.Double)
	private double income_tax; // 所得税费用
	@Field(type = FieldType.Double)
	private double n_income; // 净利润(含少数股东损益)
	@Field(type = FieldType.Double)
	private double n_income_attr_p; // 净利润(不含少数股东损益)
	@Field(type = FieldType.Double)
	private double minority_gain; // 少数股东损益
	@Field(type = FieldType.Double)
	private double oth_compr_income; // 其他综合收益
	@Field(type = FieldType.Double)
	private double t_compr_income; // 综合收益总额
	@Field(type = FieldType.Double)
	private double compr_inc_attr_p; // 归属于母公司(或股东)的综合收益总额
	@Field(type = FieldType.Double)
	private double compr_inc_attr_m_s; // 归属于少数股东的综合收益总额
	@Field(type = FieldType.Double)
	private double undist_profit; // 年初未分配利润
	@Field(type = FieldType.Double)
	private double distable_profit; // 可分配利润
	@Field(type = FieldType.Integer)
	private int year;
	@Field(type = FieldType.Integer)
	private int quarter;
	@Field(type = FieldType.Date)
	private Date updateDate;

	public void setValue(String code, JSONArray arr) {
		int i = 0;
		ts_code = arr.getString(i++);
		try {
			ann_date = Integer.valueOf(arr.getString(i++));
		} catch (Exception e) {
		}
		try {
			f_ann_date = Integer.valueOf(arr.getString(i++));
		} catch (Exception e) {
		}
		try {
			end_date = Integer.valueOf(arr.getString(i++));
		} catch (Exception e) {
		}
		try {
			report_type = Integer.valueOf(arr.getString(i++));
		} catch (Exception e) {
		}
		try {
			comp_type = Integer.valueOf(arr.getString(i++));
		} catch (Exception e) {
		}
		try {
			basic_eps = Double.valueOf(arr.getString(i++));
		} catch (Exception e) {
		}
		try {
			diluted_eps = Double.valueOf(arr.getString(i++));
		} catch (Exception e) {
		}
		try {
			total_revenue = Double.valueOf(arr.getString(i++));
		} catch (Exception e) {
		}
		try {
			revenue = Double.valueOf(arr.getString(i++));
		} catch (Exception e) {
		}
		try {
			int_income = Double.valueOf(arr.getString(i++));
		} catch (Exception e) {
		}
		try {
			oth_b_income = Double.valueOf(arr.getString(i++));
		} catch (Exception e) {
		}
		try {
			total_cogs = Double.valueOf(arr.getString(i++));
		} catch (Exception e) {
		}
		try {
			oper_cost = Double.valueOf(arr.getString(i++));
		} catch (Exception e) {
		}
		try {
			int_exp = Double.valueOf(arr.getString(i++));
		} catch (Exception e) {
		}
		try {
			comm_exp = Double.valueOf(arr.getString(i++));
		} catch (Exception e) {
		}
		try {
			biz_tax_surchg = Double.valueOf(arr.getString(i++));
		} catch (Exception e) {
		}
		try {
			sell_exp = Double.valueOf(arr.getString(i++));
		} catch (Exception e) {
		}
		try {
			admin_exp = Double.valueOf(arr.getString(i++));
		} catch (Exception e) {
		}
		try {
			fin_exp = Double.valueOf(arr.getString(i++));
		} catch (Exception e) {
		}
		try {
			assets_impair_loss = Double.valueOf(arr.getString(i++));
		} catch (Exception e) {
		}
		try {
			other_bus_cost = Double.valueOf(arr.getString(i++));
		} catch (Exception e) {
		}
		try {
			operate_profit = Double.valueOf(arr.getString(i++));
		} catch (Exception e) {
		}
		try {
			non_oper_income = Double.valueOf(arr.getString(i++));
		} catch (Exception e) {
		}
		try {
			non_oper_exp = Double.valueOf(arr.getString(i++));
		} catch (Exception e) {
		}
		try {
			nca_disploss = Double.valueOf(arr.getString(i++));
		} catch (Exception e) {
		}
		try {
			total_profit = Double.valueOf(arr.getString(i++));
		} catch (Exception e) {
		}
		try {
			income_tax = Double.valueOf(arr.getString(i++));
		} catch (Exception e) {
		}
		try {
			n_income = Double.valueOf(arr.getString(i++));
		} catch (Exception e) {
		}
		try {
			n_income_attr_p = Double.valueOf(arr.getString(i++));
		} catch (Exception e) {
		}
		try {
			minority_gain = Double.valueOf(arr.getString(i++));
		} catch (Exception e) {
		}
		try {
			oth_compr_income = Double.valueOf(arr.getString(i++));
		} catch (Exception e) {
		}
		try {
			t_compr_income = Double.valueOf(arr.getString(i++));
		} catch (Exception e) {
		}
		try {
			compr_inc_attr_p = Double.valueOf(arr.getString(i++));
		} catch (Exception e) {
		}
		try {
			compr_inc_attr_m_s = Double.valueOf(arr.getString(i++));
		} catch (Exception e) {
		}
		try {
			undist_profit = Double.valueOf(arr.getString(i++));
		} catch (Exception e) {
		}
		try {
			distable_profit = Double.valueOf(arr.getString(i++));
		} catch (Exception e) {
		}
		this.code = code;
		this.updateDate = new Date();
		getYearMonth(String.valueOf(end_date));
		this.id = this.code + "_" + this.end_date;
	}

	private void getYearMonth(String datestr) {
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
	}
}
