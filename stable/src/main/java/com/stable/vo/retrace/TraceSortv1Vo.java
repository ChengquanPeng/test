package com.stable.vo.retrace;

import com.stable.vo.bus.DaliyBasicInfo;
import com.stable.vo.bus.TickData;

import lombok.Data;

@Data
public class TraceSortv1Vo {
	private DaliyBasicInfo daliyBasicInfo;
	private TickData firstTopPrice;
	private boolean buyed = false;
	private boolean isOk = false;

	public String toDetailStr() {
		return "TraceSortv1Vo:" + daliyBasicInfo.getCode() + "," + daliyBasicInfo.getTrade_date() + ",买入?" + buyed
				+ ",盈利?" + isOk;
	}

	public String toDetailStrShow() {
		return daliyBasicInfo.getCode() + "@" + daliyBasicInfo.getTrade_date();
	}
}
