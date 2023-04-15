package self.me.mp.db.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import self.me.mp.plugin.ffmpeg.metadata.FFmpegMetadata;
import self.me.mp.util.JsonParser;

@Converter
public class FFmpegMetadataConverter implements AttributeConverter<FFmpegMetadata, String> {

	@Override
	public String convertToDatabaseColumn(FFmpegMetadata attribute) {
		return JsonParser.toJson(attribute);
	}

	@Override
	public FFmpegMetadata convertToEntityAttribute(String dbData) {
		return JsonParser.fromJson(dbData, FFmpegMetadata.class);
	}
}
