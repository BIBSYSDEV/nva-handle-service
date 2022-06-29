package no.sikt.nva.handle;

import no.sikt.nva.handle.exceptions.CreateHandleException;
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
    public static final String ERROR_CONNECTING_TO_HANDLE_DATABASE = "Error connecting to handle database";
    public static final String REUSED_EXISTING_HANDLE_FOR_URI = "Reused existing handle '%s' for URI '%s'";
    public static final String CREATED_HANDLE_FOR_URI = "Created handle '%s' for URI '%s'";
    public static final String ERROR_CREATING_HANDLE_FOR_URI = "Error creating handle for URI '%s'";
    public static final String CHARACTER_SLASH = "/";
    public static final String ENV_DATABASE_PASSWORD = "DATABASE_PASSWORD";
    public static final String ENV_DATABASE_USER = "DATABASE_USER";
    public static final String ENV_HANDLE_PREFIX = "HANDLE_PREFIX";
    public static final String ENV_HANDLE_HOST = "HANDLE_HOST";
    public static final String ENV_DATABASE_URI = "DATABASE_URI";
    public static final String USER = "user";
    public static final String PASSWORD = "password";

    private final Environment environment = new Environment();
    private final Connection connection;

    @JacocoGenerated
    public HandleDatabase() {
        try {
            this.connection = DriverManager.getConnection(environment.readEnv(ENV_DATABASE_URI),
                    getConnectionProperties());
        } catch (SQLException e) {
            logger.error(ERROR_CONNECTING_TO_HANDLE_DATABASE, e);
            throw new RuntimeException(e);
        }
    }

    public HandleDatabase(Connection connection) {
        this.connection = connection;
    }

    public URI createHandle(URI uri) throws CreateHandleException {
        try (connection;
             PreparedStatement preparedStatementCreate = connection.prepareStatement(CREATE_ID_SQL);
             PreparedStatement preparedStatementUpdate = connection.prepareStatement(SET_HANDLE_SQL);
             PreparedStatement preparedStatementCheckUrl = connection.prepareStatement(CHECK_URL_SQL);
        ) {
            preparedStatementCheckUrl.setString(1, uri.toString());
            try (ResultSet existingResult = preparedStatementCheckUrl.executeQuery()) {
                if (existingResult.next()) {
                    final String existingHandleString = existingResult.getString(1);
                    URI existingHandle = UriWrapper.fromUri(environment.readEnv(ENV_HANDLE_HOST))
                            .addChild(existingHandleString).getUri();
                    logger.info(String.format(REUSED_EXISTING_HANDLE_FOR_URI, existingHandle, uri));
                    return existingHandle;
                }
                return createNewHandle(uri, preparedStatementCreate, preparedStatementUpdate);
            }
        } catch (SQLException e) {
            logger.error(String.format(ERROR_CREATING_HANDLE_FOR_URI, uri), e);
            throw new CreateHandleException(String.format(ERROR_CREATING_HANDLE_FOR_URI, uri));
        }
    }

    private URI createNewHandle(URI uri, PreparedStatement preparedStatementCreate,
                                PreparedStatement preparedStatementUpdate) throws SQLException, CreateHandleException {
        connection.setAutoCommit(false);
        try (ResultSet createResult = preparedStatementCreate.executeQuery()) {
            int generatedId;
            if (createResult.next()) {
                generatedId = createResult.getInt(1);
                preparedStatementCreate.close();
                final String handleLocalPart = createHandleLocalPart(generatedId);
                preparedStatementUpdate.setString(1, handleLocalPart);
                preparedStatementUpdate.setString(2, uri.toString());
                preparedStatementUpdate.setInt(3, generatedId);
                preparedStatementUpdate.executeUpdate();
                connection.commit();
                URI handle = UriWrapper.fromUri(environment.readEnv(ENV_HANDLE_HOST)).addChild(handleLocalPart)
                        .getUri();
                logger.info(String.format(CREATED_HANDLE_FOR_URI, handle, uri));
                return handle;
            } else {
                throw new CreateHandleException(String.format(ERROR_CREATING_HANDLE_FOR_URI, uri));
            }
        } catch (SQLException e) {
            connection.rollback();
            throw e;
        }
    }

    private String createHandleLocalPart(int generatedId) {
        return environment.readEnv(ENV_HANDLE_PREFIX) + CHARACTER_SLASH + generatedId;
    }

    @JacocoGenerated
    private Properties getConnectionProperties() {
        final Properties properties = new Properties();
        properties.setProperty(USER, environment.readEnv(ENV_DATABASE_USER));
        properties.setProperty(PASSWORD, environment.readEnv(ENV_DATABASE_PASSWORD));
        return properties;
    }
}
