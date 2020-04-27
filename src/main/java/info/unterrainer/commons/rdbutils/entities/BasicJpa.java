package info.unterrainer.commons.rdbutils.entities;

import java.time.LocalDateTime;

import javax.persistence.Convert;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import info.unterrainer.commons.rdbutils.converters.LocalDateTimeConverter;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@NoArgsConstructor
@SuperBuilder(toBuilder = true)
@MappedSuperclass
public class BasicJpa {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Temporal(TemporalType.TIMESTAMP)
	@Convert(converter = LocalDateTimeConverter.class)
	private LocalDateTime createdOn;

	@Temporal(TemporalType.TIMESTAMP)
	@Convert(converter = LocalDateTimeConverter.class)
	private LocalDateTime editedOn;
}
