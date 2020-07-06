package com.stable.service.model;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.ui.Model;

import com.stable.service.FinanceService;

import lombok.extern.log4j.Log4j2;

@Service
@Log4j2
public class CodeBaseService {

	@Autowired
	private FinanceService financeService;

	public void getCodeModel(String code, Model model) {

	}
	
	
}
