package com.stable.vo;

import lombok.Data;

@Data
public class WeekendFinFetchRtl {

	private boolean thsHolderOk = false;
	private boolean dfFinOk = false;
	private boolean dzjyOk = false;

	public boolean isAllOk() {
		return (thsHolderOk && dfFinOk && dzjyOk);
	}
}
