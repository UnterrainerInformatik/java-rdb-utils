package info.unterrainer.commons.rdbutils;

import javax.persistence.EntityManagerFactory;
import javax.persistence.TypedQuery;

import org.junit.BeforeClass;
import org.junit.Test;

import info.unterrainer.commons.rdbutils.exceptions.RdbUtilException;
import info.unterrainer.commons.rdbutils.jpas.TestJpa;

public class ManualTests {

	public static EntityManagerFactory emf;

	@BeforeClass
	public static void setupClass() throws RdbUtilException {
		emf = RdbUtils.createAutoclosingEntityManagerFactory("test");
	}

	@Test
	public void writingAndReadingOfSimpleEntityWorks() {
		Transactions.withNewTransaction(emf, em -> {
			TestJpa jpa = TestJpa.builder().log("test").build();
			em.persist(jpa);
		});

		Transactions.withNewTransaction(emf, em -> {
			TypedQuery<TestJpa> q = em.createQuery(String.format("SELECT * FROM %s c", TestJpa.class.getSimpleName()),
					TestJpa.class);
			TestJpa jpa = q.getSingleResult();
			System.out.println(jpa.log());
		});
	}
}
