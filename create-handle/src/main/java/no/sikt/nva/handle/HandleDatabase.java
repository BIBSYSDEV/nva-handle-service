package no.sikt.nva.handle;

import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.paths.UriWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Properties;

public class HandleDatabase {
    private static final Logger logger = LoggerFactory.getLogger(HandleDatabase.class);
    public static final String CREATE_ID_SQL = "INSERT INTO handle(date_created) VALUES ( current_timestamp ) "
            + "RETURNING handle_id";
    public static final String SET_HANDLE_SQL = "UPDATE handle SET handle =  ?, url = ? WHERE handle_id = ?";
    public static final String CHECK_URL_SQL = "SELECT handle FROM handle WHERE url = ?";
    public static final String REUSING_EXISTING_HANDLE_FOR_URI = "Reusing existing handle '%s' for URI '%s'";
    public static final String CREATED_HANDLE_FOR_URI = "Created handle '%s' for URI '%s'";
    public static final String ERROR_CREATING_HANDLE_FOR_URI = "Error creating handle for URI '%s'";
    private static final String HANDLE_SERVER_URL = new Environment().readEnv("HANDLE_HOST");
    private static final String HANDLE_PREFIX = new Environment().readEnv("HANDLE_PREFIX");
    private static final String DATABASE_URI = new Environment().readEnv("DATABASE_URI");
    private static final String DATABASE_USER = new Environment().readEnv("DATABASE_USER");
    private static final String DATABASE_PASSWORD = new Environment().readEnv("DATABASE_PASSWORD");
    private final Connection connection;

    @JacocoGenerated
    public HandleDatabase() {
        try {
            this.connection = DriverManager.getConnection(DATABASE_URI, getConnectionProperties());
        } catch (SQLException e) {
            logger.error("Error connecting to handle database {}", DATABASE_URI, e);
            throw new RuntimeException(e);
        }
    }

    public HandleDatabase(Connection connection) {
        this.connection = connection;
    }

    public URI createHandle(URI uri) {
        final URI handle;
        try (
                PreparedStatement preparedStatementCreate = connection.prepareStatement(CREATE_ID_SQL);
                PreparedStatement preparedStatementUpdate = connection.prepareStatement(SET_HANDLE_SQL);
                PreparedStatement preparedStatementCheckUrl = connection.prepareStatement(CHECK_URL_SQL);
        ) {
            connection.setAutoCommit(false);
            preparedStatementCheckUrl.setString(1, uri.toString());
            try (ResultSet existingResult = preparedStatementCheckUrl.executeQuery()) {
                if (existingResult.next()) {
                    final String existingHandle = existingResult.getString(1);
                    preparedStatementCheckUrl.close();
                    handle = UriWrapper.fromUri(HANDLE_SERVER_URL).addChild(existingHandle).getUri();
                    logger.info(String.format(REUSING_EXISTING_HANDLE_FOR_URI, handle, uri));
                } else {
                    try (ResultSet createResult = preparedStatementCreate.executeQuery()) {
                        int generatedId;
                        if (createResult.next()) {
                            generatedId = createResult.getInt(1);
                            preparedStatementCreate.close();
                            final String handleLocalPart = HANDLE_PREFIX + "/" + generatedId;
                            preparedStatementUpdate.setString(1, handleLocalPart);
                            preparedStatementUpdate.setString(2, uri.toString());
                            preparedStatementUpdate.setInt(3, generatedId);
                            preparedStatementUpdate.executeUpdate();
                            handle = UriWrapper.fromUri(HANDLE_SERVER_URL).addChild(handleLocalPart).getUri();
                            logger.info(String.format(CREATED_HANDLE_FOR_URI, handle, uri));
                        } else {
                            throw new RuntimeException();
                        }
                        connection.commit();
                    }
                }
            }
        } catch (Exception e) {
            logger.error(String.format(ERROR_CREATING_HANDLE_FOR_URI, uri));
            throw new RuntimeException(e);
        }
        return handle;
    }

    @JacocoGenerated
    private Properties getConnectionProperties() {
        final Properties properties = new Properties();
        properties.setProperty("user", DATABASE_USER);
        properties.setProperty("password", DATABASE_PASSWORD);
        return properties;
    }
}
