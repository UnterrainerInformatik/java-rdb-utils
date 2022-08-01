package info.unterrainer.commons.rdbutils.entities;

import java.time.LocalDateTime;

import jakarta.persistence.Convert;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;

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

	@Convert(converter = LocalDateTimeConverter.class)
	private LocalDateTime createdOn;

	@Convert(converter = LocalDateTimeConverter.class)
	private LocalDateTime editedOn;
}
