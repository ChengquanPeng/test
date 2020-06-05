package com.stable.vo;

import java.util.List;
import java.util.Map;

import com.stable.service.ConceptService.ConceptInfo;
import com.stable.vo.bus.DaliyBasicInfo;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class ModelContext {
	// 1强势:短中长期买入=>次数和差值:3/5/10/20/120/250天
	private String code;
	private int date;
	DaliyBasicInfo today;
	private String baseDataOk = "";
	private int priceIndex = 0;
	private Map<String, List<ConceptInfo>> gnDaliy;

	private int score;
	private boolean base30Avg;
	private int sortWay;
	private int sortPgm;

	private int wayTimes3;
	private Long wayDef3;
	private int wayTimes5;
	private Long wayDef5;

	private int pgmTimes3;
	private int pgmTimes5;

	private String imgResult;

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

	public int getDate() {
		return date;
	}

	public void setDate(int date) {
		this.date = date;
	}

	public DaliyBasicInfo getToday() {
		return today;
	}

	public void setToday(DaliyBasicInfo today) {
		this.today = today;
	}

	public String getBaseDataOk() {
		return baseDataOk;
	}

	public void setBaseDataOk(String baseDataOk) {
		this.baseDataOk = baseDataOk;
	}

	public int getPriceIndex() {
		return priceIndex;
	}

	public void setPriceIndex(int priceIndex) {
		this.priceIndex = priceIndex;
	}

	public Map<String, List<ConceptInfo>> getGnDaliy() {
		return gnDaliy;
	}

	public void setGnDaliy(Map<String, List<ConceptInfo>> gnDaliy) {
		this.gnDaliy = gnDaliy;
	}

	public int getScore() {
		return score;
	}

	public void setScore(int score) {
		this.score = score;
	}

	public boolean isBase30Avg() {
		return base30Avg;
	}

	public void setBase30Avg(boolean base30Avg) {
		this.base30Avg = base30Avg;
	}

	public int getSortWay() {
		return sortWay;
	}

	public void setSortWay(int sortWay) {
		this.sortWay = sortWay;
	}

	public int getSortPgm() {
		return sortPgm;
	}

	public void setSortPgm(int sortPgm) {
		this.sortPgm = sortPgm;
	}

	public int getWayTimes3() {
		return wayTimes3;
	}

	public void setWayTimes3(int wayTimes3) {
		this.wayTimes3 = wayTimes3;
	}

	public Long getWayDef3() {
		return wayDef3;
	}

	public void setWayDef3(Long wayDef3) {
		this.wayDef3 = wayDef3;
	}

	public int getWayTimes5() {
		return wayTimes5;
	}

	public void setWayTimes5(int wayTimes5) {
		this.wayTimes5 = wayTimes5;
	}

	public Long getWayDef5() {
		return wayDef5;
	}

	public void setWayDef5(Long wayDef5) {
		this.wayDef5 = wayDef5;
	}

	public int getPgmTimes3() {
		return pgmTimes3;
	}

	public void setPgmTimes3(int pgmTimes3) {
		this.pgmTimes3 = pgmTimes3;
	}

	public int getPgmTimes5() {
		return pgmTimes5;
	}

	public void setPgmTimes5(int pgmTimes5) {
		this.pgmTimes5 = pgmTimes5;
	}

	public String getImgResult() {
		return imgResult;
	}

	public void setImgResult(String imgResult) {
		this.imgResult = imgResult;
	}

}
