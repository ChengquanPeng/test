package com.stable.web.controller;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.dangdang.ddframe.job.api.simple.SimpleJob;
import com.stable.service.StockBasicService;
import com.stable.service.TradeCalService;
import com.stable.utils.OSystemUtil;
import com.stable.utils.SpringUtil;
import com.stable.vo.http.JsonResult;

import lombok.extern.log4j.Log4j2;

@RequestMapping("/job")
@RestController
@Log4j2
public class JobController {

	@Autowired
	private TradeCalService tradeCalService;
	@Autowired
	private StockBasicService stockBasicService;

	@RequestMapping(value = "/jobs", method = RequestMethod.GET)
	public ResponseEntity<JsonResult> jobs() {
		JsonResult r = new JsonResult();
		try {
			r.setStatus(JsonResult.OK);
			String[] jobs = SpringUtil.getApplicationContext().getBeanDefinitionNames();
			r.setResult(jobs);
		} catch (Exception e) {
			r.setResult(e.getClass().getName() + ":" + e.getMessage());
			r.setStatus(JsonResult.ERROR);
			e.printStackTrace();
		}
		return ResponseEntity.ok(r);
	}

	@RequestMapping(value = "/executejob", method = RequestMethod.GET)
	public ResponseEntity<JsonResult> executejob(String jobName) {
		JsonResult r = new JsonResult();
		log.info("jobName:[{}]", jobName);
		try {
			Object job = SpringUtil.getBean(jobName);
			if (job != null && job instanceof SimpleJob) {
				((SimpleJob) job).execute(null);
				r.setStatus(JsonResult.OK);
				r.setResult(job.getClass().getName());
			} else {
				r.setStatus(JsonResult.FAIL);
				r.setResult(jobName);
			}
		} catch (Exception e) {
			r.setResult(e.getClass().getName() + ":" + e.getMessage());
			r.setStatus(JsonResult.ERROR);
			e.printStackTrace();
		}
		return ResponseEntity.ok(r);
	}

	/**
	 * 手动同步交易日历
	 */
	@RequestMapping(value = "/tradecal", method = RequestMethod.GET)
	public ResponseEntity<JsonResult> tradecal() {
		JsonResult r = new JsonResult();
		try {
			tradeCalService.josSynTradeCal();
			r.setStatus(JsonResult.OK);
		} catch (Exception e) {
			r.setResult(e.getClass().getName() + ":" + e.getMessage());
			r.setStatus(JsonResult.ERROR);
			e.printStackTrace();
		}
		return ResponseEntity.ok(r);
	}

	/**
	 * 手动同步交易日历
	 */
	@RequestMapping(value = "/stocklist", method = RequestMethod.GET)
	public ResponseEntity<JsonResult> stocklist() {
		JsonResult r = new JsonResult();
		try {
			stockBasicService.jobSynStockListV2();
			r.setStatus(JsonResult.OK);
		} catch (Exception e) {
			r.setResult(e.getClass().getName() + ":" + e.getMessage());
			r.setStatus(JsonResult.ERROR);
			e.printStackTrace();
		}
		return ResponseEntity.ok(r);
	}

	@RequestMapping(value = "/systemStatus", method = RequestMethod.GET)
	public ResponseEntity<JsonResult> systemStatus() {
		JsonResult r = new JsonResult();
		InputStreamReader ir = null;
		BufferedReader input = null;
		StringBuffer sb = new StringBuffer("进程信息列表:");
		try {
			String cmd = "jps";
			Process proc = Runtime.getRuntime().exec(cmd);
			ir = new InputStreamReader(proc.getInputStream());
			input = new BufferedReader(ir);

			String line;
			while ((line = input.readLine()) != null) {
				// System.out.println(line);
				sb.append(line).append(",");
			}
			proc.waitFor();
			r.setStatus(JsonResult.OK);
			r.setResult(sb.toString());
			return ResponseEntity.ok(r);
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		} finally {
			if (input != null) {
				try {
					input.close();
				} catch (IOException e) {
				}
			}
			if (ir != null) {
				try {
					ir.close();
				} catch (IOException e) {
				}
			}
		}
	}

	@RequestMapping(value = "/restart", method = RequestMethod.GET)
	public ResponseEntity<JsonResult> restart() {
		JsonResult r = new JsonResult();
		try {
			OSystemUtil.restart();
			r.setStatus(JsonResult.OK);
			r.setResult("reboot");
			return ResponseEntity.ok(r);
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		} finally {
		}
	}
}
