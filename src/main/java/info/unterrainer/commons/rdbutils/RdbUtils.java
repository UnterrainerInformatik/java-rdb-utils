package info.unterrainer.commons.rdbutils;

import java.io.IOException;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;

import info.unterrainer.commons.jreutils.Resources;
import info.unterrainer.commons.jreutils.ShutdownHook;
import info.unterrainer.commons.rdbutils.exceptions.RdbUtilException;
import liquibase.Contexts;
import liquibase.LabelExpression;
import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseFactory;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.DatabaseException;
import liquibase.resource.ClassLoaderResourceAccessor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RdbUtils {

	public static final String PROPERTY_NAME_URL = "javax.persistence.jdbc.url";
	public static final String PROPERTY_NAME_USER = "javax.persistence.jdbc.user";
	public static final String PROPERTY_NAME_PASSWORD = "javax.persistence.jdbc.password";

	/**
	 * Creates a new {@link EntityManagerFactory} with default-parameters or
	 * parameters given via environment variables.
	 * <p>
	 * Runs liquibase-update to apply any changes.<br / > Installs a shutdown-hook
	 * that ensures that the connection to the database is properly closed.
	 *
	 * @param persistenceUnitName the name of the persistence-unit to use (from your
	 *                            persistence.xml)
	 * @return an {@link EntityManagerFactory}
	 * @throws RdbUtilException if the database could not have been opened by
	 *                          liquibase.
	 */
	public static EntityManagerFactory createAutoclosingEntityManagerFactory(final String persistenceUnitName)
			throws RdbUtilException {
		return createAutoclosingEntityManagerFactory(persistenceUnitName, null);
	}

	/**
	 * Creates a new {@link EntityManagerFactory} with default-parameters or
	 * parameters given via environment variables.
	 * <p>
	 * Runs liquibase-update to apply any changes.<br / > Installs a shutdown-hook
	 * that ensures that the connection to the database is properly closed.
	 *
	 * @param persistenceUnitName the name of the persistence-unit to use (from your
	 *                            persistence.xml)
	 * @return an {@link EntityManagerFactory}
	 * @throws RdbUtilException if the database could not have been opened by
	 *                          liquibase.
	 */
	public static EntityManagerFactory createAutoclosingEntityManagerFactory(final String persistenceUnitName,
			final String prefix) throws RdbUtilException {
		Map<String, String> properties = getProperties(prefix);
		liquibaseUpdate(properties);
		EntityManagerFactory factory = Persistence.createEntityManagerFactory(persistenceUnitName, properties);
		ShutdownHook.register(() -> factory.close());
		return factory;
	}

	private static Map<String, String> getProperties(final String prefix) {
		RdbConfiguration c = RdbConfiguration.read(prefix);

		Map<String, String> result = new HashMap<>();
		result.put(PROPERTY_NAME_URL, String.format("jdbc:%s://%s:%s/%s", c.driver(), c.server(), c.port(), c.name()));
		result.put(PROPERTY_NAME_USER, c.user());
		result.put(PROPERTY_NAME_PASSWORD, c.password());
		return result;
	}

	private static void liquibaseUpdate(final Map<String, String> properties) throws RdbUtilException {
		Connection connection;
		try {
			connection = DriverManager.getConnection(
					properties.get(PROPERTY_NAME_URL) + "?allowPublicKeyRetrieval=true&useSSL=false",
					properties.get(PROPERTY_NAME_USER), properties.get(PROPERTY_NAME_PASSWORD));
			Database database = DatabaseFactory.getInstance()
					.findCorrectDatabaseImplementation(new JdbcConnection(connection));
			List<Path> masterLogFiles = Resources.walk(path -> path.toString().endsWith("-master.xml"));
			for (Path changelog : masterLogFiles) {
				log.info("running Liquibase for master-file [{}]", changelog.toString());
				liquibaseUpdate(database, changelog);
			}
		} catch (DatabaseException e) {
			String msg = "Error accessing the database";
			log.error(msg);
			throw new RdbUtilException(msg, e);
		} catch (SQLException e) {
			String msg = "Error getting connection via DriverManager";
			log.error(msg);
			throw new RdbUtilException(msg, e);
		} catch (IOException e) {
			String msg = "Error scanning for database-changelog-master files";
			log.error(msg);
			throw new RdbUtilException(msg, e);
		}
	}

	private static void liquibaseUpdate(final Database database, final Path changelog) throws RdbUtilException {
		try (Liquibase liquibase = new liquibase.Liquibase(changelog.toString(), new ClassLoaderResourceAccessor(),
				database)) {
			liquibase.update(new Contexts(), new LabelExpression());
		} catch (Exception e) {
			String msg = "Error running the liquibase script or closing the liquibase-client";
			log.error(msg);
			throw new RdbUtilException(msg, e);
		}
	}
}
