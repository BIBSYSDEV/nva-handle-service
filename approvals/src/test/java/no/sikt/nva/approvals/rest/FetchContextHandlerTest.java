package no.sikt.nva.approvals.rest;

import com.amazonaws.services.lambda.runtime.Context;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.stubs.FakeContext;
import no.unit.nva.testutils.HandlerRequestBuilder;
import nva.commons.apigateway.GatewayResponse;
import nva.commons.core.ioutils.IoUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.nio.file.Path;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.StringContains.containsString;

class FetchContextHandlerTest {

    private static final String EXPECTED_CONTEXT = IoUtils.stringFromResources(Path.of("approval-context.json"));
    private static final Context CONTEXT = new FakeContext();
    private FetchContextHandler handler;
    private ByteArrayOutputStream outputStream;

    @BeforeEach
    void setUp() {
        handler = new FetchContextHandler();
        outputStream = new ByteArrayOutputStream();
    }

    @Test
    void shouldReturnContextJson() throws IOException {
        InputStream inputStream = createRequest();

        handler.handleRequest(inputStream, outputStream, CONTEXT);

        GatewayResponse<String> response = GatewayResponse.fromOutputStream(outputStream, String.class);
        assertThat(response.getStatusCode(), is(HttpURLConnection.HTTP_OK));
        assertThat(response.getBody(), containsString("@context"));
        assertThat(response.getBody(), containsString("@vocab"));
        assertThat(response.getBody(), containsString("https://nva.unit.no/approval#"));
    }

    @Test
    void shouldReturnExpectedContextContent() throws IOException {
        InputStream inputStream = createRequest();

        handler.handleRequest(inputStream, outputStream, CONTEXT);

        GatewayResponse<String> response = GatewayResponse.fromOutputStream(outputStream, String.class);
        assertThat(response.getBody(), is(EXPECTED_CONTEXT));
    }

    private InputStream createRequest() throws JsonProcessingException {
        return new HandlerRequestBuilder<Void>(JsonUtils.dtoObjectMapper).build();
    }
}
