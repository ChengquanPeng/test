package com.stable.utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.LinkedList;

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

	public static String doGet2(String url) {
		CloseableHttpClient httpclient = HttpClientBuilder.create().build();
		HttpGet httpget = new HttpGet(url);
		try {
			HttpResponse response = httpclient.execute(httpget);
			HttpEntity entity = response.getEntity();
			if (entity != null) {
				String result = EntityUtils.toString(entity, Constant.UTF_8);
				if (result != null) {
					result = result.substring(result.indexOf("<title>"), result.lastIndexOf("</title>"));
					if (result != null && result.length() > 0) {
						if (!result.contains("找不到网页")) {
							return result;
						}
					}
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

		return null;
	}

	private static final File file = new File("F:\\Downloads\\vdio\\urls.log");
	private static FileOutputStream fos;
	private static FileChannel channel;
	private static final String LINE = System.getProperty("line.separator");
	static {
		if (!file.exists()) {
			try {
				file.createNewFile();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		try {
			fos = new FileOutputStream(file, true);
			channel = fos.getChannel();
		} catch (IOException e) {
			e.printStackTrace();
		}

		Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					channel.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
				try {
					fos.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}));
	}

	public static void writeStringLine(LinkedList<String> list) {
		try {
			StringBuffer sb = new StringBuffer();
			for (String s : list) {
				sb.append(LINE);
				sb.append(s);
			}
			// System.err.println(s.toString());
			ByteBuffer buf = ByteBuffer.wrap(sb.toString().getBytes());
			// System.err.println(s.toString());
			buf.put(sb.toString().getBytes());
			buf.flip();
			channel.write(buf);
		} catch (Exception e1) {
			e1.printStackTrace();
		}
	}

	public static void main(String[] args) throws InterruptedException {
		String urlb = "https://yuese74.com/embed/";
		String s = null;
		String r = null;
		LinkedList<String> list = new LinkedList<String>();
		for (int i = 20001; i < 30000; i++) {
			s = urlb + i;
			r = HttpUtil.doGet2(s);
			if (r != null) {
				System.err.println(r);
				list.add(s + "" + r);
			}
			if (i % 100 == 0) {
				//Thread.sleep(1 * 1000);
				HttpUtil.writeStringLine(list);
				list = new LinkedList<String>();
			}
		}
		HttpUtil.writeStringLine(list);
	}
}