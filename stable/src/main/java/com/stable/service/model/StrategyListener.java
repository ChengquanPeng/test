package com.stable.service.model;

import java.util.List;

import com.stable.service.model.data.LineAvgPrice;
import com.stable.service.model.data.LinePrice;
import com.stable.service.model.data.LineTickData;
import com.stable.service.model.data.LineVol;
import com.stable.vo.ModelContext;
import com.stable.vo.bus.Monitoring;
import com.stable.vo.up.strategy.ModelV1;

public interface StrategyListener {

	public void fulshToFile();

	public void processingModelResult(ModelContext cxt, LineAvgPrice lineAvgPrice, LinePrice linePrice, LineVol lineVol,
			LineTickData lineTickData);

	public List<ModelV1> getResultList();

	public default List<Monitoring> getMonitoringList() {
		return null;
	}
}
