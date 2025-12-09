package no.sikt.nva.approvals;

import static java.net.HttpURLConnection.HTTP_ACCEPTED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
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
        handler = new CreateApprovalHandler();
        this.output = new ByteArrayOutputStream();
    }

    @Test
    void shouldReturnAcceptedResponse() throws IOException {
        var request = createRequest();

        handler.handleRequest(request, output, context);

        var response = GatewayResponse.fromOutputStream(output, Void.class);

        assertEquals(HTTP_ACCEPTED, response.getStatusCode());
    }

    private InputStream createRequest() throws JsonProcessingException {
        return new HandlerRequestBuilder<Void>(JsonUtils.dtoObjectMapper).build();
    }
}
