package com.stable.utils;

import java.io.File;

import lombok.extern.log4j.Log4j2;

@Log4j2
public class FileDeleteUitl {
	public static void deletePastDateFile(String folder) {
		try {
			File file = new File(folder);
			log.info("文件夹：{}", folder);
			File[] filelist = file.listFiles();
			if (filelist == null || filelist.length == 0) {
				return;
			}
			long newtime = System.currentTimeMillis();
			for (int i = 0; i < filelist.length; i++) {
				long txttime = filelist[i].lastModified();// 遍历的度文件时间知
				long time = newtime - txttime;
				if ((time / (1000 * 60 * 60 * 24)) > 30) {
					boolean b = filelist[i].delete();
					if (b) {
						log.info(filelist[i].getName() + "删除成功!");
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
