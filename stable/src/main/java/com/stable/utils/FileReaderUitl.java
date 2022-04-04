package com.stable.utils;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class FileReaderUitl {

	private FileReader reader;
	private BufferedReader br;

	public FileReaderUitl(String filepath) {
		try {
			reader = new FileReader(filepath);
			br = new BufferedReader(reader);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void readLineAndClosed(FileReaderLineWorker worker) {
		try {
			String line = null;
			while ((line = br.readLine()) != null) {
				worker.doworker(line);
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		} finally {
			close();
		}
	}

	private void close() {
		try {
			br.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		try {
			reader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
