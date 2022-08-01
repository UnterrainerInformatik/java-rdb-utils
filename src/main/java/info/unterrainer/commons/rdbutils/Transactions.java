package info.unterrainer.commons.rdbutils;

import java.util.function.Consumer;
import java.util.function.Function;

import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityManager;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class Transactions {

	public static void withNewTransaction(final EntityManagerFactory emf, final Consumer<EntityManager> consumer) {
		Function<EntityManager, Void> function = em -> {
			consumer.accept(em);
			return null;
		};
		withNewTransactionReturning(emf, function);
	}

	public static <T> T withNewTransactionReturning(final EntityManagerFactory emf,
			final Function<EntityManager, T> function) {
		EntityManager em = null;
		try {
			em = emf.createEntityManager();
			T result = withNewTransactionReturning(em, function);
			return result;
		} finally {
			if (em != null && em.isOpen())
				em.close();
		}
	}

	public static void withNewTransaction(final EntityManager em, final Consumer<EntityManager> consumer) {
		Function<EntityManager, Void> function = entityManager -> {
			consumer.accept(em);
			return null;
		};
		withNewTransactionReturning(em, function);
	}

	public static <T> T withNewTransactionReturning(final EntityManager em, final Function<EntityManager, T> function) {
		try {
			if (!em.getTransaction().isActive())
				em.getTransaction().begin();

			T result = function.apply(em);

			em.flush();
			em.getTransaction().commit();
			return result;
		} catch (Exception e) {
			if (em.getTransaction() != null && em.getTransaction().isActive())
				em.getTransaction().rollback();
			throw e;
		}
	}
}
