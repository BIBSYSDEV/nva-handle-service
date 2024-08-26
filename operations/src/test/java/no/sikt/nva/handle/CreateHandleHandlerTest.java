package no.sikt.nva.handle;

import com.fasterxml.jackson.core.JsonProcessingException;
import java.util.function.Supplier;
import no.sikt.nva.handle.exceptions.CreateHandleException;
import no.sikt.nva.handle.model.HandleRequest;
import no.sikt.nva.handle.model.HandleResponse;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.stubs.FakeContext;
import no.unit.nva.testutils.HandlerRequestBuilder;
import nva.commons.apigateway.GatewayResponse;
import nva.commons.core.Environment;
import nva.commons.core.paths.UriWrapper;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import nva.commons.logutils.LogUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.zalando.problem.Problem;

import static no.sikt.nva.handle.CreateHandleHandler.NULL_URI_ERROR;
import static no.sikt.nva.handle.HandleDatabase.CHARACTER_SLASH;
import static no.sikt.nva.handle.HandleDatabase.CHECK_URL_SQL;
import static no.sikt.nva.handle.HandleDatabase.CREATED_HANDLE_FOR_URI;
import static no.sikt.nva.handle.HandleDatabase.CREATE_ID_SQL;
import static no.sikt.nva.handle.HandleDatabase.ENV_HANDLE_BASE_URI;
import static no.sikt.nva.handle.HandleDatabase.ENV_HANDLE_PREFIX;
import static no.sikt.nva.handle.HandleDatabase.ERROR_CREATING_HANDLE_FOR_URI;
import static no.sikt.nva.handle.HandleDatabase.REUSED_EXISTING_HANDLE_FOR_URI;
import static no.sikt.nva.handle.HandleDatabase.SET_HANDLE_SQL;
import static no.sikt.nva.handle.utils.DatabaseConnectionSupplier.ENV_DATABASE_PASSWORD;
import static no.sikt.nva.handle.utils.DatabaseConnectionSupplier.ENV_DATABASE_URI;
import static no.sikt.nva.handle.utils.DatabaseConnectionSupplier.ENV_DATABASE_USER;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doReturn;
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
    public static final String ENV_ALLOWED_ORIGIN = "ALLOWED_ORIGIN";
    public static final String ENV_API_HOST = "API_HOST";

    @BeforeEach
    public void init() {
        this.environment = mock(Environment.class);
        when(environment.readEnv(ENV_HANDLE_BASE_URI)).thenReturn("https://hdl.handle.net");
        when(environment.readEnv(ENV_HANDLE_PREFIX)).thenReturn("11250.1");
        when(environment.readEnv(ENV_DATABASE_URI)).thenReturn("database_uri");
        when(environment.readEnv(ENV_DATABASE_USER)).thenReturn("database_user");
        when(environment.readEnv(ENV_DATABASE_PASSWORD)).thenReturn("database_password");
        when(environment.readEnv(ENV_API_HOST)).thenReturn("api.localhost.nva.aws.unit.no");
        when(environment.readEnv(ENV_ALLOWED_ORIGIN)).thenReturn("*");

        this.context = new FakeContext();
        this.outputStream = new ByteArrayOutputStream();
        this.connection = mock(Connection.class);
        this.handler = new CreateHandleHandler(environment, () -> connection);
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
        var response = GatewayResponse.fromOutputStream(outputStream, HandleResponse.class);
        assertThat(appender.getMessages(), containsString(String.format(CREATED_HANDLE_FOR_URI,
                                                                        createHandleFromHandleId(CREATED_HANDLE_ID),
                                                                        uri)));
        assertThat(response.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_CREATED)));
        assertThat(response.getBodyObject(HandleResponse.class).handle(),
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
        var response = GatewayResponse.fromOutputStream(outputStream, HandleResponse.class);
        assertThat(appender.getMessages(), containsString(String.format(REUSED_EXISTING_HANDLE_FOR_URI,
                                                                        createHandleFromHandleId(EXISTING_HANDLE_ID),
                                                                        uri)));
        assertThat(response.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_CREATED)));
        assertThat(response.getBodyObject(HandleResponse.class).handle(),
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
    void createHandleRequestThrowsHandleExceptionAndLogsErrorWhenNotAbleToConnectToHandleDatabase() {
        var appender = LogUtils.getTestingAppenderForRootLogger();
        @SuppressWarnings("unchecked") var connectionSupplier = (Supplier<Connection>) mock(Supplier.class);
        var uri = randomUri();
        var failure = "some connection failure";
        when(connectionSupplier.get()).thenThrow(new RuntimeException(new SQLException(failure)));
        var failingHandler = new CreateHandleHandler(environment, connectionSupplier);
        var request = new HandleRequest(uri);
        assertThrows(CreateHandleException.class,
                     () -> failingHandler.processInput(request, null, null));
        assertThat(appender.getMessages(), containsString(failure));
    }

    private InputStream createCreateHandleRequest(URI uri) throws JsonProcessingException {
        HandleRequest request = new HandleRequest(uri);
        return new HandlerRequestBuilder<HandleRequest>(JsonUtils.dtoObjectMapper)
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
            doReturn(createExistingHandleLocalPart()).when(resultSet).getString(anyInt());
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