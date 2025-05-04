package net.tomasbot.mp.db.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import net.tomasbot.ffmpeg_wrapper.metadata.FFmpegMetadata;
import net.tomasbot.mp.util.JsonParser;

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
