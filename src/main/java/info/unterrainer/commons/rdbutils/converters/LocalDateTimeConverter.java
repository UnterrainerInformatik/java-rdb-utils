package info.unterrainer.commons.rdbutils.converters;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

import info.unterrainer.commons.jreutils.DateUtils;

@Converter()
public class LocalDateTimeConverter implements AttributeConverter<LocalDateTime, Timestamp> {

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