package no.sikt.nva.handle;

import no.unit.nva.stubs.FakeContext;
import nva.commons.core.Environment;
import nva.commons.core.paths.UriWrapper;
import nva.commons.logutils.LogUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import static no.sikt.nva.handle.HandleDatabase.CHECK_URL_SQL;
import static no.sikt.nva.handle.HandleDatabase.CREATED_HANDLE_FOR_URI;
import static no.sikt.nva.handle.HandleDatabase.CREATE_ID_SQL;
import static no.sikt.nva.handle.HandleDatabase.ERROR_CREATING_HANDLE_FOR_URI;
import static no.sikt.nva.handle.HandleDatabase.REUSING_EXISTING_HANDLE_FOR_URI;
import static no.sikt.nva.handle.HandleDatabase.SET_HANDLE_SQL;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CreateHandleHandlerTest {

    private static final String DUMMY_LANDING_PAGE_HOST = "www.example.com";
    public static final int EXISTING_HANDLE_ID = 1111;
    public static final int CREATED_HANDLE_ID = 2222;
    private FakeContext context;
    private CreateHandleHandler handler;
    private Connection connection;
    private Environment environment;

    @BeforeEach
    public void init() {
        this.context = new FakeContext();
        this.connection = mock(Connection.class);
        this.handler = new CreateHandleHandler(connection);
        this.environment = new Environment();
    }

    @AfterEach
    public void tearDown() {

    }

    @Test
    void handleRequestShouldCreateHandleForPublicationUriWhenPublicationUriDoesNotAlreadyHaveHandle()
            throws SQLException {
        var publicationIdentifier = randomString();
        var uri = UriWrapper.fromHost(DUMMY_LANDING_PAGE_HOST).addChild(publicationIdentifier).getUri();
        var event = createCreateHandleForPublicationEvent(publicationIdentifier, uri);
        mockHandleDatabaseCreateHandle(false, true);
        var appender = LogUtils.getTestingAppenderForRootLogger();
        handler.handleRequest(event, context);
        assertThat(appender.getMessages(), containsString(String.format(CREATED_HANDLE_FOR_URI,
                createHandleUriFromHandleId(CREATED_HANDLE_ID), uri)));
    }

    @Test
    void handleRequestShouldReuseHandleForPublicationUriWhenPublicationUriAlreadyHasHandle() throws SQLException {
        var publicationIdentifier = randomString();
        var uri = UriWrapper.fromHost(DUMMY_LANDING_PAGE_HOST).addChild(publicationIdentifier).getUri();
        var event = createCreateHandleForPublicationEvent(publicationIdentifier, uri);
        mockHandleDatabaseCreateHandle(true, false);
        var appender = LogUtils.getTestingAppenderForRootLogger();
        handler.handleRequest(event, context);
        assertThat(appender.getMessages(), containsString(String.format(REUSING_EXISTING_HANDLE_FOR_URI,
                createHandleUriFromHandleId(EXISTING_HANDLE_ID), uri)));
    }

    @Test
    void handleRequestShouldLogErrorAndThrowRuntimeExceptionWhenNotAbleToCreateHandle() throws SQLException {
        var publicationIdentifier = randomString();
        var uri = UriWrapper.fromHost(DUMMY_LANDING_PAGE_HOST).addChild(publicationIdentifier).getUri();
        var event = createCreateHandleForPublicationEvent(publicationIdentifier, uri);
        mockHandleDatabaseCreateHandle(false, false);
        var appender = LogUtils.getTestingAppenderForRootLogger();
        assertThrows(RuntimeException.class, () -> handler.handleRequest(event, context));
        assertThat(appender.getMessages(), containsString(String.format(ERROR_CREATING_HANDLE_FOR_URI, uri)));
    }

    private CreateHandleForPublicationEvent createCreateHandleForPublicationEvent(String publicationIdentifier,
                                                                                  URI publicationUri) {
        return new CreateHandleForPublicationEvent(publicationIdentifier, publicationUri);
    }

    private URI createHandleUriFromHandleId(int handleId) {
        return UriWrapper.fromHost(environment.readEnv("HANDLE_HOST"))
                .addChild(environment.readEnv("HANDLE_PREFIX"), Integer.toString(handleId)).getUri();
    }

    private void mockHandleDatabaseCreateHandle(boolean uriAlreadyExists, boolean successfulCreate)
            throws SQLException {
        PreparedStatement preparedStatementCheckUrl = createPreparedStatementCheckUrl(uriAlreadyExists);
        when(connection.prepareStatement(CHECK_URL_SQL)).thenReturn(preparedStatementCheckUrl);

        PreparedStatement preparedStatementCreateId = createPreparedStatementCreateId(successfulCreate);
        when(connection.prepareStatement(CREATE_ID_SQL)).thenReturn(preparedStatementCreateId);

        PreparedStatement preparedStatementSetHandle = createPreparedStatementSetHandle();
        when(connection.prepareStatement(SET_HANDLE_SQL)).thenReturn(preparedStatementSetHandle);
    }

    private PreparedStatement createPreparedStatementCheckUrl(boolean uriAlreadyExists) throws SQLException {
        PreparedStatement preparedStatement = mock(PreparedStatement.class);
        ResultSet resultSet = mock(ResultSet.class);
        when(resultSet.next()).thenReturn(uriAlreadyExists);
        if (uriAlreadyExists) {
            when(resultSet.getString(1)).thenReturn(createExistingHandleString());
        }
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        return preparedStatement;
    }

    private String createExistingHandleString() {
        return environment.readEnv("HANDLE_PREFIX") + "/" + EXISTING_HANDLE_ID;
    }

    private PreparedStatement createPreparedStatementCreateId(boolean success) throws SQLException {
        PreparedStatement preparedStatement = mock(PreparedStatement.class);
        ResultSet resultSet = mock(ResultSet.class);
        when(resultSet.next()).thenReturn(success);
        when(resultSet.getInt(anyInt())).thenReturn(CREATED_HANDLE_ID);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        return preparedStatement;
    }

    private PreparedStatement createPreparedStatementSetHandle() throws SQLException {
        PreparedStatement preparedStatement = mock(PreparedStatement.class);
        when(preparedStatement.executeUpdate()).thenReturn(1);
        return preparedStatement;
    }

}