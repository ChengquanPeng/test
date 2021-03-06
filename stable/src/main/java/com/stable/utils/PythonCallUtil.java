package com.stable.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Semaphore;

import org.apache.commons.lang3.StringUtils;

import com.stable.config.SpringConfig;

public class PythonCallUtil {

	public static final String EXCEPT = "except";
	private static final String CALL_FORMAT;
	private static final Semaphore semp;
	static {
		SpringConfig efc = SpringUtil.getBean(SpringConfig.class);
		semp = new Semaphore(efc.getPythonconcurrencynum());
		if (OSystemUtil.isWindows()) {
			CALL_FORMAT = "python %s %s";
		} else {
			CALL_FORMAT = "/usr/local/bin/python3.8 %s %s";
		}
	}
	// 调用python脚本会CPU会瞬时100%，从而导致python吃CPU导致ES异常退出， 控制python的调用数量：

	public static List<String> callPythonScript(String pythonScriptPathAndFileName, String params) {
		try {
			semp.acquire();
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		}
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
			semp.release();
		}
	}

	public synchronized static void callPythonScriptNoReturn(String pythonScriptPathAndFileName, String params) {
		try {
			String cmd = String.format(CALL_FORMAT, pythonScriptPathAndFileName, params);
			// System.err.println("call Python Script Cmd:" + cmd);
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
			String line = HttpUtil.doGet2(url, null);
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
}
