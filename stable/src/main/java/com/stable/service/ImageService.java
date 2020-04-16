package com.stable.service;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import javax.annotation.PostConstruct;

import org.apache.commons.lang3.StringUtils;
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

import lombok.extern.log4j.Log4j2;

@Service
@Log4j2
public class ImageService {

	@Autowired
	private DaliyBasicHistroyService daliyBasicHistroyService;
	@Autowired
	private DaliyTradeHistroyService daliyTradeHistroyService;
	@Value("${image.folder}")
	private String imageFolder = "";

	private String PRICE = "PRICE";
	private String Volume = "Volume";

	@Value("${image.standard.p}")
	private String standardImgp = "";
	@Value("${image.standard.v}")
	private String standardImgv = "";
	@Value("${image.chk.p}")
	private double checklinep = 99.0;
	@Value("${image.chk.v}")
	private double checklinev = 99.0;
	@Value("${image.records.size}")
	private int recordsSize = 0;

	private EsQueryPageReq queryPage = null;
	private EsQueryPageReq all = new EsQueryPageReq(10000);

	@PostConstruct
	private void init() {
		queryPage = new EsQueryPageReq(recordsSize);
	}

	public String setCheckParm(String p_standardImgp, String p_standardImgv, double p_checklinep, double p_checklinev,
			int p_recordsSize) {
		if (StringUtils.isBlank(p_standardImgp)) {
			this.standardImgp = p_standardImgp;
		}
		if (StringUtils.isBlank(p_standardImgv)) {
			this.standardImgv = p_standardImgv;
		}
		if (p_checklinep > 0) {
			this.checklinep = p_checklinep;
		}
		if (p_checklinev > 0) {
			this.checklinev = p_checklinev;
		}
		if (recordsSize > 0) {
			this.recordsSize = p_recordsSize;
		}
		return "standardImgp=" + standardImgp + ",standardImgv=" + standardImgv + ",checklinep=" + checklinep
				+ ",checklinev=" + checklinev + ",recordsSize=" + recordsSize;
	}

	public int checkImg(String code, int startDate, int endDate) {
		String img_p = this.genPriceImage(code, startDate, endDate);
		double psimilarity = this.compareImage(img_p, standardImgp);
		log.info("code={},imgpurl={}, price相似度得分={},是否OK={}", code, img_p, psimilarity, (psimilarity >= checklinep));
		if (psimilarity >= checklinep) {
			String img_v = this.genVolumeImage(code, startDate, endDate);
			double vsimilarity = this.compareImage(img_p, standardImgp);
			log.info("code={},imgpurl={}, volume相似度得分={},是否OK={}", code, img_v, vsimilarity,
					(vsimilarity >= checklinev));
			if (vsimilarity >= checklinev) {
				return 2;
			}
			return 1;
		}
		return 0;
	}

	public int checkImg(String code) {
		return checkImg(code, 0, 0);
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
		if (startDate == 0 && endDate == 0) {
			list = daliyTradeHistroyService.queryListByCode(code, 0, 0, queryPage, SortOrder.ASC);
		} else if (startDate == 0 && endDate != 0) {
			list = daliyTradeHistroyService.queryListByCode(code, 0, endDate, queryPage, SortOrder.ASC);
		} else {
			list = daliyTradeHistroyService.queryListByCode(code, startDate, endDate, all, SortOrder.ASC);
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
		if (startDate == 0 && endDate == 0) {
			list = daliyBasicHistroyService.queryListByCode(code, 0, 0, queryPage, SortOrder.ASC);
		} else if (startDate == 0 && endDate != 0) {
			list = daliyBasicHistroyService.queryListByCode(code, 0, endDate, queryPage, SortOrder.ASC);
		} else {
			list = daliyBasicHistroyService.queryListByCode(code, startDate, endDate, all, SortOrder.ASC);
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

	public String getFileName(String code, String typeName, String date) {
		return code + "_" + date + "_" + typeName + ".jpg";
	}

	public String getFileName(String code, String typeName, int date) {
		return code + "_" + date + "_" + typeName + ".jpg";
	}

	private String getFileName(String code, String typeName, Date date) {
		return this.getFileName(code, typeName, DateUtil.formatYYYYMMDD(date));
	}

	private String generateImages(String code, String typeName, List<ImageData> data) {
		String filename = this.getFileName(code, typeName, data.get(data.size() - 1).getDate());
		ImageGeneratingUtil.generateImages(imageFolder + filename, data);
		return filename;
	}

	public double compareImage(String image1, String image2) {
		return Double.valueOf(String.format("%.2f",
				ImageCompareSimilarityUtil.getSimilarity(imageFolder + image1, imageFolder + image2)));
	}
}
