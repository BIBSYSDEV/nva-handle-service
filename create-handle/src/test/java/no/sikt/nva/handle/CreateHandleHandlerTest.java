package no.sikt.nva.handle;

import com.fasterxml.jackson.core.JsonProcessingException;
import no.sikt.nva.handle.model.CreateHandleRequest;
import no.sikt.nva.handle.model.CreateHandleResponse;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.stubs.FakeContext;
import no.unit.nva.testutils.HandlerRequestBuilder;
import nva.commons.apigateway.GatewayResponse;
import nva.commons.core.Environment;
import nva.commons.core.paths.UriWrapper;
import nva.commons.logutils.LogUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.zalando.problem.Problem;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import static no.sikt.nva.handle.CreateHandleHandler.NULL_URI_ERROR;
import static no.sikt.nva.handle.HandleDatabase.CHARACTER_SLASH;
import static no.sikt.nva.handle.HandleDatabase.CHECK_URL_SQL;
import static no.sikt.nva.handle.HandleDatabase.CREATED_HANDLE_FOR_URI;
import static no.sikt.nva.handle.HandleDatabase.CREATE_ID_SQL;
import static no.sikt.nva.handle.HandleDatabase.ENV_HANDLE_BASE_URI;
import static no.sikt.nva.handle.HandleDatabase.ENV_HANDLE_PREFIX;
import static no.sikt.nva.handle.HandleDatabase.ERROR_CONNECTING_TO_HANDLE_DATABASE;
import static no.sikt.nva.handle.HandleDatabase.ERROR_CREATING_HANDLE_FOR_URI;
import static no.sikt.nva.handle.HandleDatabase.REUSED_EXISTING_HANDLE_FOR_URI;
import static no.sikt.nva.handle.HandleDatabase.SET_HANDLE_SQL;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CreateHandleHandlerTest {

    public static final int EXISTING_HANDLE_ID = 1111;
    public static final int CREATED_HANDLE_ID = 2222;
    private FakeContext context;
    private CreateHandleHandler handler;
    private Connection connection;
    private Environment environment;
    private ByteArrayOutputStream outputStream;


    @BeforeEach
    public void init() {
        this.environment = new Environment();
        this.context = new FakeContext();
        this.outputStream = new ByteArrayOutputStream();
        this.connection = mock(Connection.class);
        var handleDatabase = new HandleDatabase(connection);
        this.handler = new CreateHandleHandler(handleDatabase);
    }

    @AfterEach
    public void tearDown() {

    }

    @Test
    void createHandleRequestReturnsCreatedHandleForValidUri()
            throws IOException, SQLException {
        var uri = randomUri();
        var inputStream = createCreateHandleRequest(uri);
        mockHandleDatabaseCreateHandle(false, true, true);
        var appender = LogUtils.getTestingAppenderForRootLogger();
        handler.handleRequest(inputStream, outputStream, context);
        var response = GatewayResponse.fromOutputStream(outputStream, CreateHandleResponse.class);
        assertThat(appender.getMessages(), containsString(String.format(CREATED_HANDLE_FOR_URI,
                createHandleFromHandleId(CREATED_HANDLE_ID), uri)));
        assertThat(response.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_CREATED)));
        assertThat(response.getBodyObject(CreateHandleResponse.class).getHandle(),
                is(equalTo(createHandleFromHandleId(CREATED_HANDLE_ID))));
    }

    @Test
    void createHandleRequestReturnsExistingHandleForValidUriThatAlreadyHasHandle() throws
            IOException, SQLException {
        var uri = randomUri();
        var inputStream = createCreateHandleRequest(uri);
        mockHandleDatabaseCreateHandle(true, false, false);
        var appender = LogUtils.getTestingAppenderForRootLogger();
        handler.handleRequest(inputStream, outputStream, context);
        var response = GatewayResponse.fromOutputStream(outputStream, CreateHandleResponse.class);
        assertThat(appender.getMessages(), containsString(String.format(REUSED_EXISTING_HANDLE_FOR_URI,
                createHandleFromHandleId(EXISTING_HANDLE_ID), uri)));
        assertThat(response.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_CREATED)));
        assertThat(response.getBodyObject(CreateHandleResponse.class).getHandle(),
                is(equalTo(createHandleFromHandleId(EXISTING_HANDLE_ID))));
    }

    @Test
    void createHandleRequestReturnsBadRequestResponseWhenUriIsNull() throws IOException {
        var inputStream = createCreateHandleRequest(null);
        handler.handleRequest(inputStream, outputStream, context);
        var response = GatewayResponse.fromOutputStream(outputStream, Problem.class);
        assertThat(response.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_BAD_REQUEST)));
        assertThat(response.getBody(), containsString(NULL_URI_ERROR));
    }

    @Test
    void createHandleRequestReturnsBadGatewayAndLogsErrorWhenNotAbleToCreateHandleId() throws IOException,
            SQLException {
        var uri = randomUri();
        var inputStream = createCreateHandleRequest(uri);
        mockHandleDatabaseCreateHandle(false, false, false);
        var appender = LogUtils.getTestingAppenderForRootLogger();
        handler.handleRequest(inputStream, outputStream, context);
        var response = GatewayResponse.fromOutputStream(outputStream, Problem.class);
        assertThat(appender.getMessages(), containsString(String.format(ERROR_CREATING_HANDLE_FOR_URI, uri)));
        assertThat(response.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_BAD_GATEWAY)));
    }

    @Test
    void createHandleRequestReturnsBadGatewayAndLogsErrorAndRollbackDatabaseTransactionWhenNotAbleToCreateHandle()
            throws IOException, SQLException {
        var uri = randomUri();
        var inputStream = createCreateHandleRequest(uri);
        mockHandleDatabaseCreateHandle(false, true, false);
        var appender = LogUtils.getTestingAppenderForRootLogger();
        handler.handleRequest(inputStream, outputStream, context);
        var response = GatewayResponse.fromOutputStream(outputStream, Problem.class);
        assertThat(appender.getMessages(), containsString(String.format(ERROR_CREATING_HANDLE_FOR_URI, uri)));
        assertThat(response.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_BAD_GATEWAY)));
        verify(connection, times(1)).rollback();
    }

    @Test
    void createHandleRequestThrowsRuntimeExceptionAndLogsErrorWhenNotAbleToConnectToHandleDatabase() {
        var appender = LogUtils.getTestingAppenderForRootLogger();
        assertThrows(RuntimeException.class, () -> handler = new CreateHandleHandler());
        assertThat(appender.getMessages(), containsString(ERROR_CONNECTING_TO_HANDLE_DATABASE));
    }

    private InputStream createCreateHandleRequest(URI uri) throws JsonProcessingException {
        CreateHandleRequest request = new CreateHandleRequest(uri);
        return new HandlerRequestBuilder<CreateHandleRequest>(JsonUtils.dtoObjectMapper)
                .withBody(request)
                .build();
    }

    private URI createHandleFromHandleId(int handleId) {
        return UriWrapper.fromHost(environment.readEnv(ENV_HANDLE_BASE_URI))
                .addChild(environment.readEnv(ENV_HANDLE_PREFIX), Integer.toString(handleId)).getUri();
    }

    private void mockHandleDatabaseCreateHandle(boolean uriAlreadyExists, boolean successfulCreateHandleId,
                                                boolean successfulCreateHandle) throws SQLException {
        PreparedStatement preparedStatementCheckUrl = createPreparedStatementCheckUrl(uriAlreadyExists);
        when(connection.prepareStatement(CHECK_URL_SQL)).thenReturn(preparedStatementCheckUrl);

        PreparedStatement preparedStatementCreateId = createPreparedStatementCreateId(successfulCreateHandleId);
        when(connection.prepareStatement(CREATE_ID_SQL)).thenReturn(preparedStatementCreateId);

        PreparedStatement preparedStatementSetHandle = createPreparedStatementSetHandle(successfulCreateHandle);
        when(connection.prepareStatement(SET_HANDLE_SQL)).thenReturn(preparedStatementSetHandle);
    }

    private PreparedStatement createPreparedStatementCheckUrl(boolean uriAlreadyExists) throws SQLException {
        PreparedStatement preparedStatement = mock(PreparedStatement.class);
        ResultSet resultSet = mock(ResultSet.class);
        when(resultSet.next()).thenReturn(uriAlreadyExists);
        if (uriAlreadyExists) {
            when(resultSet.getString(1)).thenReturn(createExistingHandleLocalPart());
        }
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        return preparedStatement;
    }

    private String createExistingHandleLocalPart() {
        return environment.readEnv(ENV_HANDLE_PREFIX) + CHARACTER_SLASH + EXISTING_HANDLE_ID;
    }

    private PreparedStatement createPreparedStatementCreateId(boolean success) throws SQLException {
        PreparedStatement preparedStatement = mock(PreparedStatement.class);
        ResultSet resultSet = mock(ResultSet.class);
        when(resultSet.next()).thenReturn(success);
        when(resultSet.getInt(anyInt())).thenReturn(CREATED_HANDLE_ID);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        return preparedStatement;
    }

    private PreparedStatement createPreparedStatementSetHandle(boolean success) throws SQLException {
        PreparedStatement preparedStatement = mock(PreparedStatement.class);
        if (success) {
            when(preparedStatement.executeUpdate()).thenReturn(1);
        } else {
            when(preparedStatement.executeUpdate()).thenThrow(new SQLException());
        }
        return preparedStatement;
    }

}