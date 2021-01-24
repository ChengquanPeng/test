package com.stable.vo;

import lombok.Data;

@Data
public class WeekendFinFetchRtl {

	private boolean thsHolderOk = false;
	private boolean dfZfOk = false;
	private boolean dfFinOk = false;

	public boolean isAllOk() {
		return (thsHolderOk && dfFinOk && dfFinOk);
	}
}
