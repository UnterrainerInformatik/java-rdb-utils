package info.unterrainer.commons.rdbutils.converters;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter()
public class LocalDateTimeConverter implements AttributeConverter<LocalDateTime, Timestamp> {

	@Override
	public Timestamp convertToDatabaseColumn(final LocalDateTime entityValue) {
		if (entityValue == null)
			return null;
		Timestamp timestamp = Timestamp.valueOf(entityValue);
		timestamp.setNanos(entityValue.truncatedTo(ChronoUnit.MICROS).getNano());
		return timestamp;
	}

	@Override
	public LocalDateTime convertToEntityAttribute(final Timestamp dbValue) {
		if (dbValue == null)
			return null;
		return dbValue.toLocalDateTime();
	}
}