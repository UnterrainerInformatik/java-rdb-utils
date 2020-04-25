package info.unterrainer.commons.rdbutils.entities;

import java.time.LocalDateTime;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;

import lombok.Data;
import lombok.experimental.SuperBuilder;

@Data
@Entity
@MappedSuperclass
@SuperBuilder
public class BasicEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private LocalDateTime createdOn;
	private LocalDateTime editedAt;
}
