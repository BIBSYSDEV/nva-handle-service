package no.sikt.nva.approvals.rest;

import static java.net.HttpURLConnection.HTTP_ACCEPTED;
import static java.net.HttpURLConnection.HTTP_BAD_GATEWAY;
import static java.net.HttpURLConnection.HTTP_BAD_REQUEST;
import static java.net.HttpURLConnection.HTTP_NOT_FOUND;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static org.junit.jupiter.api.Assertions.assertEquals;
import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import no.sikt.nva.approvals.domain.ApprovalNotFoundException;
import no.sikt.nva.approvals.domain.ApprovalServiceException;
import no.sikt.nva.approvals.domain.FakeApprovalService;
import no.sikt.nva.approvals.domain.NamedIdentifier;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.stubs.FakeContext;
import no.unit.nva.testutils.HandlerRequestBuilder;
import nva.commons.apigateway.GatewayResponse;
import nva.commons.core.Environment;
import nva.commons.core.paths.UriWrapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class UpdateApprovalHandlerTest {

    private static final Context context = new FakeContext();
    private static final String APPROVAL_ID_PATH_PARAMETER = "approvalId";
    private UpdateApprovalHandler handler;
    private ByteArrayOutputStream output;
    private UUID approvalId;

    @BeforeEach
    void setUp() {
        this.output = new ByteArrayOutputStream();
        this.approvalId = UUID.randomUUID();
        FakeApprovalService approvalService = new FakeApprovalService();
        handler = new UpdateApprovalHandler(approvalService);
    }

    @Test
    void shouldReturnAcceptedResponseOnSuccess() throws IOException {
        var request = createRequest(randomUpdateApprovalRequest(), approvalId);

        handler.handleRequest(request, output, context);

        var response = GatewayResponse.fromOutputStream(output, Void.class);

        assertEquals(HTTP_ACCEPTED, response.getStatusCode());
    }

    @Test
    void shouldSetLocationHeaderOnSuccess() throws IOException {
        var request = createRequest(randomUpdateApprovalRequest(), approvalId);

        handler.handleRequest(request, output, context);

        var response = GatewayResponse.fromOutputStream(output, Void.class);

        var locationHeader = response.getHeaders().get("Location");
        var expectedLocation = createExpectedLocationHeader(approvalId);

        assertEquals(expectedLocation, locationHeader);
    }

    @Test
    void shouldSetRetryAfterHeaderOnSuccessWith5SecondsValue() throws IOException {
        var request = createRequest(randomUpdateApprovalRequest(), approvalId);

        handler.handleRequest(request, output, context);

        var response = GatewayResponse.fromOutputStream(output, Void.class);

        var retryAfterHeader = response.getHeaders().get("Retry-After");

        assertEquals("5", retryAfterHeader);
    }

    @Test
    void shouldReturnBadRequestWhenRequestBodyIsInvalidDueToMissingIdentifiers() throws IOException {
        var invalidJson = invalidRequestBody(randomUri());
        var request = createRawJsonRequest(invalidJson, approvalId);

        handler.handleRequest(request, output, context);

        var response = GatewayResponse.fromOutputStream(output, Void.class);

        assertEquals(HTTP_BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void shouldReturnBadRequestWhenRequestBodyIsInvalidDueToMissingSource() throws IOException {
        var invalidJson = invalidRequestBody(null);
        var request = createRawJsonRequest(invalidJson, approvalId);

        handler.handleRequest(request, output, context);

        var response = GatewayResponse.fromOutputStream(output, Void.class);

        assertEquals(HTTP_BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void shouldReturnBadRequestWhenRequestBodyIsInvalidDueToMissingHandle() throws IOException {
        var invalidJson = invalidRequestBody(randomUri());
        var request = createRawJsonRequest(invalidJson, approvalId);

        handler.handleRequest(request, output, context);

        var response = GatewayResponse.fromOutputStream(output, Void.class);

        assertEquals(HTTP_BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void shouldReturnBadRequestWhenApprovalIdIsInvalid() throws IOException {
        var request = createRequestWithInvalidApprovalId(randomUpdateApprovalRequest());

        handler.handleRequest(request, output, context);

        var response = GatewayResponse.fromOutputStream(output, Void.class);

        assertEquals(HTTP_BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void shouldReturnNotFoundWhenApprovalServiceThrowsNotFoundException() throws IOException {
        handler = new UpdateApprovalHandler(new FakeApprovalService(new ApprovalNotFoundException("not found")));
        var request = createRequest(randomUpdateApprovalRequest(), approvalId);

        handler.handleRequest(request, output, context);

        var response = GatewayResponse.fromOutputStream(output, Void.class);

        assertEquals(HTTP_NOT_FOUND, response.getStatusCode());
    }

    @Test
    void shouldReturnBadGatewayWhenApprovalServiceThrowsApprovalServiceException() throws IOException {
        handler = new UpdateApprovalHandler(new FakeApprovalService(new ApprovalServiceException("error")));
        var request = createRequest(randomUpdateApprovalRequest(), approvalId);

        handler.handleRequest(request, output, context);

        var response = GatewayResponse.fromOutputStream(output, Void.class);

        assertEquals(HTTP_BAD_GATEWAY, response.getStatusCode());
    }

    private static String invalidRequestBody(URI source) {
        return """
            {
              "identifiers": [
                {
                  "name": "test",
                  "value": "123"
                }
              ]
            }
            """.formatted(source);
    }

    private static UpdateApprovalRequest randomUpdateApprovalRequest() {
        return new UpdateApprovalRequest(List.of(new NamedIdentifier(randomString(), randomString())));
    }

    private String createExpectedLocationHeader(UUID identifier) {
        return UriWrapper.fromHost(new Environment().readEnv("API_HOST"))
                   .addChild("approval")
                   .addChild(identifier.toString())
                   .toString();
    }

    private InputStream createRequest(UpdateApprovalRequest request, UUID approvalId) throws JsonProcessingException {
        return new HandlerRequestBuilder<UpdateApprovalRequest>(JsonUtils.dtoObjectMapper).withBody(request)
                   .withPathParameters(Map.of(APPROVAL_ID_PATH_PARAMETER, approvalId.toString()))
                   .build();
    }

    private InputStream createRequestWithInvalidApprovalId(UpdateApprovalRequest request)
        throws JsonProcessingException {
        return new HandlerRequestBuilder<UpdateApprovalRequest>(JsonUtils.dtoObjectMapper).withBody(request)
                   .withPathParameters(Map.of(APPROVAL_ID_PATH_PARAMETER, "invalid-uuid"))
                   .build();
    }

    private InputStream createRawJsonRequest(String jsonBody, UUID approvalId) throws JsonProcessingException {
        return new HandlerRequestBuilder<String>(JsonUtils.dtoObjectMapper).withBody(jsonBody)
                   .withPathParameters(Map.of(APPROVAL_ID_PATH_PARAMETER, approvalId.toString()))
                   .build();
    }
}
