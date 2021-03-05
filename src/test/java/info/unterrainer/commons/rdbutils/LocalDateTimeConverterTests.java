package info.unterrainer.commons.rdbutils;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Timestamp;
import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

import info.unterrainer.commons.jreutils.DateUtils;
import info.unterrainer.commons.rdbutils.converters.LocalDateTimeConverter;

public class LocalDateTimeConverterTests {

	LocalDateTimeConverter converter = new LocalDateTimeConverter();

	@Test
	public void conversionFromLocalDateTimeToTimestamp() {
		LocalDateTime d = DateUtils.nowUtc();
		Timestamp ts = converter.convertToDatabaseColumn(d);

		assertThat(DateUtils.utcLocalDateTimeToEpoch(d)).isEqualTo(ts.getTime());
	}

	@Test
	public void conversionFromTimestampToLocalDateTime() {
		Timestamp ts = new Timestamp(DateUtils.utcLocalDateTimeToEpoch(DateUtils.nowUtc()));
		LocalDateTime d = converter.convertToEntityAttribute(ts);

		assertThat(ts.getTime()).isEqualTo(DateUtils.utcLocalDateTimeToEpoch(d));
	}
}
