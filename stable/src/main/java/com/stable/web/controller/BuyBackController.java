package com.stable.web.controller;

import java.util.Calendar;
import java.util.concurrent.Callable;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.stable.service.BuyBackService;
import com.stable.utils.DateUtil;
import com.stable.utils.TasksWorker;
import com.stable.vo.http.JsonResult;
import com.stable.vo.spi.req.EsQueryPageReq;

import lombok.extern.log4j.Log4j2;

@RequestMapping("/buyback")
@RestController
@Log4j2
public class BuyBackController {

	@Autowired
	private BuyBackService buyBackService;

	/**
	 * 根据 开始时间和结束时间 抓取回购信息
	 */
	@RequestMapping(value = "/fetch", method = RequestMethod.GET)
	public ResponseEntity<JsonResult> fetch(String startDate, String endDate) {
		JsonResult r = new JsonResult();
		try {
			if (startDate.length() != 8 || endDate.length() != 8) {
				r.setStatus(JsonResult.FAIL);
				return ResponseEntity.ok(r);
			}
			Integer.parseInt(startDate);
			Integer.parseInt(endDate);
			buyBackService.spiderBuyBackHistoryInfo(startDate, endDate);
			r.setResult(JsonResult.OK);
		} catch (Exception e) {
			r.setResult(e.getClass().getName() + ":" + e.getMessage());
			r.setStatus(JsonResult.ERROR);
			e.printStackTrace();
		}
		return ResponseEntity.ok(r);
	}

	@RequestMapping(value = "/fetchall", method = RequestMethod.GET)
	public ResponseEntity<JsonResult> fetchall() {
		JsonResult r = new JsonResult();
		try {
			TasksWorker.getInstance().getService().submit(new Callable<Object>() {

				@Override
				public Object call() throws Exception {
					Calendar cal = Calendar.getInstance();
					String startDate = "", endDate = "";
					int ife = 0, first = 0, last = 0;
					do {
						first = cal.getActualMinimum(Calendar.DAY_OF_MONTH);
						cal.set(Calendar.DAY_OF_MONTH, first);
						startDate = DateUtil.getYYYYMMDD(cal.getTime());

						last = cal.getActualMaximum(Calendar.DAY_OF_MONTH);
						cal.set(Calendar.DAY_OF_MONTH, last);
						endDate = DateUtil.getYYYYMMDD(cal.getTime());

						log.info("回购爬虫时间从{}到{}", startDate, endDate);
						buyBackService.fetchHist(startDate, endDate);

						ife = Integer.valueOf(endDate);
						cal.add(Calendar.MONTH, -1);
					} while (ife >= 20100101);
					return null;
				}

			});

			r.setResult(JsonResult.OK);
		} catch (Exception e) {
			r.setResult(e.getClass().getName() + ":" + e.getMessage());
			r.setStatus(JsonResult.ERROR);
			e.printStackTrace();
		}
		return ResponseEntity.ok(r);
	}

	/**
	 * 根据code查询财务信息
	 */
	@RequestMapping(value = "/list", method = RequestMethod.GET)
	public ResponseEntity<JsonResult> list(String code, int dtype, int asc, EsQueryPageReq page) {
		JsonResult r = new JsonResult();
		try {
			r.setResult(buyBackService.getListByCodeForWebPage(code, dtype, asc, page));
			r.setStatus(JsonResult.OK);
		} catch (Exception e) {
			r.setResult(e.getClass().getName() + ":" + e.getMessage());
			r.setStatus(JsonResult.ERROR);
			e.printStackTrace();
		}
		return ResponseEntity.ok(r);
	}

	@RequestMapping(value = "/list/{code}", method = RequestMethod.GET)
	public ResponseEntity<JsonResult> listcode(@PathVariable(value = "code") String code, int dtype, int asc,
			EsQueryPageReq page) {
		JsonResult r = new JsonResult();
		try {
			r.setResult(buyBackService.getListByCodeForWebPage(code, dtype, asc, page));
			r.setStatus(JsonResult.OK);
		} catch (Exception e) {
			r.setResult(e.getClass().getName() + ":" + e.getMessage());
			r.setStatus(JsonResult.ERROR);
			e.printStackTrace();
		}
		return ResponseEntity.ok(r);
	}
}
