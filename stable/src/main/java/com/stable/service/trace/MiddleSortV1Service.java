package com.stable.service.trace;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.stable.service.CodePoolService;
import com.stable.service.model.data.AvgService;
import com.stable.service.model.data.LineAvgPrice;
import com.stable.vo.bus.CodeBaseModel;
import com.stable.vo.bus.CodePool;

import lombok.extern.log4j.Log4j2;

/**
 * 基本面趋势票
 *
 */
@Service
@Log4j2
public class MiddleSortV1Service {
	@Autowired
	private CodePoolService codePoolService;
	@Autowired
	private AvgService avgService;

	private String OK = "系统默认OK";
	private String NOT_OK = "系统默认NOT_OK";

	public synchronized void start(List<CodeBaseModel> listLast) {
		Map<String, CodePool> map = codePoolService.getCodePoolMap();
		List<CodePool> list = new LinkedList<CodePool>();
		LineAvgPrice lvp = new LineAvgPrice(avgService);
		log.info("codelist:" + listLast.size());
		for (CodeBaseModel m : listLast) {
			String code = m.getCode();
			CodePool c = map.get(code);
			if (c == null) {
				c = new CodePool();
				c.setCode(code);
			}
			c.setMidChkDate(m.getDate());
			c.setInMid(0);
			c.setMidRemark(NOT_OK);
			// 业绩支撑
			if (m.getMidOk() == 1) {
				// 均线支持
				if (lvp.isWhiteHorseForMid(code, m.getDate())) {
					c.setInMid(1);
					c.setMidRemark(OK);
				}
			}
			list.add(c);
		}
		codePoolService.saveAll(list);
	}

}
