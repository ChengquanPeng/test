package com.stable.service.model;

public interface StrategyListener {

	public boolean condition(Object ...obj);

	public void fulshToFile();

}
