package com.stable.service;

import java.util.LinkedList;
import java.util.List;

import javax.annotation.PostConstruct;

import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.stable.utils.DateUtil;
import com.stable.utils.ImageCompareSimilarityUtil;
import com.stable.utils.ImageGeneratingUtil;
import com.stable.vo.ImageData;
import com.stable.vo.bus.DaliyBasicInfo;
import com.stable.vo.bus.TradeHistInfoDaliy;
import com.stable.vo.spi.req.EsQueryPageReq;

@Service
public class ImageService {

	@Autowired
	private DaliyBasicHistroyService daliyBasicHistroyService;
	@Autowired
	private DaliyTradeHistroyService daliyTradeHistroyService;
	@Value("${image.folder}")
	private String imageFolder = "";
	@Value("${image.records.size}")
	private int recordsSize = 0;
	private String PRICE = "PRICE";
	private String Volume = "Volume";

	private EsQueryPageReq queryPage = null;

	@PostConstruct
	private void init() {
		queryPage = new EsQueryPageReq(recordsSize);
	}

	public String genPriceImage(String code) {
		return this.genPriceImage(code, 0, 0);
	}

	public String genVolumeImage(String code) {
		return this.genVolumeImage(code, 0, 0);
	}

	/**
	 * 根据价格生成
	 */
	public String genPriceImage(String code, int startDate, int endDate) {
		List<TradeHistInfoDaliy> list;
		if (startDate == 0) {
			list = daliyTradeHistroyService.queryListByCode(code, queryPage, SortOrder.ASC);
		} else {
			list = daliyTradeHistroyService.queryListByCode(code, startDate, endDate, SortOrder.ASC);
		}
		if (list == null || list.size() <= 0) {
			return null;
		}
		List<ImageData> data = new LinkedList<ImageData>();
		for (TradeHistInfoDaliy r : list) {
			ImageData id = new ImageData();
			id.setDate(DateUtil.parseDate(r.getDate()));
			id.setNum(r.getClosed());
			data.add(id);
		}
		return generateImages(code, PRICE, data);
	}

	/**
	 * 根据量生成
	 */
	public String genVolumeImage(String code, int startDate, int endDate) {
		List<DaliyBasicInfo> list;
		if (startDate == 0) {
			list = daliyBasicHistroyService.queryListByCode(code, null, null, queryPage, SortOrder.ASC).getContent();
		} else {
			list = daliyBasicHistroyService.queryListByCode(code, startDate, endDate, SortOrder.ASC);
		}
		if (list == null || list.size() <= 0) {
			return null;
		}
		List<ImageData> data = new LinkedList<ImageData>();
		for (DaliyBasicInfo r : list) {
			ImageData id = new ImageData();
			id.setDate(DateUtil.parseDate(r.getTrade_date()));
			id.setNum(r.getVol());
			data.add(id);
		}
		return generateImages(code, Volume, data);
	}

	private String generateImages(String code, String typeName, List<ImageData> data) {
		String filename = code + "_" + DateUtil.formatYYYYMMDD(data.get(data.size() - 1).getDate()) + "_" + typeName
				+ ".jpg";
		ImageGeneratingUtil.generateImages(imageFolder + filename, data);
		return filename;
	}

	public String compareImagee(String image1, String image2) {
		return String.format("%.2f",
				ImageCompareSimilarityUtil.getSimilarity(imageFolder + image1, imageFolder + image2));
	}
}
