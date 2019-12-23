package com.stable.job;

import java.util.Date;
import java.util.concurrent.Callable;

import com.stable.enums.RunCycleEnum;
import com.stable.enums.RunLogBizTypeEnum;
import com.stable.service.RunLogService;
import com.stable.utils.DateUtil;
import com.stable.utils.ErrorLogFileUitl;
import com.stable.utils.SpringUtil;
import com.stable.vo.bus.RunLog;

import lombok.extern.log4j.Log4j2;

@Log4j2
public abstract class MyCallable implements Callable<Object> {
	private RunLogBizTypeEnum biz;
	private RunCycleEnum cycle;
	private RunLogService service = SpringUtil.getBean("RunLogService", RunLogService.class);
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
		RunLog rl = new RunLog();
		rl.setBtype(biz.bcode);
		rl.setRunCycle(cycle.code);
		rl.setDate(Integer.valueOf(DateUtil.getTodayYYYYMMDD()));
		rl.setStartTime(DateUtil.getTodayYYYYMMDDHHMMSS());
		rl.setStatus(0);
		rl.setRemark(remark);
		rl.setCreateDate(new Date());
		service.addLog(rl);
		try {
			Object result = mycall();
			rl.setStatus(1);
			log.info("正常完成任务：{}", biz.getBtypeName());
			return result;
		} catch (Exception e) {
			e.printStackTrace();
			ErrorLogFileUitl.writeError(e, biz.getBtypeName(), cycle.getName(), remark);
			log.info("执行任务异常：{},remark", biz.getBtypeName(), remark);
			rl.setRemark(remark + e.getMessage());
			rl.setStatus(2);

		} finally {
			rl.setEndTime(DateUtil.getTodayYYYYMMDDHHMMSS());
			service.addLog(rl);
		}
		return null;
	}

	public abstract Object mycall();
}
