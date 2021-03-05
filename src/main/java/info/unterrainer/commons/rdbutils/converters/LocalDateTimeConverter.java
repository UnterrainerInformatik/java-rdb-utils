package info.unterrainer.commons.rdbutils.converters;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;

import info.unterrainer.commons.jreutils.DateUtils;

@Converter()
public class LocalDateTimeConverter implements AttributeConverter<LocalDateTime, Timestamp> {

	DateUtils u;

	@Override
	public Timestamp convertToDatabaseColumn(final LocalDateTime attribute) {
		return attribute == null ? null
				: new Timestamp(DateUtils.utcLocalDateTimeToEpoch(attribute.truncatedTo(ChronoUnit.MICROS)));
	}

	@Override
	public LocalDateTime convertToEntityAttribute(final Timestamp dbData) {
		return dbData == null ? null : DateUtils.epochToUtcLocalDateTime(dbData.getTime());
	}
}