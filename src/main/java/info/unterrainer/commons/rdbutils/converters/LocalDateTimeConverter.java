package info.unterrainer.commons.rdbutils.converters;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import info.unterrainer.commons.jreutils.DateUtils;

@Converter()
public class LocalDateTimeConverter implements AttributeConverter<LocalDateTime, Timestamp> {

	@Override
	public Timestamp convertToDatabaseColumn(final LocalDateTime entityValue) {
		if (entityValue == null)
			return null;
		Timestamp timestamp = new Timestamp(ZonedDateTime.of(entityValue, ZoneId.of("UTC")).toInstant().toEpochMilli());
		timestamp.setNanos(entityValue.truncatedTo(ChronoUnit.MICROS).getNano());
		return timestamp;
	}

	// FIXXXING
	@Override
	public LocalDateTime convertToEntityAttribute(final Timestamp dbValue) {
		if (dbValue == null)
			return null;
		LocalDateTime ldt = DateUtils.epochToUtcLocalDateTime(dbValue.getTime());
		return ldt.withNano(dbValue.getNanos());
	}
}