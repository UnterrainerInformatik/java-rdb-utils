package info.unterrainer.commons.rdbutils.converters;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter()
public class LocalDateTimeConverter implements AttributeConverter<LocalDateTime, Timestamp> {

	@Override
	public Timestamp convertToDatabaseColumn(final LocalDateTime entityValue) {
		if (entityValue == null)
			return null;
		return Timestamp.from(entityValue.truncatedTo(ChronoUnit.MICROS).toInstant(ZoneOffset.UTC));
	}

	@Override
	public LocalDateTime convertToEntityAttribute(final Timestamp dbValue) {
		if (dbValue == null)
			return null;
		return LocalDateTime.ofInstant(dbValue.toInstant(), ZoneOffset.UTC);
	}
}