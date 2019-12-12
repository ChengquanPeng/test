package com.stable.vo.bus;

import java.beans.Transient;
import java.util.Date;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import com.alibaba.fastjson.JSONArray;

import lombok.Data;

@SuppressWarnings("serial")
@Data
@Document(indexName = "finance_base_info")
public class FinanceBaseInfo extends EsBase {

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
	@Field(type = FieldType.Float)
	private float basic_eps; // 基本每股收益
	@Field(type = FieldType.Float)
	private float diluted_eps; // 稀释每股收益
	@Field(type = FieldType.Float)
	private float total_revenue; // 营业总收入
	@Field(type = FieldType.Float)
	private float revenue; // 营业收入
	@Field(type = FieldType.Float)
	private float int_income; // 利息收入
	@Field(type = FieldType.Float)
	private float prem_earned; // 已赚保费
	@Field(type = FieldType.Float)
	private float comm_income; // 手续费及佣金收入
	@Field(type = FieldType.Float)
	private float n_commis_income; // 手续费及佣金净收入
	@Field(type = FieldType.Float)
	private float n_oth_income; // 其他经营净收益
	@Field(type = FieldType.Float)
	private float n_oth_b_income; // 加:其他业务净收益
	@Field(type = FieldType.Float)
	private float prem_income; // 保险业务收入
	@Field(type = FieldType.Float)
	private float out_prem; // 减:分出保费
	@Field(type = FieldType.Float)
	private float une_prem_reser; // 提取未到期责任准备金
	@Field(type = FieldType.Float)
	private float reins_income; // 其中:分保费收入
	@Field(type = FieldType.Float)
	private float n_sec_tb_income; // 代理买卖证券业务净收入
	@Field(type = FieldType.Float)
	private float n_sec_uw_income; // 证券承销业务净收入
	@Field(type = FieldType.Float)
	private float n_asset_mg_income; // 受托客户资产管理业务净收入
	@Field(type = FieldType.Float)
	private float oth_b_income; // 其他业务收入
	@Field(type = FieldType.Float)
	private float fv_value_chg_gain; // 加:公允价值变动净收益
	@Field(type = FieldType.Float)
	private float invest_income; // 加:投资净收益
	@Field(type = FieldType.Float)
	private float ass_invest_income; // 其中:对联营企业和合营企业的投资收益
	@Field(type = FieldType.Float)
	private float forex_gain; // 加:汇兑净收益
	@Field(type = FieldType.Float)
	private float total_cogs; // 营业总成本
	@Field(type = FieldType.Float)
	private float oper_cost; // 减:营业成本
	@Field(type = FieldType.Float)
	private float int_exp; // 减:利息支出
	@Field(type = FieldType.Float)
	private float comm_exp; // 减:手续费及佣金支出
	@Field(type = FieldType.Float)
	private float biz_tax_surchg; // 减:营业税金及附加
	@Field(type = FieldType.Float)
	private float sell_exp; // 减:销售费用
	@Field(type = FieldType.Float)
	private float admin_exp; // 减:管理费用
	@Field(type = FieldType.Float)
	private float fin_exp; // 减:财务费用
	@Field(type = FieldType.Float)
	private float assets_impair_loss; // 减:资产减值损失
	@Field(type = FieldType.Float)
	private float prem_refund; // 退保金
	@Field(type = FieldType.Float)
	private float compens_payout; // 赔付总支出
	@Field(type = FieldType.Float)
	private float reser_insur_liab; // 提取保险责任准备金
	@Field(type = FieldType.Float)
	private float div_payt; // 保户红利支出
	@Field(type = FieldType.Float)
	private float reins_exp; // 分保费用
	@Field(type = FieldType.Float)
	private float oper_exp; // 营业支出
	@Field(type = FieldType.Float)
	private float compens_payout_refu; // 减:摊回赔付支出
	@Field(type = FieldType.Float)
	private float insur_reser_refu; // 减:摊回保险责任准备金
	@Field(type = FieldType.Float)
	private float reins_cost_refund; // 减:摊回分保费用
	@Field(type = FieldType.Float)
	private float other_bus_cost; // 其他业务成本
	@Field(type = FieldType.Float)
	private float operate_profit; // 营业利润
	@Field(type = FieldType.Float)
	private float non_oper_income; // 加:营业外收入
	@Field(type = FieldType.Float)
	private float non_oper_exp; // 减:营业外支出
	@Field(type = FieldType.Float)
	private float nca_disploss; // 其中:减:非流动资产处置净损失
	@Field(type = FieldType.Float)
	private float total_profit; // 利润总额
	@Field(type = FieldType.Float)
	private float income_tax; // 所得税费用
	@Field(type = FieldType.Float)
	private float n_income; // 净利润(含少数股东损益)
	@Field(type = FieldType.Float)
	private float n_income_attr_p; // 净利润(不含少数股东损益)
	@Field(type = FieldType.Float)
	private float minority_gain; // 少数股东损益
	@Field(type = FieldType.Float)
	private float oth_compr_income; // 其他综合收益
	@Field(type = FieldType.Float)
	private float t_compr_income; // 综合收益总额
	@Field(type = FieldType.Float)
	private float compr_inc_attr_p; // 归属于母公司(或股东)的综合收益总额
	@Field(type = FieldType.Float)
	private float compr_inc_attr_m_s; // 归属于少数股东的综合收益总额
	@Field(type = FieldType.Float)
	private float ebit; // 息税前利润
	@Field(type = FieldType.Float)
	private float ebitda; // 息税折旧摊销前利润
	@Field(type = FieldType.Float)
	private float insurance_exp; // 保险业务支出
	@Field(type = FieldType.Float)
	private float undist_profit; // 年初未分配利润
	@Field(type = FieldType.Float)
	private float distable_profit; // 可分配利润
	@Field(type = FieldType.Integer)
	private int update_flag; // 更新标识，0未修改1更正过
	@Field(type = FieldType.Integer)
	private int year;
	@Field(type = FieldType.Integer)
	private int quarter;
	@Field(type = FieldType.Date)
	private Date updateDate;

	// 要获取类的信息，首先要获取类的类类型
	private Class<?> classType = null;
	// 获得对象的所有属性
	private java.lang.reflect.Field[] classfields = null;

	//@Transient 是不需要把字段放入到es中
	@Transient
	public void setClassType(Class<?> classType) {
		this.classType = classType;
	}

	@Transient
	public void setClassfields(java.lang.reflect.Field[] classfields) {
		this.classfields = classfields;
	}

	private java.lang.reflect.Field getClassfield(String fieldName) {
		if (classType == null) {
			classType = this.getClass();
			classfields = classType.getDeclaredFields();
		}
		for (int i = 0; i < classfields.length; i++) {
			if (classfields[i].getName().equalsIgnoreCase(fieldName)) {
				return classfields[i];
			}
		}
		return null;
	}

	public void setValue(String code, JSONArray fields, JSONArray items) {
		for (int i = 0; i < fields.size(); i++) {
			String fieldName = fields.getString(i);
			java.lang.reflect.Field clzField = this.getClassfield(fieldName);
			Object value = items.get(i);
			if (value != null && clzField != null) {

				String firstLetter = fieldName.substring(0, 1).toUpperCase();
				String setMethodName = "set" + firstLetter + fieldName.substring(1);
				try {
					java.lang.reflect.Method setMethod = classType.getMethod(setMethodName,
							new Class[] { clzField.getType() });

					String paramType = setMethod.getParameters()[0].getType().getName();
					if (paramType.equalsIgnoreCase("int")) {
						value = Integer.valueOf(value.toString().trim());
					} else if (paramType.equalsIgnoreCase("float")) {
						value = Float.valueOf(value.toString().trim());
					}
					setMethod.invoke(this, new Object[] { value });
				} catch (Exception e) {
					e.printStackTrace();
				}
			}

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
