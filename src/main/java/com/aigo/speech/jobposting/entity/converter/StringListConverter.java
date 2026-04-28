package com.aigo.speech.jobposting.entity.converter;

import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class StringListConverter implements AttributeConverter<List<String>, String> {

	private static final ObjectMapper MAPPER = new ObjectMapper();

	@Override
	public String convertToDatabaseColumn(List<String> attribute) {
		if (attribute == null || attribute.isEmpty()) return null;
		try {
			return MAPPER.writeValueAsString(attribute);
		} catch (JsonProcessingException e) {
			throw new IllegalStateException("List<String> 직렬화 실패", e);
		}
	}

	@Override
	public List<String> convertToEntityAttribute(String dbData) {
		if (dbData == null || dbData.isBlank()) return List.of();
		try {
			return MAPPER.readValue(dbData, new TypeReference<>() {});
		} catch (JsonProcessingException e) {
			throw new IllegalStateException("List<String> 역직렬화 실패", e);
		}
	}
}
