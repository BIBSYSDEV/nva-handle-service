package no.sikt.nva.handle;

import static no.sikt.nva.handle.HandleDatabase.CHARACTER_SLASH;
import static no.sikt.nva.handle.HandleDatabase.CHECK_URL_SQL;
import static no.sikt.nva.handle.HandleDatabase.CREATE_ID_SQL;
import static no.sikt.nva.handle.HandleDatabase.ENV_HANDLE_BASE_URI;
import static no.sikt.nva.handle.HandleDatabase.ENV_HANDLE_PREFIX;
import static no.sikt.nva.handle.HandleDatabase.SET_HANDLE_AND_URI_BY_ID_SQL;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import java.net.URI;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import nva.commons.core.Environment;
import nva.commons.core.paths.UriWrapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class HandleDatabaseTest {

    private static final String HANDLE_BASE_URI = "https://hdl.handle.net";
    private static final String DEFAULT_PREFIX = "11250.1";
    private static final String CUSTOM_PREFIX = "12345.6";
    private static final int GENERATED_HANDLE_ID = 7890;
    private static final int EXISTING_HANDLE_ID = 1111;

    private HandleDatabase handleDatabase;
    private Connection connection;

    @BeforeEach
    void setUp() {
        var environment = mock(Environment.class);
        when(environment.readEnv(ENV_HANDLE_BASE_URI)).thenReturn(HANDLE_BASE_URI);
        when(environment.readEnv(ENV_HANDLE_PREFIX)).thenReturn(DEFAULT_PREFIX);

        connection = mock(Connection.class);
        handleDatabase = new HandleDatabase(environment);
    }

    @Test
    void shouldCreateHandleWithCustomPrefixWhenNoExistingHandleExists() throws SQLException {
        mockHandleDatabaseForNewHandle();

        var result = handleDatabase.createHandle(CUSTOM_PREFIX, randomUri(), connection);

        var expectedHandle = createExpectedHandle(Integer.toString(GENERATED_HANDLE_ID));

        assertThat(result, is(equalTo(expectedHandle)));
    }

    @Test
    void shouldReturnExistingHandleWhenCreatingHandleWithCustomPrefixAndHandleExists() throws SQLException {
        mockHandleDatabaseForExistingHandle();

        var result = handleDatabase.createHandle(CUSTOM_PREFIX, randomUri(), connection);

        var expectedHandle = createExpectedHandle(Integer.toString(EXISTING_HANDLE_ID));

        assertThat(result, is(equalTo(expectedHandle)));
    }

    @Test
    void shouldCreateHandleWithCustomPrefixAndSuffixGeneratedByDatabaseId() throws SQLException {
        mockHandleDatabaseForNewHandle();

        var result = handleDatabase.createHandle(CUSTOM_PREFIX, randomUri(), connection);

        var expectedSuffix = Integer.toString(GENERATED_HANDLE_ID);
        assertThat(result.toString(), containsString(CUSTOM_PREFIX + CHARACTER_SLASH + expectedSuffix));
    }

    @Test
    void shouldThrowRuntimeExceptionWhenDatabaseFailsToCreateHandleId() throws SQLException {
        var uri = randomUri();
        mockHandleDatabaseForFailedHandleCreation();

        assertThrows(RuntimeException.class,
                                    () -> handleDatabase.createHandle(CUSTOM_PREFIX, uri, connection));
    }

    private void mockHandleDatabaseForNewHandle() throws SQLException {
        var checkUrlStatement = createPreparedStatementCheckUrl(false, null, 0);
        when(connection.prepareStatement(CHECK_URL_SQL)).thenReturn(checkUrlStatement);

        var createIdStatement = createPreparedStatementCreateId();
        when(connection.prepareStatement(CREATE_ID_SQL)).thenReturn(createIdStatement);

        var setHandleStatement = createPreparedStatementSetHandle();
        when(connection.prepareStatement(SET_HANDLE_AND_URI_BY_ID_SQL)).thenReturn(setHandleStatement);
    }

    private void mockHandleDatabaseForExistingHandle() throws SQLException {
        var checkUrlStatement = createPreparedStatementCheckUrl(true, HandleDatabaseTest.CUSTOM_PREFIX,
                                                                              HandleDatabaseTest.EXISTING_HANDLE_ID);
        when(connection.prepareStatement(CHECK_URL_SQL)).thenReturn(checkUrlStatement);
    }

    private void mockHandleDatabaseForFailedHandleCreation() throws SQLException {
        var checkUrlStatement = createPreparedStatementCheckUrl(false, null, 0);
        when(connection.prepareStatement(CHECK_URL_SQL)).thenReturn(checkUrlStatement);

        var createIdStatement = createPreparedStatementCreateIdFailing();
        when(connection.prepareStatement(CREATE_ID_SQL)).thenReturn(createIdStatement);
    }

    private PreparedStatement createPreparedStatementCheckUrl(boolean uriAlreadyExists,
                                                             String prefix,
                                                             int handleId) throws SQLException {
        var preparedStatement = mock(PreparedStatement.class);
        var resultSet = mock(ResultSet.class);
        when(resultSet.next()).thenReturn(uriAlreadyExists);
        if (uriAlreadyExists) {
            String existingHandleLocalPart = prefix + CHARACTER_SLASH + handleId;
            doReturn(existingHandleLocalPart).when(resultSet).getString(anyInt());
        }
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        return preparedStatement;
    }

    private PreparedStatement createPreparedStatementCreateId() throws SQLException {
        var preparedStatement = mock(PreparedStatement.class);
        var resultSet = mock(ResultSet.class);
        when(resultSet.next()).thenReturn(true);
        when(resultSet.getInt(anyInt())).thenReturn(GENERATED_HANDLE_ID);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        return preparedStatement;
    }

    private PreparedStatement createPreparedStatementCreateIdFailing() throws SQLException {
        var preparedStatement = mock(PreparedStatement.class);
        var resultSet = mock(ResultSet.class);
        when(resultSet.next()).thenReturn(false);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        return preparedStatement;
    }

    private PreparedStatement createPreparedStatementSetHandle() throws SQLException {
        var preparedStatement = mock(PreparedStatement.class);
        when(preparedStatement.executeUpdate()).thenReturn(1);
        return preparedStatement;
    }

    private URI createExpectedHandle(String suffix) {
        return UriWrapper.fromHost(HANDLE_BASE_URI)
                   .addChild(HandleDatabaseTest.CUSTOM_PREFIX, suffix)
                   .getUri();
    }
}
