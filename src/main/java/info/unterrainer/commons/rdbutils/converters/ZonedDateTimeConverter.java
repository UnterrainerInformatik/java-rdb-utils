package info.unterrainer.commons.rdbutils.converters;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import info.unterrainer.commons.jreutils.DateUtils;

@Converter(autoApply = true)
public class ZonedDateTimeConverter implements AttributeConverter<LocalDateTime, Timestamp> {

	@Override
	public Timestamp convertToDatabaseColumn(final LocalDateTime entityValue) {
		return entityValue == null ? null
				: new Timestamp(DateUtils.utcLocalDateTimeToEpoch(entityValue.truncatedTo(ChronoUnit.MICROS)));
	}

	@Override
	public LocalDateTime convertToEntityAttribute(final Timestamp dbValue) {
		return dbValue == null ? null : DateUtils.epochToUtcLocalDateTime(dbValue.getTime());
	}
}