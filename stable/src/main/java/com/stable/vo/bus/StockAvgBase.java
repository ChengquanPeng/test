package com.stable.vo.bus;

/*
 * 
 * 
 * StockAvg 前复权
 * StockAvgNofq 不复权
 * StockAvgBase 父类，解决历史问题
 * 
 */

public interface StockAvgBase {

	public void setId();

	public String getId();

	public void setId(String id);

	public String getCode();

	public void setCode(String code);

	public int getDate();

	public void setDate(int date);

	public double getAvgPriceIndex5();

	public void setAvgPriceIndex5(double avgPriceIndex5);

	public double getAvgPriceIndex10();

	public void setAvgPriceIndex10(double avgPriceIndex10);

	public double getAvgPriceIndex20();

	public void setAvgPriceIndex20(double avgPriceIndex20);

	public double getAvgPriceIndex30();

	public void setAvgPriceIndex30(double avgPriceIndex30);

	public double getAvgPriceIndex60();

	public void setAvgPriceIndex60(double avgPriceIndex60);

	public double getAvgPriceIndex120();

	public void setAvgPriceIndex120(double avgPriceIndex120);

	public double getAvgPriceIndex250();

	public void setAvgPriceIndex250(double avgPriceIndex250);

	public int getLastDividendDate();

	public void setLastDividendDate(int lastDividendDate);
}
