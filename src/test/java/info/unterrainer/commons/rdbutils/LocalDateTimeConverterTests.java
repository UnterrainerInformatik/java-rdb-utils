package info.unterrainer.commons.rdbutils;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.TimeZone;

import org.junit.jupiter.api.Test;

import info.unterrainer.commons.jreutils.DateUtils;
import info.unterrainer.commons.rdbutils.converters.LocalDateTimeConverter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class LocalDateTimeConverterTests {

	private static final List<String> ZONES = List.of("UTC", "Europe/Vienna", "America/Los_Angeles", "Asia/Tokyo");

	LocalDateTimeConverter converter = new LocalDateTimeConverter();

	@Test
	public void conversionFromLocalDateTimeToTimestamp() {
		LocalDateTime d = DateUtils.nowUtc();
		Timestamp ts = converter.convertToDatabaseColumn(d);
		assertThat(ts.getTime()).isEqualTo(d.truncatedTo(ChronoUnit.MICROS).toInstant(ZoneOffset.UTC).toEpochMilli());
	}

	@Test
	public void conversionFromTimestampToLocalDateTime() {
		LocalDateTime now = DateUtils.nowUtc();
		Timestamp ts = Timestamp.from(now.truncatedTo(ChronoUnit.MICROS).toInstant(ZoneOffset.UTC));

		LocalDateTime d = converter.convertToEntityAttribute(ts);

		assertThat(d).isEqualTo(now.truncatedTo(ChronoUnit.MICROS));
	}

	@Test
	public void convertToDatabaseColumn_isJvmZoneIndependent() {
		LocalDateTime utcValue = LocalDateTime.of(2026, 4, 29, 12, 0, 0);
		long expectedEpochMillis = utcValue.toInstant(ZoneOffset.UTC).toEpochMilli();

		TimeZone original = TimeZone.getDefault();
		try {
			for (String zoneId : ZONES) {
				TimeZone.setDefault(TimeZone.getTimeZone(zoneId));
				Timestamp ts = converter.convertToDatabaseColumn(utcValue);
				assertThat(ts.getTime())
						.as("Persisted instant must equal the UTC interpretation regardless of JVM zone (was %s)",
								zoneId)
						.isEqualTo(expectedEpochMillis);
			}
		} finally {
			TimeZone.setDefault(original);
		}
	}

	@Test
	public void convertToEntityAttribute_isJvmZoneIndependent() {
		LocalDateTime utcValue = LocalDateTime.of(2026, 4, 29, 12, 0, 0);
		Timestamp ts = Timestamp.from(utcValue.toInstant(ZoneOffset.UTC));

		TimeZone original = TimeZone.getDefault();
		try {
			for (String zoneId : ZONES) {
				TimeZone.setDefault(TimeZone.getTimeZone(zoneId));
				LocalDateTime read = converter.convertToEntityAttribute(ts);
				assertThat(read)
						.as("Read value must equal the original UTC value regardless of JVM zone (was %s)", zoneId)
						.isEqualTo(utcValue);
			}
		} finally {
			TimeZone.setDefault(original);
		}
	}

	@Test
	public void roundTrip_truncatesToMicroseconds() {
		LocalDateTime subMicro = LocalDateTime.of(2026, 4, 29, 12, 0, 0, 123_456_789);
		LocalDateTime expected = subMicro.truncatedTo(ChronoUnit.MICROS);

		Timestamp ts = converter.convertToDatabaseColumn(subMicro);
		LocalDateTime roundTripped = converter.convertToEntityAttribute(ts);

		assertThat(roundTripped).isEqualTo(expected);
	}

	@Test
	public void nullInputs_passThrough() {
		assertThat(converter.convertToDatabaseColumn(null)).isNull();
		assertThat(converter.convertToEntityAttribute(null)).isNull();
	}
}
