package com.stable.web.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.stable.service.monitor.RealtimeMonitoringService;
import com.stable.spider.realtime.RealtimeCall;

@RestController
public class RealtimeController {

	@Autowired
	private RealtimeMonitoringService realtimeMonitoringService;

	/**
	 * 个股当前状态
	 */
	@RequestMapping(value = "/rtset/{s}", method = RequestMethod.GET)
	public synchronized String detail2(@PathVariable(value = "s") int s) {
		if (s == 0 || s == 1 || s == 2) {
			// 1.设置数据源
			RealtimeCall.source = s;
			// 2.终止线程
			realtimeMonitoringService.stopAllThreads();
			if (realtimeMonitoringService.currThread != null) {
				realtimeMonitoringService.currThread.interrupt();
			}
			// 3.开启新线程
			new Thread(new Runnable() {
				public void run() {
					realtimeMonitoringService.startObservable();
				}
			}).start();
		}

		return "0：全部 | 1：新浪 | 2：163 | 目前source=" + s;
	}

}
