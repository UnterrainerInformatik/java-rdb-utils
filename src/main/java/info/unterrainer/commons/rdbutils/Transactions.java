package info.unterrainer.commons.rdbutils;

import java.util.function.Function;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class Transactions {

	public static <T> T withNewTransaction(final EntityManagerFactory emf, final Function<EntityManager, T> function) {
		EntityManager em = null;
		try {
			em = emf.createEntityManager();
			T result = withNewTransaction(em, function);
			return result;
		} finally {
			if (em != null && em.isOpen())
				em.close();
		}
	}

	public static <T> T withNewTransaction(final EntityManager em, final Function<EntityManager, T> function) {
		try {
			if (!em.getTransaction().isActive())
				em.getTransaction().begin();

			T result = function.apply(em);

			em.getTransaction().commit();
			return result;
		} catch (Exception e) {
			if (em.getTransaction() != null && em.getTransaction().isActive())
				em.getTransaction().rollback();
			throw e;
		}
	}
}
