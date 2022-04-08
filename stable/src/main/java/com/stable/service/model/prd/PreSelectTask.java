package com.stable.service.model.prd;

public class PreSelectTask implements Runnable {

	private PreSelectService dayModeService;
	private String code;
	private double mkv;
	private int date;

	public PreSelectTask(PreSelectService sdm, String code, double mkv, int date) {
		this.dayModeService = sdm;
		this.code = code;
		this.mkv = mkv;
		this.date = date;
		sdm.cntDone.incrementAndGet();
	}

	@Override
	public void run() {
		dayModeService.sort2ModeChk(code, mkv, date);
	}

}
