package info.unterrainer.commons.rdbutils;

import java.time.LocalDateTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.persistence.EntityManagerFactory;
import javax.persistence.LockModeType;
import javax.persistence.Query;
import javax.persistence.TypedQuery;

import org.junit.BeforeClass;
import org.junit.Test;

import info.unterrainer.commons.rdbutils.exceptions.RdbUtilException;
import info.unterrainer.commons.rdbutils.jpas.TestJpa;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ManualLockingTests {

	public static EntityManagerFactory emf;

	@BeforeClass
	public static void setupClass() {
		try {
			emf = RdbUtils.createAutoclosingEntityManagerFactory(ManualLockingTests.class, "test");
		} catch (RdbUtilException e) {
			log.error("Error getting EntityManagerFactory", e);
		}
	}

	@Test
	public void persistingAndReadingEntityWorks() throws InterruptedException {
		deleteTestTable();
		for (int i = 0; i < 10; i++)
			persistTestEntity(TestJpa.builder()
					.message("test")
					.createdOn(LocalDateTime.now())
					.editedOn(LocalDateTime.now())
					.build());

		ExecutorService executorService = new ThreadPoolExecutor(1, 200, 0L, TimeUnit.MILLISECONDS,
				new LinkedBlockingQueue<Runnable>());
		executorService.execute(() -> selectFirstTestEntityPessimistically());
		executorService.execute(() -> selectFirstTestEntityPessimistically());
		executorService.shutdown();
		executorService.awaitTermination(5, TimeUnit.MINUTES);
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

	private TestJpa selectFirstTestEntityPessimistically() {
		return Transactions.withNewTransactionReturning(emf, em -> {
			TypedQuery<TestJpa> q = em
					.createQuery(String.format("SELECT o FROM %s AS o", TestJpa.class.getSimpleName()), TestJpa.class);
			q.setMaxResults(1).setLockMode(LockModeType.PESSIMISTIC_WRITE);
			TestJpa result = q.getSingleResult();
			try {
				Thread.sleep(400L);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			System.out.println("Done with SELECT.");
			return result;
		});
	}
}
