package com.stable.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.alibaba.fastjson.JSONObject;
import com.stable.vo.MarketHistroyVo;

public class PythonCallUtil {

	public static final String EXCEPT = "except";
	private static final String CALL_FORMAT;
	static {
		// SpringConfig efc = SpringUtil.getBean(SpringConfig.class);
		if (OSystemUtil.isWindows()) {
			CALL_FORMAT = "python %s %s";
		} else {
			CALL_FORMAT = "/usr/local/bin/python3.8 %s %s";
		}
	}
	// TODO 调用python脚本会CPU会瞬时100%，从而导致python吃CPU导致ES异常退出， 控制python的调用数量：

	public synchronized static List<String> callPythonScript(String pythonScriptPathAndFileName, String params) {
		InputStreamReader ir = null;
		BufferedReader input = null;
		try {
			List<String> sb = new LinkedList<String>();
			String cmd = String.format(CALL_FORMAT, pythonScriptPathAndFileName, params);
			// log.info("call Python Script Cmd:{}", cmd);
			Process proc = Runtime.getRuntime().exec(cmd);
			ir = new InputStreamReader(proc.getInputStream());
			input = new BufferedReader(ir);

			String line;
			while ((line = input.readLine()) != null) {
				// System.out.println(line);
				sb.add(line);
			}
			proc.waitFor();
			// int r = proc.waitFor();
			// log.info("call Python Script Cmd:{}，proc.waitFor：{}", cmd, r);
			return sb;
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		} finally {
			if (input != null) {
				try {
					input.close();
				} catch (IOException e) {
				}
			}
			if (ir != null) {
				try {
					ir.close();
				} catch (IOException e) {
				}
			}

		}
	}

	public synchronized static void callPythonScriptNoReturn(String pythonScriptPathAndFileName, String params) {
		try {
			String cmd = String.format(CALL_FORMAT, pythonScriptPathAndFileName, params);
			//System.err.println("call Python Script Cmd:" + cmd);
			Process proc = Runtime.getRuntime().exec(cmd);
			proc.waitFor();
			// log.info("call Python Script Cmd:{}，proc.waitFor：{}", cmd, r);
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		} finally {

		}
	}

	private static final String URL_TEMPLATE1 = "http://localhost:9090/tickdata?%s,%s";

	public synchronized static List<String> callPythonScriptByServerTickData(String code, String date) {
		return callPythonScriptByServer(String.format(URL_TEMPLATE1, code, date));
	}

	public synchronized static List<String> callPythonScriptByServer(String url) {
		List<String> sb = new LinkedList<String>();
		try {
			// System.err.println(url);
			String line = HttpUtil.doGet2(url);
			// System.err.println(line);
			String[] strs = line.split("A");
			for (int i = 0; i < strs.length; i++) {
				String l = strs[i];
				// System.err.println(l);
				if (StringUtils.isNotBlank(l)) {
					sb.add(l);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			sb.add(EXCEPT);
		}
		return sb;
	}

	public static void main(String[] args) {
		test2();
	}

	public static void test2() {
		for (int i = 0; i < 100; i++) {
			callPythonScriptByServerTickData("600000", "2020-01-09");
		}

	}

	public static void test1() {
		String pythonScriptPathAndFileName = "E:\\pythonworkspace\\tushareTickData.py";
		MarketHistroyVo mh = new MarketHistroyVo();
		mh.setTs_code("000029.SZ");
		mh.setAdj("qfq");
		mh.setStart_date("20191220");
		mh.setEnd_date("20191220");
		mh.setFreq("D");

		String params = JSONObject.toJSONString(mh);
		params = params.replaceAll("\"", "\'");
		PythonCallUtil.callPythonScript(pythonScriptPathAndFileName, params).forEach(str -> {
			System.out.println(str);
		});

	}
}
