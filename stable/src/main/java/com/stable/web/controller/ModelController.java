package com.stable.web.controller;

import java.util.Date;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.stable.service.model.CodeModelService;
import com.stable.service.model.UpModelLineService;
import com.stable.utils.DateUtil;
import com.stable.vo.http.JsonResult;

import lombok.extern.log4j.Log4j2;

@RequestMapping("/model")
@RestController
@Log4j2
public class ModelController {

	@Autowired
	private UpModelLineService upLevel1Service;
	@Autowired
	private CodeModelService codeModelService;

	/**
	 * 执行模型（交易面）
	 */
	@RequestMapping(value = "/run", method = RequestMethod.GET)
	public ResponseEntity<JsonResult> run(String startDate, String endDate) {
		JsonResult r = new JsonResult();
		try {
			log.info("startDate={},endDate={}", startDate, endDate);
			if (StringUtils.isBlank(startDate) && StringUtils.isBlank(endDate)) {
				return ResponseEntity.badRequest().build();
			}
			if (StringUtils.isNotBlank(startDate) && StringUtils.isBlank(endDate)) {
				log.info("request date={}", startDate);
				upLevel1Service.runJob(false, Integer.valueOf(startDate));
			} else if (StringUtils.isBlank(startDate) && StringUtils.isNotBlank(endDate)) {
				log.info("request date={}", endDate);
				upLevel1Service.runJob(false, Integer.valueOf(endDate));
			} else if (StringUtils.isNotBlank(startDate) && StringUtils.isNotBlank(endDate)) {

				Date d = DateUtil.parseDate(startDate);
				int end = Integer.valueOf(endDate);
				int date = Integer.valueOf(DateUtil.formatYYYYMMDD(d));
				do {
					log.info("request date={}", date);
					upLevel1Service.runJob(false, date);
					d = DateUtil.addDate(d, 1);
					date = Integer.valueOf(DateUtil.formatYYYYMMDD(d));
				} while (date <= end);
			}
			r.setStatus(JsonResult.OK);
		} catch (Exception e) {
			r.setResult(e.getClass().getName() + ":" + e.getMessage());
			r.setStatus(JsonResult.ERROR);
			e.printStackTrace();
		}
		return ResponseEntity.ok(r);
	}

	/**
	 * 执行模型（基本面)
	 */
	@RequestMapping(value = "/coderun", method = RequestMethod.GET)
	public ResponseEntity<JsonResult> coderun(String startDate, String endDate) {
		JsonResult r = new JsonResult();
		try {
			codeModelService.reset();
			codeModelService.runJob(false, DateUtil.getTodayIntYYYYMMDD());
			r.setStatus(JsonResult.OK);
		} catch (Exception e) {
			r.setResult(e.getClass().getName() + ":" + e.getMessage());
			r.setStatus(JsonResult.ERROR);
			e.printStackTrace();
		}
		return ResponseEntity.ok(r);
	}
}
