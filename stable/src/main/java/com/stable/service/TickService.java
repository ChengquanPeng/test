package com.stable.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.stable.vo.bus.DaliyBasicInfo2;

@Service
public class TickService {
	@Autowired
	private StockBasicService stockBasicService;
	
	public void genTickEveryDay(List<DaliyBasicInfo2> daliybasicList) {
		for (DaliyBasicInfo2 d : daliybasicList) {
			String code = d.getCode();
			if (stockBasicService.isHuShenCode(code)) {

			}
		}
	}
}
