package com.stable.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import com.stable.job.RealtimeJob;
import com.stable.utils.WxPushUtil;

@Component
//@Log4j2
public class MyApplicationRunner implements ApplicationRunner {
	@Autowired
	private RealtimeJob realtimeJob;

	@Override
	public void run(ApplicationArguments args) throws Exception {
//        System.out.println("通过实现ApplicationRunner接口，在spring boot项目启动后打印参数");
//        String[] sourceArgs = args.getSourceArgs();
//        for (String arg : sourceArgs) {
//            System.out.print(arg + " ");
//        }
		WxPushUtil.pushSystem1("系统正常启动");

		new Thread(new Runnable() {
			@Override
			public void run() {
				realtimeJob.execute(null);
			}
		}).start();
	}
}