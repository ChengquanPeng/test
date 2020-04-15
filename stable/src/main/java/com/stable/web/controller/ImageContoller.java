package com.stable.web.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.stable.service.ImageService;
import com.stable.vo.http.JsonResult;

@RequestMapping("/image")
@RestController
public class ImageContoller {

	@Autowired
	private ImageService imageService;

	/**
	 * 根据code 生成图片，startDate=0,默认250个交易日,价格：type=1,量：type=2
	 */
	@RequestMapping(value = "/generate")
	public ResponseEntity<JsonResult> generate(String code, int startDate, int endDate, int type) {
		JsonResult r = new JsonResult();
		try {
			r.setStatus(JsonResult.OK);
			if (type == 1) {
				r.setResult(imageService.genPriceImage(code, startDate, endDate));
			} else if (type == 2) {
				r.setResult(imageService.genVolumeImage(code, startDate, endDate));
			}
		} catch (Exception e) {
			r.setResult(e.getClass().getName() + ":" + e.getMessage());
			r.setStatus(JsonResult.ERROR);
			e.printStackTrace();
		}
		return ResponseEntity.ok(r);
	}

	/**
	 * compare比较两张图片
	 */
	@RequestMapping(value = "/compare")
	public ResponseEntity<JsonResult> compare(String image1, String image2) {
		JsonResult r = new JsonResult();
		try {
			r.setStatus(JsonResult.OK);
			r.setResult(imageService.compareImagee(image1, image2));
		} catch (Exception e) {
			r.setResult(e.getClass().getName() + ":" + e.getMessage());
			r.setStatus(JsonResult.ERROR);
			e.printStackTrace();
		}
		return ResponseEntity.ok(r);
	}
}
