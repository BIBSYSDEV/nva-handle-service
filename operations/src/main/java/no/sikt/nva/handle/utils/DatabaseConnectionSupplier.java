package no.sikt.nva.handle.utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;
import java.util.function.Supplier;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@JacocoGenerated
public class DatabaseConnectionSupplier {
    public static final String ENV_DATABASE_PASSWORD = "DATABASE_PASSWORD";
    public static final String ENV_DATABASE_USER = "DATABASE_USER";
    public static final String ENV_DATABASE_URI = "DATABASE_URI";
    public static final String USER = "user";
    public static final String PASSWORD = "password";
    public static final String ERROR_CONNECTING_TO_HANDLE_DATABASE = "Error connecting to handle database";
    private static final Logger logger = LoggerFactory.getLogger(DatabaseConnectionSupplier.class);

    private DatabaseConnectionSupplier() {
    }

    private static Connection createConnection(Environment environment) {
        try {
            var connection = DriverManager.getConnection(environment.readEnv(ENV_DATABASE_URI),
                                                         getConnectionProperties(environment));
            connection.setAutoCommit(false);
            return connection;
        } catch (SQLException e) {
            logger.error(ERROR_CONNECTING_TO_HANDLE_DATABASE, e);
            throw new RuntimeException(e);
        }
    }

    public static Supplier<Connection> getConnectionSupplier() {
        return () -> createConnection(new Environment());
    }

    private static Properties getConnectionProperties(Environment environment) {
        final Properties properties = new Properties();
        properties.setProperty(USER, environment.readEnv(ENV_DATABASE_USER));
        properties.setProperty(PASSWORD, environment.readEnv(ENV_DATABASE_PASSWORD));
        return properties;
    }
}
