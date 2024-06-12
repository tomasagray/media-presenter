package net.tomasbot.mp.db.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.net.URI;

@Converter
public class UriConverter implements AttributeConverter<URI, String> {

	@Override
	public String convertToDatabaseColumn(URI attribute) {
		return attribute != null ? attribute.toString() : null;
	}

	@Override
	public URI convertToEntityAttribute(String dbData) {
		return dbData != null ? URI.create(dbData) : null;
	}
}
