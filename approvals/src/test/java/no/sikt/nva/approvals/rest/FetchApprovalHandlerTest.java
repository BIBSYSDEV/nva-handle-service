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
import no.sikt.nva.approvals.domain.Approval;
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
        var request = createRequest(approvalId);

        var response = handleRequest(request);

        assertEquals(HTTP_OK, response.getStatusCode());
    }

    @Test
    void shouldReturnNotFoundWhenApprovalDoesNotExist() {
        var approvalId = UUID.randomUUID();
        handler = new FetchApprovalHandler(new FakeApprovalService(new ApprovalNotFoundException("not found")));
        var request = createRequest(approvalId);

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
        var request = createRequest(approvalId);

        var response = handleRequest(request);

        assertEquals(HTTP_BAD_GATEWAY, response.getStatusCode());
    }

    private GatewayResponse<Approval> handleRequest(InputStream request) {
        try {
            handler.handleRequest(request, output, CONTEXT);
            return GatewayResponse.fromOutputStream(output, Approval.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private InputStream createRequest(UUID approvalId) {
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
}
