package com.stable.job;

import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import com.stable.enums.RunCycleEnum;
import com.stable.enums.RunLogBizTypeEnum;
import com.stable.utils.DateUtil;
import com.stable.utils.ErrorLogFileUitl;
import com.stable.utils.WxPushUtil;

import lombok.extern.log4j.Log4j2;

@Log4j2
public abstract class MyCallable implements Callable<Object> {
	private RunLogBizTypeEnum biz;
	private RunCycleEnum cycle;
	// private RunLogService service = SpringUtil.getBean("RunLogService",
	// RunLogService.class);
	private String remark = "";

	public MyCallable(RunLogBizTypeEnum biz, RunCycleEnum cycle) {
		this.biz = biz;
		this.cycle = cycle;
	}

	public MyCallable(RunLogBizTypeEnum biz, RunCycleEnum cycle, String remark) {
		this.biz = biz;
		this.cycle = cycle;
		this.remark = remark;
	}

	@Override
	public Object call() throws Exception {
		log.info("开始执行任务：{}", biz.getBtypeName());
		String starttime = DateUtil.getTodayYYYYMMDDHHMMSS();
		Thread t = new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					TimeUnit.HOURS.sleep(12);
					WxPushUtil.pushSystem1(">>>执行超时异常<<< " + cycle.getName() + " " + biz.getBtypeName() + " 开始时间:"
							+ starttime + " 结束时间：" + DateUtil.getTodayYYYYMMDDHHMMSS());
				} catch (Exception e) {
				}
			}
		});
		t.start();
		int status = 0;
		try {
			Object result = mycall();
			log.info("正常完成任务：{}", biz.getBtypeName());
			return result;
		} catch (Exception e) {
			e.printStackTrace();
			ErrorLogFileUitl.writeError(e, biz.getBtypeName(), cycle.getName(), remark);
			log.info("执行任务异常：{},remark", biz.getBtypeName(), remark);
			status = 2;
		} finally {
			pushWx(status, starttime, DateUtil.getTodayYYYYMMDDHHMMSS());
			// service.addLog(rl);
			t.interrupt();
		}
		return null;
	}

	private void pushWx(int status, String startTime, String endTime) {
		if (status == 2) {
			WxPushUtil.pushSystem1(">>>异常<<< " + cycle.getName() + " " + biz.getBtypeName() + " 开始时间:" + startTime
					+ " 结束时间：" + endTime);
		}
	}

	public abstract Object mycall();
}
