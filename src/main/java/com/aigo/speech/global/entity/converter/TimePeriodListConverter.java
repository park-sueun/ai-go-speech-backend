package com.aigo.speech.global.entity.converter;

import java.util.List;

import com.aigo.speech.global.dto.TimePeriod;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class TimePeriodListConverter implements AttributeConverter<List<TimePeriod>, String> {

	private static final ObjectMapper MAPPER = new ObjectMapper();

	@Override
	public String convertToDatabaseColumn(List<TimePeriod> attribute) {
		if (attribute == null || attribute.isEmpty()) return null;
		try {
			return MAPPER.writeValueAsString(attribute);
		} catch (JsonProcessingException e) {
			throw new IllegalStateException("List<TimePeriod> 직렬화 실패", e);
		}
	}

	@Override
	public List<TimePeriod> convertToEntityAttribute(String dbData) {
		if (dbData == null || dbData.isBlank()) return List.of();
		try {
			return MAPPER.readValue(dbData, new TypeReference<>() {});
		} catch (JsonProcessingException e) {
			throw new IllegalStateException("List<TimePeriod> 역직렬화 실패", e);
		}
	}
}
