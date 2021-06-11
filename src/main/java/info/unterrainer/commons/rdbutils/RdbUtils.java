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
	 * Runs Liquibase-update to apply any changes.<br>
	 * Installs a shutdown-hook that ensures that the connection to the database is
	 * properly closed.
	 *
	 * @param classLoaderSource   the source of the class-loader to use
	 * @param persistenceUnitName the name of the persistence-unit to use (from your
	 *                            persistence.xml)
	 * @return an {@link EntityManagerFactory}
	 * @throws RdbUtilException if the database could not have been opened by
	 *                          Liquibase.
	 */
	public static EntityManagerFactory createAutoclosingEntityManagerFactory(final Class<?> classLoaderSource,
			final String persistenceUnitName) throws RdbUtilException {
		return createAutoclosingEntityManagerFactory(classLoaderSource, persistenceUnitName, null);
	}

	/**
	 * Creates a new {@link EntityManagerFactory} with default-parameters or
	 * parameters given via environment variables.
	 * <p>
	 * Runs Liquibase-update to apply any changes.<br>
	 * Installs a shutdown-hook that ensures that the connection to the database is
	 * properly closed.
	 *
	 * @param classLoaderSource   the source of the class-loader to use
	 * @param persistenceUnitName the name of the persistence-unit to use (from your
	 *                            persistence.xml)
	 * @param masterFileName      the master-file (should not end with -master since
	 *                            this would auto-load it using the non-specific
	 *                            method)
	 * @return an {@link EntityManagerFactory}
	 * @throws RdbUtilException if the database could not have been opened by
	 *                          Liquibase.
	 */
	public static EntityManagerFactory createSpecificAutoclosingEntityManagerFactory(final Class<?> classLoaderSource,
			final String persistenceUnitName, final String masterFileName) throws RdbUtilException {
		return createSpecificAutoclosingEntityManagerFactory(classLoaderSource, persistenceUnitName, null,
				masterFileName);
	}

	/**
	 * Creates a new {@link EntityManagerFactory} with default-parameters or
	 * parameters given via environment variables.
	 * <p>
	 * Runs liquibase-update to apply any changes.<br>
	 * Installs a shutdown-hook that ensures that the connection to the database is
	 * properly closed.
	 *
	 * @param classLoaderSource   the source of the class-loader to use
	 * @param persistenceUnitName the name of the persistence-unit to use (from your
	 *                            persistence.xml)
	 * @param prefix              the prefix
	 * @return an {@link EntityManagerFactory}
	 * @throws RdbUtilException if the database could not have been opened by
	 *                          liquibase.
	 */
	public static EntityManagerFactory createAutoclosingEntityManagerFactory(final Class<?> classLoaderSource,
			final String persistenceUnitName, final String prefix) throws RdbUtilException {
		return createSpecificAutoclosingEntityManagerFactory(classLoaderSource, persistenceUnitName, prefix, "-master");
	}

	/**
	 * Creates a new {@link EntityManagerFactory} with default-parameters or
	 * parameters given via environment variables.
	 * <p>
	 * Runs liquibase-update to apply any changes.<br>
	 * Installs a shutdown-hook that ensures that the connection to the database is
	 * properly closed.
	 *
	 * @param classLoaderSource   the source of the class-loader to use
	 * @param persistenceUnitName the name of the persistence-unit to use (from your
	 *                            persistence.xml)
	 * @param prefix              the prefix
	 * @param masterFileName      the master-file (should not end with -master since
	 *                            this would auto-load it using the non-specific
	 *                            method)
	 * @return an {@link EntityManagerFactory}
	 * @throws RdbUtilException if the database could not have been opened by
	 *                          liquibase.
	 */
	public static EntityManagerFactory createSpecificAutoclosingEntityManagerFactory(final Class<?> classLoaderSource,
			final String persistenceUnitName, final String prefix, final String masterFileName)
			throws RdbUtilException {
		Map<String, String> properties = getProperties(prefix);
		liquibaseUpdate(classLoaderSource, properties, masterFileName);
		EntityManagerFactory factory = Persistence.createEntityManagerFactory(persistenceUnitName, properties);
		ShutdownHook.register(() -> {
			if (factory != null && factory.isOpen())
				factory.close();
		});
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

	public static void liquibaseUpdate(final Class<?> classLoaderSource, final Map<String, String> properties,
			final String masterFileName) throws RdbUtilException {
		Connection connection;
		try {
			log.info("getting connection from DriverManager");
			connection = DriverManager.getConnection(
					properties.get(PROPERTY_NAME_URL) + "?allowPublicKeyRetrieval=true&useSSL=false",
					properties.get(PROPERTY_NAME_USER), properties.get(PROPERTY_NAME_PASSWORD));
			log.info("getting Database from JDBC-connection");
			Database database = DatabaseFactory.getInstance()
					.findCorrectDatabaseImplementation(new JdbcConnection(connection));
			log.info("scanning file-system for master-changelog files");
			List<Path> masterLogFiles = Resources.walk(classLoaderSource,
					path -> path.toString().endsWith(masterFileName + ".xml"));
			for (Path p : masterLogFiles)
				log.info("found file [{}]", p.toString());
			if (masterLogFiles.size() == 0)
				log.info("no master-changelog file found!");
			for (Path changelog : masterLogFiles) {
				log.info("running Liquibase.Update for master-changelog file [{}]", changelog.toString());
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
