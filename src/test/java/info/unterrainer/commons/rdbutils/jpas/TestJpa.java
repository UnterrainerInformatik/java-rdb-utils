package info.unterrainer.commons.rdbutils.jpas;

import javax.persistence.Entity;
import javax.persistence.Table;

import info.unterrainer.commons.rdbutils.entities.BasicJpa;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@SuperBuilder(toBuilder = true)
@Entity
@Table(name = "test")
public class TestJpa extends BasicJpa {

	private String message;
}
