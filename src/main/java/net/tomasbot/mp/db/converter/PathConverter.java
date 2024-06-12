package net.tomasbot.mp.db.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.nio.file.Path;

@Converter
public class PathConverter implements AttributeConverter<Path, String> {

	@Override
	public String convertToDatabaseColumn(Path attribute) {
		return attribute != null ? attribute.toString() : null;
	}

	@Override
	public Path convertToEntityAttribute(String dbData) {
		return dbData != null ? Path.of(dbData) : null;
	}
}
