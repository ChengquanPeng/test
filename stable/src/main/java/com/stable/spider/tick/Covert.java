package com.stable.spider.tick;

import java.io.File;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.stable.service.DaliyBasicHistroyService;
import com.stable.service.model.prd.TickService;
import com.stable.utils.FileReaderLineWorker;
import com.stable.utils.FileReaderUitl;
import com.stable.utils.FileWriteUitl;
import com.stable.utils.ThreadsUtil;

@Service
public class Covert {

	@Value("${tick.folder}")
	private String tickFolder;
	@Autowired
	private DaliyBasicHistroyService daliyBasicHistroyService;

	public void startCovert() {
		File tf = new File(tickFolder);
		for (String code : tf.list()) {
			System.err.println(code);
			if (isCodeOrDate(code)) {//
				File cf = new File(tickFolder + code);
				for (String date : cf.list()) {
					System.err.println(code + ":" + date);
					if (isCodeOrDate(date)) {
						List<TickFb> fbs = readFromFile(cf.getAbsolutePath() + File.separator + date);
						if (fbs.size() > 0) {
							FileWriteUitl f1 = new FileWriteUitl(cf.getAbsolutePath() + File.separator + date, true);
							double yp = daliyBasicHistroyService.queryByCodeAndDate(code, Integer.valueOf(date))
									.getYesterdayPrice();
							Map<Integer, TickFz> map = TencentTick.getTickFzMap(fbs, yp);
							List<TickFz> fzs = TencentTick.getTickFz(map);
							for (TickFz t : fzs) {
								f1.writeLine(TencentTick.tickfzToStr(t));
							}
							f1.close();

							FileWriteUitl f2 = new FileWriteUitl(
									cf.getAbsolutePath() + File.separator + date + TickService.tickDaliy, true);
							f2.writeLine(TencentTick.tickDayToStr(TencentTick.getTickTickDay(map)));
							f2.close();
						}
						System.err.println(code + ":" + date + ":done");
					}
				}
				ThreadsUtil.sleepRandomSecBetween1And2();
			}
		}
		System.err.println("all:done");
	}

	private boolean isCodeOrDate(String str) {
		try {
			Integer.valueOf(str);
			return true;
		} catch (Exception e) {
			return false;
		}
	}

//	@PostConstruct
	private void a() {
		new Thread(new Runnable() {
			@Override
			public void run() {
				ThreadsUtil.sleepRandomSecBetween5And15();
//				tickFolder = "E:/ticks/";
				startCovert();
			}
		}).start();
	}

	public static void main(String[] args) {
		Covert cv = new Covert();
		cv.tickFolder = "E:/ticks/";
		cv.startCovert();
	}

	// 读文件
	public List<TickFb> readFromFile(String filepath) {
		FileReaderUitl reader = new FileReaderUitl(filepath);
		List<TickFb> list = new LinkedList<TickFb>();
		reader.readLineAndClosed(new FileReaderLineWorker() {
			@Override
			public void doworker(String line) {
				TickFb t = getFbVo(line);
				list.add(t);
			}
		});
		return list;
	}

	public TickFb getFbVo(String line) {
		TickFb t = new TickFb();
		// 4445,15:00:01,5.47,0.0,21953,1.2008291E7,1,
		String[] fs = line.split(",");
		t.setId(fs[0]);
		t.setTime(fs[1]);
		t.setPrice(Double.valueOf(fs[2]));
		t.setChange(Double.valueOf(fs[3]));
		t.setVol(Long.valueOf(fs[4]));
		t.setAmt(Double.valueOf(fs[5]));
		if ("S".equals(fs[6])) {
			t.setBs(1);
		}
		t.setFen(Integer.valueOf(fs[7]));
		return t;
	}

}
