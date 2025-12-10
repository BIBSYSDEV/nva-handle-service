package no.sikt.nva.approvals.rest;

import static java.net.HttpURLConnection.HTTP_BAD_GATEWAY;
import static java.net.HttpURLConnection.HTTP_BAD_REQUEST;
import static java.net.HttpURLConnection.HTTP_NOT_FOUND;
import static java.net.HttpURLConnection.HTTP_OK;
import static no.sikt.nva.approvals.rest.GetApprovalHandler.APPROVAL_ID_PATH_PARAMETER;
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

class GetApprovalHandlerTest {

    private static final Context CONTEXT = new FakeContext();
    private GetApprovalHandler handler;
    private ByteArrayOutputStream output;

    @BeforeEach
    void setUp() {
        output = new ByteArrayOutputStream();
    }

    @Test
    void shouldReturnOkResponseWithApprovalOnSuccess() throws IOException {
        var approvalId = UUID.randomUUID();
        handler = new GetApprovalHandler(new FakeApprovalService());
        var request = createRequest(approvalId);

        handler.handleRequest(request, output, CONTEXT);

        var response = GatewayResponse.fromOutputStream(output, Approval.class);

        assertEquals(HTTP_OK, response.getStatusCode());
    }

    @Test
    void shouldReturnNotFoundWhenApprovalDoesNotExist() throws IOException {
        var approvalId = UUID.randomUUID();
        handler = new GetApprovalHandler(new FakeApprovalService(new ApprovalNotFoundException("not found")));
        var request = createRequest(approvalId);

        handler.handleRequest(request, output, CONTEXT);

        var response = GatewayResponse.fromOutputStream(output, Void.class);

        assertEquals(HTTP_NOT_FOUND, response.getStatusCode());
    }

    @Test
    void shouldReturnBadRequestWhenApprovalIdIsInvalid() throws IOException {
        handler = new GetApprovalHandler(new FakeApprovalService());
        var request = createRequestWithInvalidId("not-a-uuid");

        handler.handleRequest(request, output, CONTEXT);

        var response = GatewayResponse.fromOutputStream(output, Void.class);

        assertEquals(HTTP_BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void shouldReturnBadGatewayWhenServiceThrowsException() throws IOException {
        var approvalId = UUID.randomUUID();
        handler = new GetApprovalHandler(new FakeApprovalService(new ApprovalServiceException("error")));
        var request = createRequest(approvalId);

        handler.handleRequest(request, output, CONTEXT);

        var response = GatewayResponse.fromOutputStream(output, Void.class);

        assertEquals(HTTP_BAD_GATEWAY, response.getStatusCode());
    }

    private InputStream createRequest(UUID approvalId) throws JsonProcessingException {
        return new HandlerRequestBuilder<Void>(JsonUtils.dtoObjectMapper)
            .withPathParameters(Map.of(APPROVAL_ID_PATH_PARAMETER, approvalId.toString()))
            .build();
    }

    private InputStream createRequestWithInvalidId(String invalidId) throws JsonProcessingException {
        return new HandlerRequestBuilder<Void>(JsonUtils.dtoObjectMapper)
            .withPathParameters(Map.of(APPROVAL_ID_PATH_PARAMETER, invalidId))
            .build();
    }
}
