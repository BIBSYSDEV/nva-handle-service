package no.sikt.nva.approvals.rest;

import static java.net.HttpURLConnection.HTTP_ACCEPTED;
import static java.net.HttpURLConnection.HTTP_BAD_GATEWAY;
import static java.net.HttpURLConnection.HTTP_BAD_REQUEST;
import static java.net.HttpURLConnection.HTTP_CONFLICT;
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
import no.sikt.nva.approvals.domain.Approval;
import no.sikt.nva.approvals.domain.ApprovalConflictException;
import no.sikt.nva.approvals.domain.ApprovalServiceException;
import no.sikt.nva.approvals.domain.FakeApprovalService;
import no.sikt.nva.approvals.domain.NamedIdentifier;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.stubs.FakeContext;
import no.unit.nva.testutils.HandlerRequestBuilder;
import nva.commons.apigateway.GatewayResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CreateApprovalHandlerTest {

    private static final Context context = new FakeContext();
    private CreateApprovalHandler handler;
    private ByteArrayOutputStream output;

    @BeforeEach
    void setUp() {
        this.output = new ByteArrayOutputStream();
        handler = new CreateApprovalHandler(new FakeApprovalService());
    }

    @Test
    void shouldReturnAcceptedResponseOnSuccess() throws IOException {
        var request = createRequest(randomApprovalRequest(randomUri()));

        handler.handleRequest(request, output, context);

        var response = GatewayResponse.fromOutputStream(output, Void.class);

        assertEquals(HTTP_ACCEPTED, response.getStatusCode());
    }

    @Test
    void shouldReturnBadRequestWhenRequestBodyIsInvalid() throws IOException {
        var request = createRequest(randomApprovalRequest(null));

        handler.handleRequest(request, output, context);

        var response = GatewayResponse.fromOutputStream(output, Void.class);

        assertEquals(HTTP_BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void shouldReturnConflictWhenApprovalServiceThrowsConflictException() throws IOException {
        handler = new CreateApprovalHandler(new FakeApprovalService(new ApprovalConflictException("conflict")));
        var request = createRequest(randomApprovalRequest(randomUri()));

        handler.handleRequest(request, output, context);

        var response = GatewayResponse.fromOutputStream(output, Void.class);

        assertEquals(HTTP_CONFLICT, response.getStatusCode());
    }

    @Test
    void shouldReturnBadGatewayOnWhenApprovalServiceThrowsApprovalServiceException() throws IOException {
        handler = new CreateApprovalHandler(new FakeApprovalService(new ApprovalServiceException("error")));
        var request = createRequest(randomApprovalRequest(randomUri()));

        handler.handleRequest(request, output, context);

        var response = GatewayResponse.fromOutputStream(output, Void.class);

        assertEquals(HTTP_BAD_GATEWAY, response.getStatusCode());
    }

    private static CreateApprovalRequest randomApprovalRequest(URI source) {
        return new CreateApprovalRequest(List.of(new NamedIdentifier(randomString(), randomString())), source);
    }

    private InputStream createRequest(CreateApprovalRequest request) throws JsonProcessingException {
        return new HandlerRequestBuilder<CreateApprovalRequest>(JsonUtils.dtoObjectMapper).withBody(request).build();
    }
}
