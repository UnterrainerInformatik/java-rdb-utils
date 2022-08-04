package info.unterrainer.commons.rdbutils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import info.unterrainer.commons.rdbutils.exceptions.RdbUtilException;
import info.unterrainer.commons.rdbutils.jpas.TestJpa;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Query;
import jakarta.persistence.TypedQuery;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ManualTests {

	public static EntityManagerFactory emf;
	public static TestJpa testJpa = TestJpa.builder()
			.message("test")
			.createdOn(LocalDateTime.now())
			.editedOn(LocalDateTime.now())
			.build();

	@BeforeAll
	public static void setupClass() {
		try {
			emf = RdbUtils.createAutoclosingEntityManagerFactory(ManualTests.class, "test");
		} catch (RdbUtilException e) {
			log.error("Error getting EntityManagerFactory", e);
		}
	}

	@BeforeEach
	public void beforeMethod() {
		deleteTestTable();
	}

	@Test
	public void persistingAndReadingEntityWorks() {
		persistTestEntity(testJpa);
		TestJpa jpa = selectFirstTestEntity();
		assertThat(jpa.getMessage()).isEqualTo(testJpa.getMessage());
		assertEquals(jpa.getCreatedOn(), testJpa.getCreatedOn().truncatedTo(ChronoUnit.MICROS));
		assertThat(jpa.getEditedOn()).isEqualTo(testJpa.getEditedOn().truncatedTo(ChronoUnit.MICROS));
	}

	@Test
	public void persistingAndUpdatingAndReadingEntityWorks() {
		persistTestEntity(testJpa);

		testJpa.setMessage("new message");
		testJpa.setCreatedOn(LocalDateTime.now().plus(1, ChronoUnit.SECONDS));
		testJpa.setEditedOn(LocalDateTime.now().plus(14, ChronoUnit.SECONDS));
		updateTestEntity(testJpa);

		TestJpa jpa = selectFirstTestEntity();
		assertThat(jpa.getMessage()).isEqualTo(testJpa.getMessage());
		assertEquals(jpa.getCreatedOn(), testJpa.getCreatedOn().truncatedTo(ChronoUnit.MICROS));
		assertThat(jpa.getEditedOn()).isEqualTo(testJpa.getEditedOn().truncatedTo(ChronoUnit.MICROS));
	}

	private int deleteTestTable() {
		return Transactions.withNewTransactionReturning(emf, em -> {
			Query q = em.createQuery(String.format("DELETE FROM %s AS o", TestJpa.class.getSimpleName()));
			int entitiesDeleted = q.executeUpdate();
			log.info("[{}] entities deleted", entitiesDeleted);
			return entitiesDeleted;
		});
	}

	private void persistTestEntity(final TestJpa jpa) {
		Transactions.withNewTransaction(emf, em -> {
			em.persist(jpa);
		});
	}

	private void updateTestEntity(final TestJpa jpa) {
		Transactions.withNewTransaction(emf, em -> {
			em.merge(jpa);
		});
	}

	private TestJpa selectFirstTestEntity() {
		return Transactions.withNewTransactionReturning(emf, em -> {
			TypedQuery<TestJpa> q = em
					.createQuery(String.format("SELECT o FROM %s AS o", TestJpa.class.getSimpleName()), TestJpa.class);
			q.setMaxResults(1);
			return q.getSingleResult();
		});
	}
}
