package info.unterrainer.commons.rdbutils.entities;

import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.MappedSuperclass;

import info.unterrainer.commons.rdbutils.enums.AsyncState;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@SuperBuilder(toBuilder = true)
@MappedSuperclass
public class BasicAsyncJpa extends BasicJpa {

	@Enumerated(EnumType.STRING)
	private AsyncState state;
}
