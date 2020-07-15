package com.stable.service.model.image;

import java.io.File;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.stable.service.DaliyBasicHistroyService;
import com.stable.service.DaliyTradeHistroyService;
import com.stable.utils.DateUtil;
import com.stable.utils.ImageCompareSimilarityUtil;
import com.stable.utils.ImageGeneratingByPythonUtil;
import com.stable.vo.ImageData;
import com.stable.vo.bus.DaliyBasicInfo;
import com.stable.vo.bus.TradeHistInfoDaliy;
import com.stable.vo.spi.req.EsQueryPageReq;

import lombok.extern.log4j.Log4j2;

@Service
@Log4j2
public class ImageService {

	public static final String MATCH_L1 = "L1";
	public static final String MATCH_L2 = "L2";
	private static final String PIC = ".png";
	@Autowired
	private DaliyBasicHistroyService daliyBasicHistroyService;
	@Autowired
	private DaliyTradeHistroyService daliyTradeHistroyService;
	@Value("${image.folder}")
	public String imageFolder = "";

	private String PRICE = "PRICE";
	private String Volume = "Volume";

	private final EsQueryPageReq all = new EsQueryPageReq(10000);
	List<ImageChkGroup> list = new LinkedList<ImageChkGroup>();

	@Autowired
	private ImageGeneratingByPythonUtil imageGeneratingByPythonUtil;

	// @PostConstruct //暂时去掉
	private void load() {
		list = ImageChkGroupUtil.getList();
	}

	public String checkImgByUser(String code, int startDate, int endDate) {
		load();
		return checkImg(code, startDate, endDate, false);
	}

	// 返回格式： index1:1,index2:1,index3:2,
	public String checkImg(String code, int startDate, int endDate, boolean delete) {
		String str = "";
		String img_p = null;
		String img_v = null;
		for (ImageChkGroup icg : list) {
			if (StringUtils.isBlank(img_p)) {
				img_p = this.genPriceImage(code, startDate, endDate, icg.getRecordsSize());
			}
			double psimilarity = this.compareImage(imageFolder + img_p, icg.getPixels2p());
			log.info("P标准std image id={},code={},imgpurl={}, price相似度得分={},是否OK={}", icg.getId(), code, img_p,
					psimilarity, (psimilarity >= icg.getChecklinep()));
			if (psimilarity >= icg.getChecklinep()) {
				if (StringUtils.isBlank(img_v)) {
					img_v = this.genVolumeImage(code, startDate, endDate, icg.getRecordsSize());
				}
				double vsimilarity = this.compareImage(imageFolder + img_v, icg.getPixels2v());
				log.info("V标准std image id={}, code={},imgpurl={}, volume相似度得分={},是否OK={}", icg.getId(), code, img_v,
						vsimilarity, (vsimilarity >= icg.getChecklinev()));

				String result = MATCH_L1;
				if (vsimilarity >= icg.getChecklinev()) {
					result = MATCH_L2;
				}

				str += "index" + icg.getId() + ":" + result + ",";
			}
		}
		if (delete) {
			if (StringUtils.isNotBlank(img_p)) {
				deleteFile(img_p);
			}
			if (StringUtils.isNotBlank(img_v)) {
				deleteFile(img_v);
			}
		}
		return str;
	}

	public String checkImg(String code, int endDate) {
		return checkImg(code, 0, endDate, true);
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
		list = daliyTradeHistroyService.queryListByCode(code, startDate, endDate, queryPage, SortOrder.DESC);
		if (list == null || list.size() <= 0) {
			return null;
		}
		List<ImageData> data = new LinkedList<ImageData>();
		list = list.stream().sorted(Comparator.comparing(TradeHistInfoDaliy::getDate)).collect(Collectors.toList());
		for (TradeHistInfoDaliy r : list) {
			ImageData id = new ImageData();
			id.setDate(DateUtil.parseDate(r.getDate()));
			id.setNum(r.getClosed());
			data.add(id);
		}
		return generateImages(code, PRICE, data);
	}

	public int getSize(String code, int startDate, int endDate) {
		return daliyTradeHistroyService.queryListByCode(code, startDate, endDate, all, SortOrder.DESC).size();
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
		list = daliyBasicHistroyService.queryListByCode(code, startDate, endDate, queryPage, SortOrder.DESC);
		if (list == null || list.size() <= 0) {
			return null;
		}
		list = list.stream().sorted(Comparator.comparing(DaliyBasicInfo::getTrade_date)).collect(Collectors.toList());
		List<ImageData> data = new LinkedList<ImageData>();
		for (DaliyBasicInfo r : list) {
			ImageData id = new ImageData();
			id.setDate(DateUtil.parseDate(r.getTrade_date()));
			id.setNum(r.getTurnover_rate_f());
			data.add(id);
		}
		return generateImages(code, Volume, data);
	}

	public String getFileName(String code, String typeName, String date) {
		return code + "_" + date + "_" + typeName + PIC;
	}

	public String getFileName(String code, String typeName, int date) {
		return code + "_" + date + "_" + typeName + PIC;
	}

	private String getFileName(String code, String typeName, Date date) {
		return this.getFileName(code, typeName, DateUtil.formatYYYYMMDD(date));
	}

	private void deleteFile(String fileName) {
		new File(imageFolder + fileName).delete();
	}

	private String generateImages(String code, String typeName, List<ImageData> data) {
		String filename = this.getFileName(code, typeName, data.get(data.size() - 1).getDate());
//		ImageGeneratingUtil.generateImages(imageFolder + filename, data);
		imageGeneratingByPythonUtil.generateImages(imageFolder + filename, data);
		return filename;
	}

	public double compareImageWithoutPath(String image1, String image2) {
		return compareImage(imageFolder + image1, imageFolder + image2);
	}

	public double compareImage(String image1, int[] pixels2) {
		return Double.valueOf(String.format("%.2f", ImageCompareSimilarityUtil.getSimilarity(image1, pixels2)));
	}

	public double compareImage(String image1, String image2) {
		return Double.valueOf(String.format("%.2f", ImageCompareSimilarityUtil.getSimilarity(image1, image2)));
	}
}
