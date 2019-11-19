package com.stable.utils;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.util.LinkedList;
import java.util.List;

public class PythonCallUtil {

	private static final String CALL_FORMAT = "python %s %s";

	public static List<String> callPythonScript(String pythonScriptPathAndFileName, String params) {
		InputStreamReader ir = null;
		LineNumberReader input = null;
		List<String> sb = new LinkedList<String>();
		try {
			String cmd = String.format(CALL_FORMAT, pythonScriptPathAndFileName, params);
			System.out.println("call Python Script Cmd:" + cmd);
			Process process = Runtime.getRuntime().exec(cmd);
			ir = new InputStreamReader(process.getInputStream());
			input = new LineNumberReader(ir);
			String line;
			while ((line = input.readLine()) != null) {
				//System.out.println(line);
				sb.add(line);
			}

			return sb;
		} catch (IOException e) {
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

	public static void main(String[] args) {
		String pythonScriptPathAndFileName = "E:\\pythonworkspace\\tushareTickData.py";
		String params = "";
		PythonCallUtil.callPythonScript(pythonScriptPathAndFileName, params).forEach(str ->{
			System.out.println(str);
		});
		
	}
}
