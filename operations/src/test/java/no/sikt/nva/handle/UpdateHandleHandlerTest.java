package no.sikt.nva.handle;

import static no.sikt.nva.handle.CreateHandleHandler.NULL_URI_ERROR;
import static no.sikt.nva.handle.HandleDatabase.ENV_HANDLE_BASE_URI;
import static no.sikt.nva.handle.HandleDatabase.ENV_HANDLE_PREFIX;
import static no.sikt.nva.handle.HandleDatabase.SET_URI_SQL;
import static no.sikt.nva.handle.utils.DatabaseConnectionSupplier.ENV_DATABASE_PASSWORD;
import static no.sikt.nva.handle.utils.DatabaseConnectionSupplier.ENV_DATABASE_URI;
import static no.sikt.nva.handle.utils.DatabaseConnectionSupplier.ENV_DATABASE_USER;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.StringContains.containsString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Map;
import java.util.function.Supplier;
import no.sikt.nva.handle.model.HandleRequest;
import no.sikt.nva.handle.model.HandleResponse;
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

class UpdateHandleHandlerTest {

    public static final int UPDATE_HANDLE_ID = 2222;
    public static final String ENV_ALLOWED_ORIGIN = "ALLOWED_ORIGIN";
    public static final String ENV_API_HOST = "API_HOST";
    public static final String HANDLE_PREFIX = "11250.1";
    private FakeContext context;
    private UpdateHandleHandler handler;
    private Connection connection;
    private Environment environment;
    private ByteArrayOutputStream outputStream;

    @BeforeEach
    public void init() {
        this.environment = mock(Environment.class);
        when(environment.readEnv(ENV_HANDLE_BASE_URI)).thenReturn("https://hdl.handle.net");
        when(environment.readEnv(ENV_HANDLE_PREFIX)).thenReturn("11250.1");
        when(environment.readEnv(ENV_DATABASE_URI)).thenReturn("database_uri");
        when(environment.readEnv(ENV_DATABASE_USER)).thenReturn("database_user");
        when(environment.readEnv(ENV_DATABASE_PASSWORD)).thenReturn("database_password");
        when(environment.readEnv(ENV_API_HOST)).thenReturn("api.localhost.nva.aws.unit.no");
        when(environment.readEnv(ENV_HANDLE_PREFIX)).thenReturn(HANDLE_PREFIX);
        when(environment.readEnv(ENV_ALLOWED_ORIGIN)).thenReturn("*");

        this.context = new FakeContext();
        this.outputStream = new ByteArrayOutputStream();
        this.connection = mock(Connection.class);
        this.handler = new UpdateHandleHandler(environment, () -> connection);
    }

    @AfterEach
    public void tearDown() {

    }

    @Test
    void updateHandleRequestReturnsOkForValidUri()
        throws IOException, SQLException {
        var uri = randomUri();
        var inputStream = createUpdateHandleRequest(uri);

        PreparedStatement preparedStatementSetHandle = mock(PreparedStatement.class);
        when(preparedStatementSetHandle.executeUpdate()).thenReturn(1);
        when(connection.prepareStatement(SET_URI_SQL)).thenReturn(preparedStatementSetHandle);

        handler.handleRequest(inputStream, outputStream, context);
        var response = GatewayResponse.fromOutputStream(outputStream, HandleResponse.class);

        assertThat(response.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_OK)));
        assertThat(response.getBodyObject(HandleResponse.class).handle(),
                   is(equalTo(createHandleFromHandleId(UPDATE_HANDLE_ID))));
    }

    @Test
    void updateHandleRequestReturnsErrorIfNotFound()
        throws IOException, SQLException {
        var uri = randomUri();
        var inputStream = createUpdateHandleRequest(uri);

        PreparedStatement preparedStatementSetHandle = mock(PreparedStatement.class);
        when(preparedStatementSetHandle.toString()).thenReturn("some query");
        when(preparedStatementSetHandle.executeUpdate()).thenReturn(0);
        when(connection.prepareStatement(SET_URI_SQL)).thenReturn(preparedStatementSetHandle);

        handler.handleRequest(inputStream, outputStream, context);
        var response = GatewayResponse.fromOutputStream(outputStream, HandleResponse.class);

        assertThat(response.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_BAD_GATEWAY)));
    }

    @Test
    void createHandleRequestThrowsHandleExceptionAndLogsErrorWhenNotAbleToConnectToHandleDatabase()
        throws IOException {
        var appender = LogUtils.getTestingAppenderForRootLogger();
        @SuppressWarnings("unchecked") var connectionSupplier = (Supplier<Connection>) mock(Supplier.class);
        var uri = randomUri();
        var request = createUpdateHandleRequest(uri);
        var failure = "some connection failure";
        when(connectionSupplier.get()).thenThrow(new RuntimeException(new SQLException(failure)));
        var failingHandler = new UpdateHandleHandler(environment, connectionSupplier);

        failingHandler.handleRequest(request, outputStream, context);

        var response = GatewayResponse.fromOutputStream(outputStream, HandleResponse.class);
        assertThat(response.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_BAD_GATEWAY)));
        assertThat(appender.getMessages(), containsString(failure));
    }

    @Test
    void createHandleRequestReturnsBadRequestResponseWhenUriIsNull() throws IOException {
        var inputStream = createUpdateHandleRequest(null);
        handler.handleRequest(inputStream, outputStream, context);
        var response = GatewayResponse.fromOutputStream(outputStream, Problem.class);
        assertThat(response.getStatusCode(), is(equalTo(HttpURLConnection.HTTP_BAD_REQUEST)));
        assertThat(response.getBody(), containsString(NULL_URI_ERROR));
    }

    private InputStream createUpdateHandleRequest(URI uri) throws JsonProcessingException {
        HandleRequest request = new HandleRequest(uri);
        return new HandlerRequestBuilder<HandleRequest>(JsonUtils.dtoObjectMapper)
                   .withBody(request)
                   .withPathParameters(Map.of("prefix", HANDLE_PREFIX, "suffix",
                                              String.valueOf(UPDATE_HANDLE_ID)))
                   .build();
    }

    private URI createHandleFromHandleId(int handleId) {
        return UriWrapper.fromHost(environment.readEnv(ENV_HANDLE_BASE_URI))
                   .addChild(environment.readEnv(ENV_HANDLE_PREFIX), Integer.toString(handleId)).getUri();
    }
}