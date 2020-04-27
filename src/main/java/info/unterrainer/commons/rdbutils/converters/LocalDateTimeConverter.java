package info.unterrainer.commons.rdbutils.converters;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

@Converter()
public class LocalDateTimeConverter implements AttributeConverter<LocalDateTime, Timestamp> {

	@Override
	public Timestamp convertToDatabaseColumn(final LocalDateTime attribute) {
		return attribute == null ? null : Timestamp.valueOf(attribute.truncatedTo(ChronoUnit.MICROS));
	}

	@Override
	public LocalDateTime convertToEntityAttribute(final Timestamp dbData) {
		return dbData == null ? null : dbData.toLocalDateTime();
	}
}