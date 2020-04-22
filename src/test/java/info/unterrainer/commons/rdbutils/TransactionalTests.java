package info.unterrainer.commons.rdbutils;

import static org.assertj.core.api.Assertions.assertThat;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Query;

import org.junit.Test;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TransactionalTests {

	@Test
	public void liquibaseWorks() {
		try {
			EntityManager entityManager;
			EntityManagerFactory factory = RdbUtils.createAutoclosingEntityManagerFactory("test");
			assertThat(true).isTrue();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Test
	public void queryWorks() {
		try {
			EntityManager entityManager;
			EntityManagerFactory factory = RdbUtils.createAutoclosingEntityManagerFactory("test");
			EntityManager em = factory.createEntityManager();
			// TypedQuery<Country> query = em.createQuery("SELECT * FROM Country c WHERE
			// c.name = :name", Country.class);
			// String result = query.setParameter("name", "Gerald").getSingleResult();
			Query q = em.createQuery("SELECT 1=1");
			log.info("result: [{}]", q.getSingleResult());
			assertThat(true).isTrue();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Test
	public void works() {
		EntityManager entityManager;
		// Transactions.using(entityManager, em -> {

		// });
		assertThat(true).isTrue();
	}
}
