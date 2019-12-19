package com.stable.job;

import java.util.concurrent.Callable;

import com.stable.enums.RunCycleEnum;
import com.stable.enums.RunLogBizTypeEnum;
import com.stable.service.RunLogService;
import com.stable.utils.DateUtil;
import com.stable.utils.SpringUtil;
import com.stable.vo.bus.RunLog;

public abstract class MyCallable implements Callable<Object> {
	private RunLogBizTypeEnum biz;
	private RunCycleEnum cycle;
	private RunLogService service = SpringUtil.getBean(RunLogService.class);

	public MyCallable(RunLogBizTypeEnum biz, RunCycleEnum cycle) {
		this.biz = biz;
		this.cycle = cycle;
	}

	@Override
	public Object call() throws Exception {
		RunLog rl = new RunLog();
		rl.setBtype(biz.bcode);
		rl.setRunCycle(cycle.code);
		rl.setDate(Integer.valueOf(DateUtil.getTodayYYYYMMDD()));
		rl.setStartTime(DateUtil.getTodayYYYYMMDDHHMMSS());
		rl.setStatus(0);
		service.addLog(rl);
		try {
			Object result = mycall();
			rl.setStatus(1);
			return result;
		} catch (Exception e) {
			e.printStackTrace();
			rl.setRemark(e.getMessage());
			rl.setStatus(2);
		} finally {
			rl.setEndTime(DateUtil.getTodayYYYYMMDDHHMMSS());
			service.addLog(rl);
		}
		return null;
	}

	public abstract Object mycall();
}
