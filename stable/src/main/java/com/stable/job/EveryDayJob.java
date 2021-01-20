package com.stable.job;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.dangdang.ddframe.job.api.ShardingContext;
import com.stable.config.SpringConfig;
import com.stable.service.BuyBackService;
import com.stable.service.model.CodeModelService;
import com.stable.utils.DateUtil;
import com.stable.utils.FileDeleteUitl;
import com.stable.utils.SpringUtil;

import lombok.extern.log4j.Log4j2;

/**
 * 周一到周五执行的任务 18:00
 * 
 */
@Component
@Log4j2
public class EveryDayJob extends MySimpleJob {

	@Autowired
	private BuyBackService buyBackService;
	@Autowired
	private CodeModelService codeModelService;

	@Override
	public void myexecute(ShardingContext sc) {
		log.info("回购公告");
		buyBackService.jobFetchHistEveryDay();

		log.info("过期文件的删除");
		SpringConfig efc = SpringUtil.getBean(SpringConfig.class);
		FileDeleteUitl.deletePastDateFile(efc.getModelImageFloder());
		FileDeleteUitl.deletePastDateFile(efc.getModelV1SortFloder());
		FileDeleteUitl.deletePastDateFile(efc.getModelV1SortFloderDesc());
		try {
			Thread.sleep(Duration.ofMinutes(5).toMillis());
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		// codeAttentionService.fetchAll();
		codeModelService.runJob(true, Integer.valueOf(DateUtil.getTodayYYYYMMDD()));
	}
}
