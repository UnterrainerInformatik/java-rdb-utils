package info.unterrainer.commons.rdbutils;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.List;

import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Query;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import info.unterrainer.commons.rdbutils.exceptions.RdbUtilException;
import info.unterrainer.commons.rdbutils.jpas.TestJpa;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class HibernateDialectTests {

	public static EntityManagerFactory emf;

	@BeforeAll
	public static void setupClass() {
		try {
			emf = RdbUtils.createAutoclosingEntityManagerFactory(HibernateDialectTests.class, "test");
			deleteTestTable();
			for (int i = 1; i <= 20; i++)
				persistTestEntity(TestJpa.builder()
						.message(i + "")
						.createdOn(LocalDateTime.now())
						.editedOn(LocalDateTime.now())
						.build());
		} catch (RdbUtilException e) {
			log.error("Error getting EntityManagerFactory", e);
		}
	}

	@Test
	public void gettingFiveEntitiesWithOffsetZeroReturnsFive() {
		List<TestJpa> results = Transactions.withNewTransactionReturning(emf, em -> em
				.createQuery(String.format("SELECT o FROM %s AS o", TestJpa.class.getSimpleName()), TestJpa.class)
				.setMaxResults(5)
				.setFirstResult(0)
				.getResultList());
		assertThat(results.size()).isEqualTo(5);
		assertThat(results.get(0).getMessage()).isEqualTo("1");
		assertThat(results.get(4).getMessage()).isEqualTo("5");
	}

	@Test
	public void gettingLastTwoEntitiesWithSize5ReturnsTwo() {
		List<TestJpa> results = Transactions.withNewTransactionReturning(emf, em -> em
				.createQuery(String.format("SELECT o FROM %s AS o", TestJpa.class.getSimpleName()), TestJpa.class)
				.setMaxResults(5)
				.setFirstResult(18)
				.getResultList());
		assertThat(results.size()).isEqualTo(2);
		assertThat(results.get(0).getMessage()).isEqualTo("19");
		assertThat(results.get(1).getMessage()).isEqualTo("20");
	}

	@Test
	public void gettingThreeEntitiesWithOffset4ReturnsThree() {
		List<TestJpa> results = Transactions.withNewTransactionReturning(emf, em -> em
				.createQuery(String.format("SELECT o FROM %s AS o", TestJpa.class.getSimpleName()), TestJpa.class)
				.setMaxResults(3)
				.setFirstResult(4)
				.getResultList());
		assertThat(results.size()).isEqualTo(3);
		assertThat(results.get(0).getMessage()).isEqualTo("5");
		assertThat(results.get(1).getMessage()).isEqualTo("6");
		assertThat(results.get(2).getMessage()).isEqualTo("7");
	}

	private static int deleteTestTable() {
		return Transactions.withNewTransactionReturning(emf, em -> {
			Query q = em.createQuery(String.format("DELETE FROM %s AS o", TestJpa.class.getSimpleName()));
			int entitiesDeleted = q.executeUpdate();
			log.info("[{}] entities deleted", entitiesDeleted);
			return entitiesDeleted;
		});
	}

	private static void persistTestEntity(final TestJpa jpa) {
		Transactions.withNewTransaction(emf, em -> {
			em.persist(jpa);
		});
	}
}
