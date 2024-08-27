package no.sikt.nva.handle.utils;

import static nva.commons.secrets.SecretsReader.defaultSecretsManagerClient;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;
import java.util.function.Supplier;
import no.sikt.nva.handle.model.HandleDatabaseSecrets;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;
import nva.commons.secrets.SecretsReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@JacocoGenerated
public final class DatabaseConnectionSupplier {
    public static final String USER = "user";
    public static final String PASSWORD = "password";
    public static final String ERROR_CONNECTING_TO_HANDLE_DATABASE = "Error connecting to handle database";
    private static final Logger logger = LoggerFactory.getLogger(DatabaseConnectionSupplier.class);
    public static final String ENV_HANDLE_DATABASE_SECRET_NAME = "HANDLE_DATABASE_SECRET_NAME";
    private DatabaseConnectionSupplier() {
    }

    private static Connection createConnection(Environment environment, SecretsReader secretsReader) {
        try {
            var dbSecrets = secretsReader.fetchClassSecret(environment.readEnv(ENV_HANDLE_DATABASE_SECRET_NAME),
                                                           HandleDatabaseSecrets.class);
            var connection = DriverManager.getConnection(dbSecrets.uri(),
                                                         getConnectionProperties(dbSecrets));
            connection.setAutoCommit(false);
            return connection;
        } catch (SQLException e) {
            logger.error(ERROR_CONNECTING_TO_HANDLE_DATABASE, e);
            throw new RuntimeException(e);
        }
    }

    public static Supplier<Connection> getConnectionSupplier() {
        return () -> createConnection(new Environment(),  new SecretsReader(defaultSecretsManagerClient()));
    }

    private static Properties getConnectionProperties(HandleDatabaseSecrets dbSecrets) {
        final Properties properties = new Properties();
        properties.setProperty(USER, dbSecrets.user());
        properties.setProperty(PASSWORD, dbSecrets.password());
        return properties;
    }
}
