package info.unterrainer.commons.rdbutils;

import java.util.Optional;

import lombok.Getter;
import lombok.experimental.Accessors;

@Accessors(fluent = true)
@Getter
public class RdbConfiguration {

	private RdbConfiguration() {
	}

	private String driver;
	private String server;
	private String port;
	private String name;
	private String user;
	private String password;

	public static RdbConfiguration read() {
		return read(null);
	}

	public static RdbConfiguration read(final String prefix) {
		String p = "";
		if (prefix != null)
			p = prefix;
		RdbConfiguration config = new RdbConfiguration();
		config.driver = Optional.ofNullable(System.getenv(p + "DB_DRIVER")).orElse("mariadb");
		config.server = Optional.ofNullable(System.getenv(p + "DB_SERVER")).orElse("10.10.196.4");
		config.port = Optional.ofNullable(System.getenv(p + "DB_PORT")).orElse("3306");
		config.name = Optional.ofNullable(System.getenv(p + "DB_NAME")).orElse("test");
		config.user = Optional.ofNullable(System.getenv(p + "DB_USER")).orElse("test");
		config.password = Optional.ofNullable(System.getenv(p + "DB_PASSWORD")).orElse("test");
		return config;
	}
}
