package com.stable.utils;

import java.time.Duration;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.serializer.SerializerFeature;

@Component
public class RedisUtil {

	@Autowired
	private StringRedisTemplate redisTemplate;

	/**
	 * 设置key
	 */
	public void set(String key, Object value) {
		this.set(key, getjsonstirng(value), Duration.ofDays(365));// 默认365天
	}

	public void set(String key, Object value, Duration timeout) {
		this.set(key, getjsonstirng(value), timeout);
	}

	/**
	 * 删除Key
	 */
	public void del(String key) {
		redisTemplate.delete(key);
	}

	/**
	 * 获取Key
	 */
	public String get(String key) {
		return redisTemplate.opsForValue().get(key);
	}

	public String get(String key, String def) {
		String value = redisTemplate.opsForValue().get(key);
		if (StringUtils.isBlank(value)) {
			return def;
		}
		return value;
	}

	public int get(String key, int def) {
		String value = redisTemplate.opsForValue().get(key);
		if (StringUtils.isBlank(value)) {
			return def;
		}
		return Integer.valueOf(value);
	}

	public double get(String key, double def) {
		String value = redisTemplate.opsForValue().get(key);
		if (StringUtils.isBlank(value)) {
			return def;
		}
		return Double.valueOf(value);
	}

	/**
	 * 过期时间
	 */
	public Boolean expire(String key, long timeout, TimeUnit unit) {
		return redisTemplate.expire(key, timeout, unit);
	}

	public Long getExpire(String key) {
		return redisTemplate.getExpire(key);
	}

	public Set<String> keys(String pattern) {
		return redisTemplate.keys(pattern);
	}

	/**
	 * 增加(自增长), 负数则为自减
	 */
	public Long incrBy(String key, long increment) {
		return redisTemplate.opsForValue().increment(key, increment);
	}

	/**
	 * Json格式
	 */
	private String getjsonstirng(Object value) {
		return JSON.toJSONString(value, SerializerFeature.WriteDateUseDateFormat);
	}
}
