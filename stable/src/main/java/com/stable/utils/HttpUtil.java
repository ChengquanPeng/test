package com.stable.utils;

import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultHttpRequestRetryHandler;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.util.EntityUtils;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;

public class HttpUtil {
	private static final String APPLICATION_JSON = "application/json";
	private static final String CONTENT_TYPE = "Content-Type";
	private static final String AUTHORIZATION = "Authorization";
	private static final String AUTHORIZATION_VALUE = "Basic bm9hdXRoOm5vYXV0aA==";
	static final String UTF_8 = "UTF-8";
	private static PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();

	private static final CloseableHttpClient httpclient;

	static {
		int tot = 500;
		connectionManager.setMaxTotal(tot);
		connectionManager.setDefaultMaxPerRoute(10);
		System.setProperty("http.maxConnections", tot + "");
		System.setProperty("http.keepAlive", "true");

		// 创建http客户端
		httpclient = HttpClients.custom().useSystemProperties().setConnectionManager(connectionManager)
				.setRetryHandler(new DefaultHttpRequestRetryHandler(3, true))
				.setDefaultRequestConfig(RequestConfig.custom().build()).build();
		// 初始化httpGet
	}

	public static JSONObject doGet(String url) {
		JSONObject jsonObj = null;
		try {
			HttpGet httpget = new HttpGet();
			httpget.setURI(URI.create(url));
			HttpResponse response = httpclient.execute(httpget);
			HttpEntity entity = response.getEntity();
			if (entity != null) {
				String result = EntityUtils.toString(entity, UTF_8);
				// System.err.println(result);
				jsonObj = JSON.parseObject(result);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return jsonObj;
	}

	public static String doGet2(String url) {
		try {
			HttpGet httpget = new HttpGet();
			httpget.setURI(URI.create(url));
			HttpResponse response = httpclient.execute(httpget);
			HttpEntity entity = response.getEntity();
			if (entity != null) {
				return EntityUtils.toString(entity, UTF_8);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return "";
	}

	public static String doGet2(String url, Map<String, String> header) {
		try {
			HttpGet httpget = new HttpGet();
			httpget.setURI(URI.create(url));
			if (header != null) {
				header.keySet().forEach(key -> {
					httpget.setHeader(key, header.get(key));
				});
			}
			HttpResponse response = httpclient.execute(httpget);
			HttpEntity entity = response.getEntity();
			if (entity != null) {
				return EntityUtils.toString(entity, UTF_8);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	public static void main(String[] args) {
		Map<String, String> header = new HashMap<String, String>();
		header.put("Referer", "http://q.10jqka.com.cn/gn/detail/code/308458/");
		header.put("Host", "d.10jqka.com.cn");
		header.put("User-Agent",
				"Mozilla/5.0 (Linux; Android 6.0; Nexus 5 Build/MRA58N) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/81.0.4044.138 Mobile Safari/537.36");
		String url = "http://d.10jqka.com.cn/v4/line/bk_885869/01/last.js";
		String resp = HttpUtil.doGet2(url, header);
		System.err.println(resp);
		String jsonstr = resp.substring(resp.indexOf("last")).replaceFirst("last", "");
		jsonstr = jsonstr.substring(1, jsonstr.length() - 1);
		// String jsonstr =
		// "{'rt':'0930-1130,1300-1500','num':120,'total':120,'start':'20191115','year':{'2019':33,'2020':87},'name':'\\u534a\\u5e74\\u62a5\\u9884\\u589e','data':'20191115,998.425,998.952,985.080,985.815,632050700,8775591400.000,,,,0;20191118,983.094,995.178,980.196,993.282,581209190,7827247700.000,,,,0;20191119,993.147,1013.170,992.726,1013.170,642907820,8903463300.000,,,,0;20191120,1012.649,1014.893,1003.517,1004.849,671548840,9458597900.000,,,,0;20191121,1001.063,1008.015,998.581,1004.772,562051210,7345254200.000,,,,0;20191122,1005.762,1013.234,987.461,993.954,836082190,10760132400.000,,,,0;20191125,991.593,992.930,979.166,989.293,758772290,8492961100.000,,,,0;20191126,988.376,990.360,981.912,986.238,662119770,8274986000.000,,,,0;20191127,982.815,991.677,981.958,987.774,507087910,6963322200.000,,,,0;20191128,987.638,992.678,983.954,985.813,496982150,6956426800.000,,,,0;20191129,985.850,989.461,977.816,987.217,513104180,6884285500.000,,,,0;20191202,988.743,989.433,982.210,985.814,458495650,6482057400.000,,,,0;20191203,980.651,989.318,976.243,989.318,629479190,8659851800.000,,,,0;20191204,983.497,990.671,981.897,989.804,455668550,6198564900.000,,,,0;20191205,990.790,1005.238,990.790,1004.129,515149200,6737707400.000,,,,0;20191206,1003.560,1011.303,1001.707,1011.303,525591500,7192575800.000,,,,0;20191209,1012.531,1017.182,1010.981,1016.674,596974210,8518198200.000,,,,0;20191210,1015.025,1020.002,1010.747,1020.002,620573490,8286518800.000,,,,0;20191211,1019.460,1022.649,1014.431,1019.447,696900840,10032553900.000,,,,0;20191212,1019.492,1026.518,1017.695,1018.601,756181500,9516933500.000,,,,0;20191213,1022.895,1032.582,1021.281,1032.354,729149960,9804669800.000,,,,0;20191216,1035.332,1053.548,1035.117,1053.548,873594380,10914075500.000,,,,0;20191217,1055.954,1070.606,1054.354,1067.337,1089941190,15062319000.000,,,,0;20191218,1067.129,1076.665,1065.755,1069.855,1084060250,16166200000.000,,,,0;20191219,1069.346,1072.020,1062.224,1069.876,939046450,13220263300.000,,,,0;20191220,1074.118,1078.339,1063.537,1064.387,854134930,10686612900.000,,,,0;20191223,1063.089,1066.110,1047.673,1049.136,864736310,11317275500.000,,,,0;20191224,1049.215,1067.448,1049.022,1067.448,757764940,9100475600.000,,,,0;20191225,1067.437,1071.290,1064.240,1070.127,851933000,13355006000.000,,,,0;20191226,1069.185,1079.538,1068.872,1079.538,925349530,12144649700.000,,,,0;20191227,1080.584,1083.635,1065.547,1066.595,969926550,14520021000.000,,,,0;20191230,1062.121,1071.256,1047.045,1071.256,894045360,12397021900.000,,,,0;20191231,1070.940,1081.596,1068.983,1081.596,840997300,12169173900.000,,,,0;20200102,1089.074,1102.575,1085.580,1102.498,1049459030,15403160000.000,,,,0;20200103,1106.495,1113.587,1101.650,1110.213,1225701300,17728013000.000,,,,0;20200106,1103.618,1128.403,1101.703,1121.963,1586550800,21911889000.000,,,,0;20200107,1126.279,1141.917,1126.279,1139.013,1711729300,25137453000.000,,,,0;20200108,1136.495,1141.126,1119.630,1121.392,1691986900,23232547000.000,,,,0;20200109,1135.225,1145.323,1135.225,1145.323,1519951300,21477721000.000,,,,0;20200110,1153.998,1154.391,1143.225,1147.177,1445433200,21087835000.000,,,,0;20200113,1151.107,1172.019,1148.551,1172.019,1910273700,30193467000.000,,,,0;20200114,1183.580,1183.889,1172.884,1173.919,2207974000,33690948000.000,,,,0;20200115,1175.467,1178.440,1166.498,1176.905,2166178600,32971478000.000,,,,0;20200116,1178.817,1179.729,1173.018,1175.232,2590477500,35832865000.000,,,,0;20200117,1179.902,1185.266,1172.386,1173.489,2855158800,41113782000.000,,,,0;20200120,1177.097,1192.017,1172.722,1192.017,3898240400,62964256000.000,,,,0;20200121,1191.669,1191.669,1179.565,1179.711,5384165700,80111506000.000,,,,0;20200122,1173.270,1191.171,1158.576,1187.261,6733932400,102459239000.000,,,,0;20200123,1177.741,1186.363,1137.566,1149.253,9097952100,129809234000.000,,,,0;20200203,1042.298,1050.349,1042.298,1046.052,4568106400,67377585000.000,,,,0;20200204,986.766,1054.712,986.766,1050.900,11680696800,147488950000.000,,,,0;20200205,1056.770,1090.848,1056.770,1077.110,10121340600,141780860000.000,,,,0;20200206,1079.789,1113.453,1073.706,1108.491,10596691100,146622940000.000,,,,0;20200207,1111.569,1123.049,1100.108,1122.367,10850244800,154078530000.000,,,,0;20200210,1121.246,1143.220,1119.159,1143.107,10588930100,150404930000.000,,,,0;20200211,1142.673,1145.844,1130.682,1137.766,8955880400,125169608000.000,,,,0;20200212,1134.965,1157.805,1134.674,1157.634,9875817300,134801010000.000,,,,0;20200213,1156.515,1161.385,1141.635,1146.117,9665511100,141336100000.000,,,,0;20200214,1142.991,1160.044,1142.932,1152.229,9618481600,141319120000.000,,,,0;20200217,1159.672,1192.627,1159.672,1192.627,11803985000,167716590000.000,,,,0;20200218,1194.536,1214.183,1190.847,1214.183,11523953800,167165330000.000,,,,0;20200219,1211.424,1222.534,1206.226,1206.226,11669594800,171359510000.000,,,,0;20200220,1209.268,1233.781,1201.392,1233.513,13356908400,192260920000.000,,,,0;20200221,1232.681,1258.303,1231.456,1248.329,14370483000,208888900000.000,,,,0;20200224,1247.429,1273.846,1245.173,1270.501,13211469200,198187750000.000,,,,0;20200225,1242.245,1271.868,1214.668,1269.210,16845335000,248695600000.000,,,,0;20200226,1251.265,1272.056,1240.118,1244.451,15772868000,224629120000.000,,,,0;20200227,1252.055,1265.688,1236.048,1253.660,12839672900,187716410000.000,,,,0;20200228,1211.545,1229.259,1183.544,1184.158,13866745000,193124310000.000,,,,0;20200302,1198.082,1244.218,1198.082,1240.053,12711212400,178794930000.000,,,,0;20200303,1260.711,1277.567,1242.584,1255.350,14571269000,207201240000.000,,,,0;20200304,1245.844,1268.460,1241.611,1268.460,12560361100,178070970000.000,,,,0;20200305,1285.038,1290.784,1275.567,1288.289,15353974000,219096850000.000,,,,0;20200306,1272.711,1290.811,1270.363,1281.220,12914797000,173011480000.000,,,,0;20200309,1261.974,1269.872,1235.123,1235.204,14089911000,192436180000.000,,,,0;20200310,1215.759,1263.643,1202.948,1263.643,13531006000,190016350000.000,,,,0;20200311,1266.490,1271.931,1243.865,1243.865,11477611700,160350430000.000,,,,0;20200312,1224.036,1230.866,1203.365,1215.080,9655452200,130508892000.000,,,,0;20200313,1151.131,1209.673,1150.459,1199.448,10947782100,148042960000.000,,,,0;20200316,1211.353,1212.727,1142.293,1145.446,11154380600,150009010000.000,,,,0;20200317,1153.795,1165.003,1105.675,1147.841,9738680800,126338168000.000,,,,0;20200318,1159.339,1177.999,1130.359,1130.359,9772616300,133280943000.000,,,,0;20200319,1128.360,1147.227,1107.699,1142.912,9330854700,125563640000.000,,,,0;20200320,1154.904,1157.490,1136.491,1156.824,7825631200,107659690000.000,,,,0;20200323,1125.901,1139.639,1105.871,1109.541,7714466900,100966647000.000,,,,0;20200324,1130.128,1134.405,1099.650,1132.027,8005758600,107977457000.000,,,,0;20200325,1155.492,1160.292,1146.389,1159.854,8368375600,114864234000.000,,,,0;20200326,1152.300,1159.708,1145.586,1147.404,7190627000,96942843000.000,,,,0;20200327,1159.859,1163.922,1143.242,1144.319,7174112900,95343897000.000,,,,0;20200330,1128.830,1131.076,1110.027,1122.773,7277431000,96321643000.000,,,,0;20200331,1133.489,1136.031,1121.987,1125.172,6663605800,90922454000.000,,,,0;20200401,1121.628,1138.054,1117.047,1119.196,6529983200,91151841000.000,,,,0;20200402,1114.407,1146.326,1112.840,1146.326,6756003900,96082662000.000,,,,0;20200403,1144.083,1149.509,1133.857,1139.200,6208177700,87856837000.000,,,,0;20200407,1158.451,1178.347,1158.002,1177.855,8818526100,122314529000.000,,,,0;20200408,1173.882,1185.111,1171.239,1182.154,8098114800,107400535000.000,,,,0;20200409,1187.685,1197.431,1185.225,1195.204,8041080900,108195433000.000,,,,0;20200410,1194.283,1194.283,1163.582,1167.684,7906732100,107813034000.000,,,,0;20200413,1160.040,1165.118,1150.905,1159.179,6228630200,84227433000.000,,,,0;20200414,1165.268,1181.631,1163.930,1181.631,7156170200,98441139000.000,,,,0;20200415,1182.139,1186.588,1173.441,1174.149,7180216800,101251092000.000,,,,0;20200416,1167.478,1182.307,1164.869,1180.153,6581094100,93878412000.000,,,,0;20200417,1186.892,1192.284,1181.065,1183.138,7729996800,116909678000.000,,,,0;20200420,1185.814,1198.176,1182.285,1198.176,7196577400,98126631000.000,,,,0;20200421,1193.697,1193.697,1179.635,1192.495,7574478100,99492784000.000,,,,0;20200422,1184.427,1202.462,1181.740,1202.462,6826934100,91956366000.000,,,,0;20200423,1206.203,1206.483,1195.868,1196.569,7447006800,95649525000.000,,,,0;20200424,1196.416,1196.530,1173.071,1177.107,7989885400,93916448000.000,,,,0;20200427,1179.065,1184.428,1169.872,1173.852,6877048700,89991967000.000,,,,0;20200428,1172.763,1173.132,1123.171,1154.703,8289345400,108940393000.000,,,,0;20200429,1149.966,1162.558,1147.925,1153.396,6302471200,84718549000.000,,,,0;20200430,1158.680,1180.510,1158.680,1177.374,7925677400,110736092000.000,,,,0;20200506,1166.311,1197.188,1165.767,1197.188,7588670900,113275920000.000,,,,0;20200507,1198.721,1201.109,1191.433,1194.722,7463812700,104854140000.000,,,,0;20200508,1200.226,1211.560,1199.374,1207.706,7688045200,107656366000.000,,,,0;20200511,1211.116,1214.875,1199.399,1206.593,7352916300,108138503000.000,,,,0;20200512,1206.962,1207.411,1190.619,1207.411,6429712500,91603043000.000,,,,0;20200513,1203.683,1215.755,1199.932,1214.719,6835274000,93468110000.000,,,,0;20200514,1204.366,1213.053,1193.433,1194.068,767416220,8227488000.000,,,,0;20200515,1198.711,1207.099,1192.043,1194.967,724070860,7855980100.000,,,,0','marketType':'','issuePrice':'1000.000','today':'20200517'}";
		System.err.println(jsonstr);
		String data = JSON.parseObject(jsonstr).getString("data");
		System.err.println(data);
		String[] datas = data.split(";");
		Arrays.asList(datas).forEach(x -> {
//			ConceptDaily cd = new ConceptDaily();
//			String[] dd = x.split(",");
//			cd.setDate(Integer.valueOf(dd[0]));
//			cd.setOpen(Double.valueOf(dd[1]));
//			cd.setHigh(Double.valueOf(dd[2]));
//			cd.setLow(Double.valueOf(dd[3]));
//			cd.setClose(Double.valueOf(dd[4]));
//			cd.setVol(Long.valueOf(dd[5]));
//			cd.setAmt(Double.valueOf(dd[6]).longValue());
//			System.err.println(cd);
		});

	}

	public static JSONObject doGet3(String url, Map<String, String> header) {
		HttpGet httpget = new HttpGet();
		httpget.setURI(URI.create(url));
		JSONObject jsonObj = null;
		try {
			if (header != null) {
				header.keySet().forEach(key -> {
					httpget.setHeader(key, header.get(key));
				});
			}

			HttpResponse response = httpclient.execute(httpget);
			HttpEntity entity = response.getEntity();
			if (entity != null) {
				String result = EntityUtils.toString(entity, UTF_8);
				jsonObj = JSON.parseObject(result);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return jsonObj;
	}

	public static String doGet3_1(String url, Map<String, String> header) {
		HttpGet httpget = new HttpGet();
		httpget.setURI(URI.create(url));
		try {
			if (header != null) {
				header.keySet().forEach(key -> {
					httpget.setHeader(key, header.get(key));
				});
			}

			HttpResponse response = httpclient.execute(httpget);
			HttpEntity entity = response.getEntity();
			if (entity != null) {
				return EntityUtils.toString(entity, UTF_8);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return "";
	}

	public static JSONObject doPost(String url) {
		HttpPost httpPost = new HttpPost();
		httpPost.setURI(URI.create(url));
		JSONObject jsonObj = null;
		try {
			httpPost.setHeader(CONTENT_TYPE, APPLICATION_JSON);
			httpPost.setHeader(AUTHORIZATION, AUTHORIZATION_VALUE);
			HttpResponse response = httpclient.execute(httpPost);
			HttpEntity entity = response.getEntity();
			if (entity != null) {
				String result = EntityUtils.toString(entity, UTF_8);
				jsonObj = JSON.parseObject(result);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return jsonObj;
	}

	public static JSONObject doPost(String url, String json) {
		HttpPost httpPost = new HttpPost();
		httpPost.setURI(URI.create(url));
		JSONObject jsonObj = null;
		try {
			httpPost.setHeader(CONTENT_TYPE, APPLICATION_JSON);
			httpPost.setHeader(AUTHORIZATION, AUTHORIZATION_VALUE);
			httpPost.setEntity(new StringEntity(json, UTF_8));
			HttpResponse response = httpclient.execute(httpPost);
			HttpEntity entity = response.getEntity();
			if (entity != null) {
				String result = EntityUtils.toString(entity, UTF_8);
				jsonObj = JSON.parseObject(result);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return jsonObj;
	}

	public static String doPost2(String url, String json) {
		HttpPost httpPost = new HttpPost();
		httpPost.setURI(URI.create(url));
		try {
			httpPost.setHeader(CONTENT_TYPE, APPLICATION_JSON);
			httpPost.setEntity(new StringEntity(json, UTF_8));
			HttpResponse response = httpclient.execute(httpPost);
			HttpEntity entity = response.getEntity();
			if (entity != null) {
				return EntityUtils.toString(entity, UTF_8);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return "";
	}

}