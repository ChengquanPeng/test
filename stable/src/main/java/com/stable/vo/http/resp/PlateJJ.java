package com.stable.vo.http.resp;

import com.stable.utils.CurrencyUitl;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Setter
@Getter
@ToString
public class PlateJJ {

	private String code;
	private String codeName;

	int total = 0;
	int yup = 0;
	int lup = 0;

	private double y = 0;
	private double l = 0;

	public void exs() {
		if (total > 0) {
			y = CurrencyUitl.roundHalfUp(Double.valueOf(yup) / Double.valueOf(total) * 100);
			l = CurrencyUitl.roundHalfUp(Double.valueOf(lup) / Double.valueOf(total) * 100);
		} else {
			System.err.println();
		}
	}

	public static void main(String[] args) {
		PlateJJ jj = new PlateJJ();
		jj.exs();
		System.err.println(jj);
	}
}
