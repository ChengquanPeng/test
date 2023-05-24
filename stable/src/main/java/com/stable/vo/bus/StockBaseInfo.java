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
@Document(indexName = "stock_base_info")
public class StockBaseInfo extends EsBase {
	private static final long serialVersionUID = 4910896688001534666L;
	// 股票代码
	@Id
	private String code;
	// 股票名称
	@Field(type = FieldType.Text)
	private String name;

	// 市场类型 （上海、深圳、北京）
	@Field(type = FieldType.Text)
	private String market;

	// 上市状态： L上市 D退市 P暂停上市
	@Field(type = FieldType.Text)
	private String list_status;
	// 上市日期
	@Field(type = FieldType.Text)
	private String list_date;

	@Field(type = FieldType.Text)
	private String oldName;// 曾用名
	@Field(type = FieldType.Text)
	private String webSite;// 网站
	@Field(type = FieldType.Text)
	private String holderName;// 控股股东
	@Field(type = FieldType.Text)
	private String finalControl;// 最终控制人
	@Field(type = FieldType.Text)
	private int compnayType;// 公司性质：0普通，1国企
	@Field(type = FieldType.Double)
	private double totalShare;// float 总股本 （亿股）
	@Field(type = FieldType.Double)
	private double floatShare;// float 流通股本 （亿股）
	@Field(type = FieldType.Double)
	private double circZb;// 持股5%以上的股东占比-流通

	@Field(type = FieldType.Integer)
	private int dfcwCompnayType;// 公司类型：1券商，2保险,3银行，4企业
	@Field(type = FieldType.Integer)
	private int tssc = 0;// 退市删除

	@Field(type = FieldType.Double)
	private double totalMarketVal;// 总市值 （亿）
	@Field(type = FieldType.Double)
	private double circMarketVal;// 流通市值（亿）

	// 同花顺行业
	@Field(type = FieldType.Text)
	private String thsIndustry;
	// 同花顺-主营
	@Field(type = FieldType.Text)
	private String thsMainBiz;
	// 同花顺-亮点
	@Field(type = FieldType.Text)
	private String thsLightspot;

	public StockBaseInfo() {

	}
}
