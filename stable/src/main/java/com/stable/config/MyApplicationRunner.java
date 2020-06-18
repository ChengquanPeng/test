package com.stable.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import com.stable.job.RealtimeJob;
import com.stable.utils.WxPushUtil;

@Component
public class MyApplicationRunner implements ApplicationRunner {
	private RealtimeJob realtimeJob;

	@Override
	public void run(ApplicationArguments args) throws Exception {
//        System.out.println("通过实现ApplicationRunner接口，在spring boot项目启动后打印参数");
//        String[] sourceArgs = args.getSourceArgs();
//        for (String arg : sourceArgs) {
//            System.out.print(arg + " ");
//        }
//        System.out.println();
		WxPushUtil.pushSystem1("系统正常启动");
		new Thread(new Runnable() {

			@Override
			public void run() {
				realtimeJob.execute(null);
			}
		}).start();

		// new RuntimeException().printStackTrace();

//		try {
//			TimeUnit.MINUTES.sleep(1);
//		} catch (InterruptedException e) {
//			e.printStackTrace();
//		}
//		new Thread(new Runnable() {
//			@Override
//			public void run() {
//				Object job = SpringUtil.getBean("tickDataJob");
//				if (job != null && job instanceof SimpleJob) {
//					WxPushUtil.pushSystem1("tickDataJob 运行中..,");
//					((SimpleJob) job).execute(null);
//
//				}
//			}
//		}).start();
	}
}