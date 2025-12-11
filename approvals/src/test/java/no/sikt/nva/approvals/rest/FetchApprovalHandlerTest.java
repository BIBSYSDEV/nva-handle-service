package no.sikt.nva.approvals.rest;

import static java.net.HttpURLConnection.HTTP_BAD_GATEWAY;
import static java.net.HttpURLConnection.HTTP_BAD_REQUEST;
import static java.net.HttpURLConnection.HTTP_NOT_FOUND;
import static java.net.HttpURLConnection.HTTP_OK;
import static org.junit.jupiter.api.Assertions.assertEquals;
import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.UUID;
import no.sikt.nva.approvals.domain.ApprovalNotFoundException;
import no.sikt.nva.approvals.domain.ApprovalServiceException;
import no.sikt.nva.approvals.domain.FakeApprovalService;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.stubs.FakeContext;
import no.unit.nva.testutils.HandlerRequestBuilder;
import nva.commons.apigateway.GatewayResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class FetchApprovalHandlerTest {

    private static final Context CONTEXT = new FakeContext();
    private static final String APPROVAL_ID_PATH_PARAMETER = "approvalId";
    private static final String HANDLE_QUERY_PARAMETER = "handle";
    private static final String NAME_QUERY_PARAMETER = "name";
    private static final String VALUE_QUERY_PARAMETER = "value";
    private static final String VALID_HANDLE = "https://hdl.handle.net/11250.1/12345";
    private FetchApprovalHandler handler;
    private ByteArrayOutputStream output;

    @BeforeEach
    void setUp() {
        output = new ByteArrayOutputStream();
    }

    @Test
    void shouldReturnOkResponseWithApprovalOnSuccess() {
        var approvalId = UUID.randomUUID();
        handler = new FetchApprovalHandler(new FakeApprovalService());
        var request = createRequestWithPathParameter(approvalId);

        var response = handleRequest(request);

        assertEquals(HTTP_OK, response.getStatusCode());
    }

    @Test
    void shouldReturnNotFoundWhenApprovalDoesNotExist() {
        var approvalId = UUID.randomUUID();
        handler = new FetchApprovalHandler(new FakeApprovalService(new ApprovalNotFoundException("not found")));
        var request = createRequestWithPathParameter(approvalId);

        var response = handleRequest(request);

        assertEquals(HTTP_NOT_FOUND, response.getStatusCode());
    }

    @Test
    void shouldReturnBadRequestWhenApprovalIdIsInvalid() {
        handler = new FetchApprovalHandler(new FakeApprovalService());
        var request = createRequestWithInvalidId();

        var response = handleRequest(request);

        assertEquals(HTTP_BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void shouldReturnBadGatewayWhenServiceThrowsException() {
        var approvalId = UUID.randomUUID();
        handler = new FetchApprovalHandler(new FakeApprovalService(new ApprovalServiceException("error")));
        var request = createRequestWithPathParameter(approvalId);

        var response = handleRequest(request);

        assertEquals(HTTP_BAD_GATEWAY, response.getStatusCode());
    }

    @Test
    void shouldReturnOkWhenLookingUpByHandle() {
        handler = new FetchApprovalHandler(new FakeApprovalService());
        var request = createRequestWithHandleQuery(VALID_HANDLE);

        var response = handleRequest(request);

        assertEquals(HTTP_OK, response.getStatusCode());
    }

    @Test
    void shouldReturnNotFoundWhenHandleLookupFindsNothing() {
        handler = new FetchApprovalHandler(new FakeApprovalService(new ApprovalNotFoundException("not found")));
        var request = createRequestWithHandleQuery(VALID_HANDLE);

        var response = handleRequest(request);

        assertEquals(HTTP_NOT_FOUND, response.getStatusCode());
    }

    @Test
    void shouldReturnBadRequestWhenHandleIsInvalid() {
        handler = new FetchApprovalHandler(new FakeApprovalService());
        var request = createRequestWithHandleQuery("not-a-valid-handle");

        var response = handleRequest(request);

        assertEquals(HTTP_BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void shouldReturnOkWhenLookingUpByNamedIdentifier() {
        handler = new FetchApprovalHandler(new FakeApprovalService());
        var request = createRequestWithNamedIdentifierQuery("doi", "10.1234/5678");

        var response = handleRequest(request);

        assertEquals(HTTP_OK, response.getStatusCode());
    }

    @Test
    void shouldReturnNotFoundWhenNamedIdentifierLookupFindsNothing() {
        handler = new FetchApprovalHandler(new FakeApprovalService(new ApprovalNotFoundException("not found")));
        var request = createRequestWithNamedIdentifierQuery("doi", "10.1234/5678");

        var response = handleRequest(request);

        assertEquals(HTTP_NOT_FOUND, response.getStatusCode());
    }

    @Test
    void shouldReturnBadRequestWhenOnlyNameIsProvided() {
        handler = new FetchApprovalHandler(new FakeApprovalService());
        var request = createRequestWithQueryParameters(Map.of(NAME_QUERY_PARAMETER, "doi"));

        var response = handleRequest(request);

        assertEquals(HTTP_BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void shouldReturnBadRequestWhenOnlyValueIsProvided() {
        handler = new FetchApprovalHandler(new FakeApprovalService());
        var request = createRequestWithQueryParameters(Map.of(VALUE_QUERY_PARAMETER, "10.1234/5678"));

        var response = handleRequest(request);

        assertEquals(HTTP_BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void shouldReturnBadRequestWhenNoQueryParametersProvided() {
        handler = new FetchApprovalHandler(new FakeApprovalService());
        var request = createRequestWithQueryParameters(Map.of());

        var response = handleRequest(request);

        assertEquals(HTTP_BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void shouldReturnBadGatewayWhenServiceThrowsExceptionOnHandleLookup() {
        handler = new FetchApprovalHandler(new FakeApprovalService(new ApprovalServiceException("error")));
        var request = createRequestWithHandleQuery(VALID_HANDLE);

        var response = handleRequest(request);

        assertEquals(HTTP_BAD_GATEWAY, response.getStatusCode());
    }

    @Test
    void shouldReturnBadGatewayWhenServiceThrowsExceptionOnNamedIdentifierLookup() {
        handler = new FetchApprovalHandler(new FakeApprovalService(new ApprovalServiceException("error")));
        var request = createRequestWithNamedIdentifierQuery("doi", "10.1234/5678");

        var response = handleRequest(request);

        assertEquals(HTTP_BAD_GATEWAY, response.getStatusCode());
    }

    @Test
    void shouldReturnBadRequestWhenBothPathParameterAndQueryParametersProvided() {
        handler = new FetchApprovalHandler(new FakeApprovalService());
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
