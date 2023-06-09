package com.stable.config;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import com.stable.job.RealtimeJob;
import com.stable.service.model.prd.msg.MsgPushServer;
import com.stable.utils.OSystemUtil;
import com.stable.utils.SpringUtil;

@Component
//@Log4j2
public class MyApplicationRunner implements ApplicationRunner {
	@Autowired
	private RealtimeJob realtimeJob;
	// @Autowired
	// private FinanceService financeService;
	
	@Override
	public void run(ApplicationArguments args) throws Exception {
//        System.out.println("通过实现ApplicationRunner接口，在spring boot项目启动后打印参数");
//        String[] sourceArgs = args.getSourceArgs();
//        for (String arg : sourceArgs) {
//            System.out.print(arg + " ");
//        }

		new Thread(new Runnable() {
			@Override
			public void run() {
				// codeAttentionService.fetchAll();
				realtimeJob.execute(null);
			}
		}).start();
		if (OSystemUtil.isWindows()) {
			// printUrl();
			return;
		}
		MsgPushServer.pushToSystem("系统正常启动");

		new Thread(new Runnable() {
			@Override
			public void run() {
//				codeModelService.runModel(20220613, true);
			}
		}).start();
	}
//	@Autowired
//	private com.stable.service.model.CodeModelService codeModelService;


	public void printUrl() {
		RequestMappingHandlerMapping bean = SpringUtil.getBean(RequestMappingHandlerMapping.class);
		Map<RequestMappingInfo, HandlerMethod> handlerMethods = bean.getHandlerMethods();
		handlerMethods.forEach((k, v) -> {
			Set<RequestMethod> methods = k.getMethodsCondition().getMethods();
			if (CollectionUtils.isEmpty(methods)) {
				methods = new HashSet<>();
				methods.add(RequestMethod.GET);
				methods.add(RequestMethod.POST);
			}
			final Set<String> patterns = k.getPatternsCondition().getPatterns();
			for (RequestMethod requestMethod : methods) {
				for (String pattern : patterns) {
					System.err.println("method:" + requestMethod + ",pattern:" + pattern);
				}
			}
		});
	}
}