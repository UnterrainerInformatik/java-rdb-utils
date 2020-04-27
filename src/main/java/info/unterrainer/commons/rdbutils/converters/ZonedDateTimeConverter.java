package info.unterrainer.commons.rdbutils.converters;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

@Converter(autoApply = true)
public class ZonedDateTimeConverter implements AttributeConverter<LocalDateTime, Timestamp> {

	@Override
	public Timestamp convertToDatabaseColumn(final java.time.LocalDateTime entityValue) {
		return entityValue == null ? null : Timestamp.valueOf(entityValue.truncatedTo(ChronoUnit.MICROS));
	}

	@Override
	public LocalDateTime convertToEntityAttribute(final Timestamp dbValue) {
		return dbValue == null ? null : dbValue.toLocalDateTime();
	}
}