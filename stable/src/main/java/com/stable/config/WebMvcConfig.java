package com.stable.config;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stable.interceptor.LoginInterceptor;
import com.stable.interceptor.MangerInterceptor;
import com.stable.utils.DateUtil;
import com.stable.utils.MyBeanSerializerModifier;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

	static List<String> excludelist = new LinkedList<String>();
	static List<String> mangerlist = new LinkedList<String>();
	static {
		excludelist.add("/login");
		excludelist.add("/logout");
		excludelist.add("/mylogin");
		excludelist.add("/sendkey");
		excludelist.add("/libs/**");
		excludelist.add("/login.html");

		mangerlist.add("/manager/**");
		mangerlist.add("/admin/**");
	}
	@Autowired
	private LoginInterceptor loginInterceptor;
	@Autowired
	private MangerInterceptor mangerInterceptor;

	@Override
	public void addResourceHandlers(ResourceHandlerRegistry registry) {
		registry.addResourceHandler("/static/**").addResourceLocations("classpath:/static/");
	}

	@Override
	public void addInterceptors(InterceptorRegistry registry) {
		registry.addInterceptor(loginInterceptor).addPathPatterns("/**").excludePathPatterns(excludelist);
		// 以下连接需要鉴权
		registry.addInterceptor(mangerInterceptor).addPathPatterns(mangerlist);
		WebMvcConfigurer.super.addInterceptors(registry);
	}

	@Bean
	@LoadBalanced
	public RestTemplate restTemplate() {
		return new RestTemplate();
	}

	@Bean
	public MappingJackson2HttpMessageConverter mappingJackson2HttpMessageConverter() {
		MappingJackson2HttpMessageConverter jsonConverter = new MappingJackson2HttpMessageConverter();
		// 设置解析JSON工具类
		ObjectMapper objectMapper = new ObjectMapper();
		// 设置解析日期的工具类
		objectMapper.setDateFormat(new SimpleDateFormat(DateUtil.YYYY_MM_DD_HH_MM_SS));
		// 忽略未知属性 防止解析报错
		objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		// 空字段自定义格式化
		objectMapper.setSerializerFactory(
				objectMapper.getSerializerFactory().withSerializerModifier(new MyBeanSerializerModifier()));
		jsonConverter.setObjectMapper(objectMapper);
		List<MediaType> list = new ArrayList<>();
		list.add(MediaType.APPLICATION_JSON_UTF8);
		jsonConverter.setSupportedMediaTypes(list);
		return jsonConverter;
	}
}
