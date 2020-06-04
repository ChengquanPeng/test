package com.stable.service.model;

import com.stable.service.model.data.LineAvgPrice;
import com.stable.service.model.data.LinePrice;
import com.stable.service.model.data.LineTickData;
import com.stable.service.model.data.LineVol;
import com.stable.vo.ModelContext;

public interface StrategyListener {

	public void fulshToFile();

	public void processingModelResult(ModelContext cxt, LineAvgPrice lineAvgPrice, LinePrice linePrice, LineVol lineVol,
			LineTickData lineTickData);

}
