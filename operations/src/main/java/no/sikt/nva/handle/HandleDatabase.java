package no.sikt.nva.handle;

import static java.util.Objects.isNull;
import java.util.Optional;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.paths.UriWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

@JacocoGenerated
public class HandleDatabase {

    private static final Logger logger = LoggerFactory.getLogger(HandleDatabase.class);
    public static final String CREATE_ID_SQL = "INSERT INTO handle(date_created) VALUES ( current_timestamp ) "
                                               + "RETURNING handle_id";
    public static final String SET_HANDLE_SQL = "UPDATE handle SET handle =  ?, url = ? WHERE handle_id = ?";
    public static final String SET_URI_SQL = "UPDATE handle SET url = ? WHERE handle = ?";
    public static final String CHECK_URL_SQL = "SELECT handle FROM handle WHERE url = ?";
    public static final String REUSED_EXISTING_HANDLE_FOR_URI = "Reused existing handle '%s' for URI '%s'";
    public static final String CREATED_HANDLE_FOR_URI = "Created handle '%s' for URI '%s'";
    public static final String ERROR_CREATING_HANDLE_FOR_URI = "Error creating handle for URI '%s'";
    public static final String ERROR_UPDATING_HANDLE_FOR_URI = "Error updating handle '%s/%s'";
    public static final String CHARACTER_SLASH = "/";
    public static final String ENV_HANDLE_PREFIX = "HANDLE_PREFIX";
    public static final String ENV_HANDLE_BASE_URI = "HANDLE_BASE_URI";
    public static final int ONE_ROW = 1;

    private final Environment environment;

    public HandleDatabase(Environment environment) {
        this.environment = environment;
    }

    public URI createHandle(URI uri, Connection connection) throws SQLException {
        var existingHandle = fetchExistingHandleByValue(uri, connection);
        if (existingHandle.isPresent()) {
            logger.info(String.format(REUSED_EXISTING_HANDLE_FOR_URI, existingHandle.get(), uri));
            return existingHandle.get();
        } else {
            return createNewHandle(uri, connection);
        }
    }

    public URI updateHandle(String prefix, String suffix, URI uri, Connection connection) throws SQLException {
        try {
            var handleLocalPart = executeUpdateUri(uri, prefix + CHARACTER_SLASH + suffix, connection);

            return convertShortHandleToFull(handleLocalPart);
        } catch (SQLException e) {
            var message = String.format(ERROR_UPDATING_HANDLE_FOR_URI, prefix, suffix) + (isNull(e.getMessage()) ?
                                                                                              "" : ": ");
            logger.error(message, e);
            throw new SQLException(message);
        }
    }

    private Optional<URI> fetchExistingHandleByValue(URI value, Connection connection) throws SQLException {
        try (PreparedStatement preparedStatementCheckUrl = connection.prepareStatement(CHECK_URL_SQL)) {
            preparedStatementCheckUrl.setString(1, value.toString());
            try (ResultSet existingResult = preparedStatementCheckUrl.executeQuery()) {
                if (!existingResult.next()) {
                    return Optional.empty();
                }
                final String existingHandleString = existingResult.getString(1);
                return Optional.of(UriWrapper.fromUri(environment.readEnv(ENV_HANDLE_BASE_URI))
                           .addChild(existingHandleString).getUri());
            }
        }
    }

    private URI createNewHandle(URI uri, Connection connection) throws SQLException {

        try (PreparedStatement preparedStatementCreate = connection.prepareStatement(CREATE_ID_SQL);

            var createResult = preparedStatementCreate.executeQuery()) {
            if (createResult.next()) {
                var handleId = createResult.getInt(1);
                final String handleLocalPart = executeUpdateHandle(uri, handleId, connection);

                URI handle = convertShortHandleToFull(handleLocalPart);
                logger.info(String.format(CREATED_HANDLE_FOR_URI, handle, uri));
                return handle;
            } else {
                throw new RuntimeException(String.format(ERROR_CREATING_HANDLE_FOR_URI, uri));
            }
        }
    }

    private URI convertShortHandleToFull(String handleLocalPart) {
        return UriWrapper.fromUri(environment.readEnv(ENV_HANDLE_BASE_URI)).addChild(handleLocalPart)
                   .getUri();
    }

    private String executeUpdateHandle(URI uri, int handleId, Connection connection) throws SQLException {
        try (PreparedStatement preparedStatementUpdate = connection.prepareStatement(SET_HANDLE_SQL)) {
            final String handleLocalPart = convertIdToShortHandle(handleId);
            preparedStatementUpdate.setString(1, handleLocalPart);
            preparedStatementUpdate.setString(2, uri.toString());
            preparedStatementUpdate.setInt(3, handleId);
            executeSingleRowUpdate(preparedStatementUpdate);
            return handleLocalPart;
        }
    }

    private String executeUpdateUri(URI uri, String handleLocalPart, Connection connection) throws SQLException {
        try (PreparedStatement preparedStatementUpdate = connection.prepareStatement(SET_URI_SQL)) {
            preparedStatementUpdate.setString(1, uri.toString());
            preparedStatementUpdate.setString(2, handleLocalPart);
            executeSingleRowUpdate(preparedStatementUpdate);
            return handleLocalPart;
        }
    }

    private void executeSingleRowUpdate(PreparedStatement preparedStatement) throws SQLException {
        var numberOfRows = preparedStatement.executeUpdate();
        if (numberOfRows != ONE_ROW) {
            throw new IllegalStateException(String.format("Expected one row to be updated, but got %s for query \"%s\"",
                                            numberOfRows, preparedStatement));
        }
    }

    private String convertIdToShortHandle(int generatedId) {
        return environment.readEnv(ENV_HANDLE_PREFIX) + CHARACTER_SLASH + generatedId;
    }
}
