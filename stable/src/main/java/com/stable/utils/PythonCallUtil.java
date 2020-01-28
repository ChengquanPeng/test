package com.stable.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import com.alibaba.fastjson.JSONObject;
import com.stable.config.SpringConfig;
import com.stable.vo.MarketHistroyVo;

import lombok.extern.log4j.Log4j2;

@Log4j2
public class PythonCallUtil {

	public static final String EXCEPT = "except";
	private static final String CALL_FORMAT;
	private static final int pythonSeqCnt;
	static {
		SpringConfig efc = SpringUtil.getBean(SpringConfig.class);
		pythonSeqCnt = efc.getPythonconcurrencynum();
		log.info("python.script.concurrency.num={}", pythonSeqCnt);
		if (OSystemUtil.isWindows()) {
			CALL_FORMAT = "python %s %s";
		} else {
			CALL_FORMAT = "/usr/local/bin/python3.8 %s %s";
		}
	}
	// 控制python的调用数量：python吃CPU导致ES异常退出
	private static final Semaphore semp = new Semaphore(pythonSeqCnt);

	public static List<String> callPythonScript(String pythonScriptPathAndFileName, String params) {
		InputStreamReader ir = null;
		BufferedReader input = null;
		try {
//			semp.acquire();
			TimeUnit.SECONDS.sleep(1);
			List<String> sb = new LinkedList<String>();
			String cmd = String.format(CALL_FORMAT, pythonScriptPathAndFileName, params);
			log.info("call Python Script Cmd:{}", cmd);
			Process proc = Runtime.getRuntime().exec(cmd);
			ir = new InputStreamReader(proc.getInputStream());
			input = new BufferedReader(ir);

			String line;
			while ((line = input.readLine()) != null) {
				// System.out.println(line);
				sb.add(line);
			}
			int r = proc.waitFor();
			log.info("call Python Script Cmd:{}，proc.waitFor：{}", cmd, r);
			return sb;
		} catch (Exception e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		} finally {
//			semp.release();
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

	public static void main(String[] args) {
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
