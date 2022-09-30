package info.unterrainer.commons.rdbutils;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import org.junit.jupiter.api.Test;

import info.unterrainer.commons.jreutils.DateUtils;
import info.unterrainer.commons.rdbutils.converters.LocalDateTimeConverter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class LocalDateTimeConverterTests {

	LocalDateTimeConverter converter = new LocalDateTimeConverter();

	@Test
	public void conversionFromLocalDateTimeToTimestamp() {
		LocalDateTime d = DateUtils.nowUtc();
		Timestamp ts = converter.convertToDatabaseColumn(d);
		assertThat(ts.toLocalDateTime()).isEqualTo(d.truncatedTo(ChronoUnit.MICROS));
	}

	@Test
	public void conversionFromTimestampToLocalDateTime() {
		LocalDateTime now = DateUtils.nowUtc();
		Timestamp ts = new Timestamp(DateUtils.utcLocalDateTimeToEpoch(now));
		ts.setNanos(now.truncatedTo(ChronoUnit.MICROS).getNano());
		LocalDateTime d = converter.convertToEntityAttribute(ts);

		Timestamp timestamp = Timestamp.valueOf(d);
		timestamp.setNanos(d.truncatedTo(ChronoUnit.MICROS).getNano());
		LocalDateTime other = DateUtils.nowUtc();

		assertThat(d).isEqualTo(other);
	}
}
