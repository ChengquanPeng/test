package com.stable.utils;

import java.io.IOException;
import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.BeanPropertyWriter;
import com.fasterxml.jackson.databind.ser.BeanSerializerModifier;
import com.stable.constant.Constant;

/**
 *json 自定义格式化
 */
public class MyBeanSerializerModifier extends BeanSerializerModifier {

	// 空数组格式json []
	private JsonSerializer<Object> _nullArrayJsonSerializer = new JsonSerializer<Object>() {
		@Override
		public void serialize(Object value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
			if (value == null) {
				gen.writeStartArray();
				gen.writeEndArray();
			} else {
				gen.writeObject(value);
			}
		}
	};

	// 空字段格式化空字符串""
	private JsonSerializer<Object> _nullStringJsonSerializer = new JsonSerializer<Object>() {
		@Override
		public void serialize(Object value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
			if (value == null) {
				gen.writeString(Constant.EMPTY_STRING);
			} else {
				gen.writeObject(value);
			}
		}
	};

	@Override
	public List<BeanPropertyWriter> changeProperties(SerializationConfig config, BeanDescription beanDesc,
			List<BeanPropertyWriter> beanProperties) {
		// 循环所有的beanPropertyWriter
		for (int i = 0; i < beanProperties.size(); i++) {
			BeanPropertyWriter writer = beanProperties.get(i);
			// 判断字段的类型，如果是array，list，set则注册nullSerializer
			if (isArrayType(writer)) {
				// 给writer注册一个自己的nullSerializer
				writer.assignNullSerializer(this._nullArrayJsonSerializer);
			} else {
				writer.assignNullSerializer(this._nullStringJsonSerializer);
			}
		}
		return beanProperties;
	}

	// 判断是Array类型
	@SuppressWarnings("deprecation")
	protected boolean isArrayType(BeanPropertyWriter writer) {
		Class<?> clazz = writer.getPropertyType();
		return clazz.isArray() || clazz.equals(List.class) || clazz.equals(Set.class);
	}
}
