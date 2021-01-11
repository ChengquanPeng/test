package com.stable.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.stable.utils.RedisUtil;

import lombok.extern.log4j.Log4j2;

/**
 * 板块
 */
@Service
@Log4j2
public class PlateService {
	@Autowired
	private ConceptService conceptService;
	@Autowired
	private StockBasicService stockBasicService;
	@Autowired
	private RedisUtil redisUtil;

}
