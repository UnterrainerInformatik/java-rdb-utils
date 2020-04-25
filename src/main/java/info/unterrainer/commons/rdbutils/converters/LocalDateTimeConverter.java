package info.unterrainer.commons.rdbutils.converters;

import java.time.temporal.ChronoUnit;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

@Converter(autoApply = true)
public class LocalDateTimeConverter implements AttributeConverter<java.time.LocalDateTime, java.sql.Timestamp> {

	@Override
	public java.sql.Timestamp convertToDatabaseColumn(final java.time.LocalDateTime entityValue) {
		return entityValue == null ? null : java.sql.Timestamp.valueOf(entityValue.truncatedTo(ChronoUnit.MICROS));
	}

	@Override
	public java.time.LocalDateTime convertToEntityAttribute(final java.sql.Timestamp dbValue) {
		return dbValue == null ? null : dbValue.toLocalDateTime();
	}
}