package com.stable.utils;

import java.io.IOException;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.stable.constant.Constant;

public class HttpUtil {

	public static JSONObject doGet(String url) {
		CloseableHttpClient httpclient = HttpClientBuilder.create().build();
		HttpGet httpget = new HttpGet(url);
		JSONObject jsonObj = null;
		try {
			HttpResponse response = httpclient.execute(httpget);
			HttpEntity entity = response.getEntity();
			if (entity != null) {
				String result = EntityUtils.toString(entity, Constant.UTF_8);
				jsonObj = JSON.parseObject(result);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

		return jsonObj;
	}

	public static JSONObject doPost(String url, String json) {
		CloseableHttpClient httpclient = HttpClientBuilder.create().build();
		HttpPost httpPost = new HttpPost(url);
		JSONObject jsonObj = null;
		try {
			httpPost.setEntity(new StringEntity(json, Constant.UTF_8));
			HttpResponse response = httpclient.execute(httpPost);
			HttpEntity entity = response.getEntity();
			if (entity != null) {
				String result = EntityUtils.toString(entity, Constant.UTF_8);
				jsonObj = JSON.parseObject(result);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return jsonObj;
	}
}