package no.sikt.nva.approvals.rest;

import static java.net.HttpURLConnection.HTTP_BAD_REQUEST;
import static java.net.HttpURLConnection.HTTP_NOT_FOUND;
import static java.net.HttpURLConnection.HTTP_OK;
import static nva.commons.apigateway.ApiGatewayHandler.ALLOWED_ORIGIN_ENV;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import static no.sikt.nva.approvals.utils.TestUtils.randomApproval;
import static no.sikt.nva.approvals.utils.TestUtils.randomHandle;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import no.sikt.nva.approvals.domain.Approval;
import no.sikt.nva.approvals.domain.FakeApprovalService;
import no.sikt.nva.approvals.domain.Handle;
import no.sikt.nva.approvals.domain.NamedIdentifier;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.stubs.FakeContext;
import no.unit.nva.testutils.HandlerRequestBuilder;
import nva.commons.apigateway.GatewayResponse;
import nva.commons.core.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class FetchApprovalHandlerTest {

    private static final Context CONTEXT = new FakeContext();
    private static final String APPROVAL_ID_PATH_PARAMETER = "approvalId";
    private static final String HANDLE_QUERY_PARAMETER = "handle";
    private static final String NAME_QUERY_PARAMETER = "name";
    private static final String VALUE_QUERY_PARAMETER = "value";
    private static final String VALID_HANDLE = "https://hdl.handle.net/11250.1/12345";
    private static final String API_HOST = "api.unittest.nva.unit.no";
    private static final String COGNITO_AUTHORIZER_URLS_ENV = "COGNITO_AUTHORIZER_URLS";
    private static final String API_HOST_ENV = "API_HOST";
    private FetchApprovalHandler handler;
    private ByteArrayOutputStream output;
    private Environment environment;

    @BeforeEach
    void setUp() {
        output = new ByteArrayOutputStream();
        environment = mock(Environment.class);
        lenient().when(environment.readEnv(ALLOWED_ORIGIN_ENV)).thenReturn("*");
        lenient().when(environment.readEnv(COGNITO_AUTHORIZER_URLS_ENV)).thenReturn("http://localhost:3000");
        lenient().when(environment.readEnv(API_HOST_ENV)).thenReturn(API_HOST);
    }

    @Test
    void shouldReturnOkResponseWithApprovalOnSuccess() {
        var approvalId = UUID.randomUUID();
        var approval = new Approval(approvalId, List.of(new NamedIdentifier("test", "value")), randomUri(), randomHandle());
        handler = new FetchApprovalHandler(new FakeApprovalService(List.of(approval)), environment);
        var request = createRequestWithPathParameter(approvalId);

        var response = handleRequest(request);

        assertEquals(HTTP_OK, response.getStatusCode());
    }

    @Test
    void shouldReturnNotFoundWhenApprovalDoesNotExist() {
        var approvalId = UUID.randomUUID();
        handler = new FetchApprovalHandler(new FakeApprovalService(), environment);
        var request = createRequestWithPathParameter(approvalId);

        var response = handleRequest(request);

        assertEquals(HTTP_NOT_FOUND, response.getStatusCode());
    }

    @Test
    void shouldReturnBadRequestWhenApprovalIdIsInvalid() {
        handler = new FetchApprovalHandler(new FakeApprovalService(), environment);
        var request = createRequestWithInvalidId();

        var response = handleRequest(request);

        assertEquals(HTTP_BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void shouldReturnOkWhenLookingUpByHandle() {
        var handle = new Handle(java.net.URI.create(VALID_HANDLE));
        var approval = randomApproval(handle);
        handler = new FetchApprovalHandler(new FakeApprovalService(List.of(approval)), environment);
        var request = createRequestWithHandleQuery(VALID_HANDLE);

        var response = handleRequest(request);

        assertEquals(HTTP_OK, response.getStatusCode());
    }

    @Test
    void shouldReturnNotFoundWhenHandleLookupFindsNothing() {
        handler = new FetchApprovalHandler(new FakeApprovalService(), environment);
        var request = createRequestWithHandleQuery(VALID_HANDLE);

        var response = handleRequest(request);

        assertEquals(HTTP_NOT_FOUND, response.getStatusCode());
    }

    @Test
    void shouldReturnBadRequestWhenHandleIsInvalid() {
        handler = new FetchApprovalHandler(new FakeApprovalService(), environment);
        var request = createRequestWithHandleQuery("not-a-valid-handle");

        var response = handleRequest(request);

        assertEquals(HTTP_BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void shouldReturnOkWhenLookingUpByNamedIdentifier() {
        var namedIdentifier = new NamedIdentifier("doi", "10.1234/5678");
        var approval = randomApproval(namedIdentifier);
        handler = new FetchApprovalHandler(new FakeApprovalService(List.of(approval)), environment);
        var request = createRequestWithNamedIdentifierQuery("doi", "10.1234/5678");

        var response = handleRequest(request);

        assertEquals(HTTP_OK, response.getStatusCode());
    }

    @Test
    void shouldReturnNotFoundWhenNamedIdentifierLookupFindsNothing() {
        handler = new FetchApprovalHandler(new FakeApprovalService(), environment);
        var request = createRequestWithNamedIdentifierQuery("doi", "10.1234/5678");

        var response = handleRequest(request);

        assertEquals(HTTP_NOT_FOUND, response.getStatusCode());
    }

    @Test
    void shouldReturnBadRequestWhenOnlyNameIsProvided() {
        handler = new FetchApprovalHandler(new FakeApprovalService(), environment);
        var request = createRequestWithQueryParameters(Map.of(NAME_QUERY_PARAMETER, "doi"));

        var response = handleRequest(request);

        assertEquals(HTTP_BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void shouldReturnBadRequestWhenOnlyValueIsProvided() {
        handler = new FetchApprovalHandler(new FakeApprovalService(), environment);
        var request = createRequestWithQueryParameters(Map.of(VALUE_QUERY_PARAMETER, "10.1234/5678"));

        var response = handleRequest(request);

        assertEquals(HTTP_BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void shouldReturnBadRequestWhenNoQueryParametersProvided() {
        handler = new FetchApprovalHandler(new FakeApprovalService(), environment);
        var request = createRequestWithQueryParameters(Map.of());

        var response = handleRequest(request);

        assertEquals(HTTP_BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void shouldReturnBadRequestWhenBothPathParameterAndQueryParametersProvided() {
        handler = new FetchApprovalHandler(new FakeApprovalService(), environment);
        var request = createRequestWithPathAndQueryParameters(UUID.randomUUID(), VALID_HANDLE);

        var response = handleRequest(request);

        assertEquals(HTTP_BAD_REQUEST, response.getStatusCode());
    }

    private GatewayResponse<ApprovalResponse> handleRequest(InputStream request) {
        try {
            handler.handleRequest(request, output, CONTEXT);
            return GatewayResponse.fromOutputStream(output, ApprovalResponse.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private InputStream createRequestWithPathParameter(UUID approvalId) {
        try {
            return new HandlerRequestBuilder<Void>(JsonUtils.dtoObjectMapper)
                .withPathParameters(Map.of(APPROVAL_ID_PATH_PARAMETER, approvalId.toString()))
                                .build();
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    private InputStream createRequestWithInvalidId() {
        try {
            return new HandlerRequestBuilder<Void>(JsonUtils.dtoObjectMapper)
                .withPathParameters(Map.of(APPROVAL_ID_PATH_PARAMETER, "not-a-uuid"))
                                .build();
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    private InputStream createRequestWithHandleQuery(String handle) {
        return createRequestWithQueryParameters(Map.of(HANDLE_QUERY_PARAMETER, handle));
    }

    private InputStream createRequestWithNamedIdentifierQuery(String name, String value) {
        return createRequestWithQueryParameters(Map.of(NAME_QUERY_PARAMETER, name, VALUE_QUERY_PARAMETER, value));
    }

    private InputStream createRequestWithQueryParameters(Map<String, String> queryParameters) {
        try {
            return new HandlerRequestBuilder<Void>(JsonUtils.dtoObjectMapper)
                .withQueryParameters(queryParameters)
                                .build();
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    private InputStream createRequestWithPathAndQueryParameters(UUID approvalId, String handle) {
        try {
            return new HandlerRequestBuilder<Void>(JsonUtils.dtoObjectMapper)
                .withPathParameters(Map.of(APPROVAL_ID_PATH_PARAMETER, approvalId.toString()))
                .withQueryParameters(Map.of(HANDLE_QUERY_PARAMETER, handle))
                                .build();
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

}
