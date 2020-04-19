package com.stable.service.model.image;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import javax.annotation.PostConstruct;

import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.stable.service.DaliyBasicHistroyService;
import com.stable.service.DaliyTradeHistroyService;
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

	private final EsQueryPageReq all = new EsQueryPageReq(10000);
	List<ImageChkGroup> list = new LinkedList<ImageChkGroup>();

	@PostConstruct
	private void load() {
		list = ImageChkGroupUtil.getList();
	}

	public String checkImgByUser(String code, int startDate, int endDate) {
		load();
		return checkImg(code, startDate, endDate);
	}

	public String checkImg(String code, int startDate, int endDate) {
		String str = "";
		for (ImageChkGroup icg : list) {
			int result = 0;
			String img_p = this.genPriceImage(code, startDate, endDate, icg.getRecordsSize());
			double psimilarity = this.compareImage(img_p, icg.getStandardImgp());
			log.info("code={},imgpurl={}, price相似度得分={},是否OK={}", code, img_p, psimilarity,
					(psimilarity >= icg.getChecklinep()));
			if (psimilarity >= icg.getChecklinep()) {
				String img_v = this.genVolumeImage(code, startDate, endDate, icg.getRecordsSize());
				double vsimilarity = this.compareImage(img_p, icg.getStandardImgv());
				log.info("code={},imgpurl={}, volume相似度得分={},是否OK={}", code, img_v, vsimilarity,
						(vsimilarity >= icg.getChecklinev()));
				if (vsimilarity >= icg.getChecklinev()) {
					result = 2;
				}
				result = 1;
			}
			str += icg.getId() + ":" + result;
		}
		return str;
	}

	public String checkImg(String code) {
		return checkImg(code, 0, 0);
	}

	/**
	 * 根据价格生成
	 */
	public String genPriceImage(String code, int startDate, int endDate, int size) {
		List<TradeHistInfoDaliy> list;
		EsQueryPageReq queryPage = null;
		if (startDate != 0 && endDate != 0) {
			queryPage = all;
		} else {
			queryPage = new EsQueryPageReq(size);
		}
		list = daliyTradeHistroyService.queryListByCode(code, startDate, endDate, queryPage, SortOrder.ASC);
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

	public int getSize(String code, int startDate, int endDate) {
		return daliyTradeHistroyService.queryListByCode(code, startDate, endDate, all, SortOrder.ASC).size();
	}

	/**
	 * 根据量生成
	 */
	public String genVolumeImage(String code, int startDate, int endDate, int size) {
		List<DaliyBasicInfo> list;
		EsQueryPageReq queryPage = null;
		if (startDate != 0 && endDate != 0) {
			queryPage = all;
		} else {
			queryPage = new EsQueryPageReq(size);
		}
		list = daliyBasicHistroyService.queryListByCode(code, startDate, endDate, queryPage, SortOrder.ASC);
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
