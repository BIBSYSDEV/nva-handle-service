package no.sikt.nva.approvals.rest;

import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.core.JsonProcessingException;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.stubs.FakeContext;
import no.unit.nva.testutils.HandlerRequestBuilder;
import nva.commons.apigateway.GatewayResponse;
import nva.commons.core.ioutils.IoUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.nio.file.Path;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.StringContains.containsString;

class FetchOntologyHandlerTest {

    private static final String EXPECTED_ONTOLOGY = IoUtils.stringFromResources(Path.of("approval-ontology.ttl"));
    private static final Context CONTEXT = new FakeContext();
    private FetchOntologyHandler handler;
    private ByteArrayOutputStream outputStream;

    @BeforeEach
    void setUp() {
        handler = new FetchOntologyHandler();
        outputStream = new ByteArrayOutputStream();
    }

    @Test
    void shouldReturnOntologyTurtle() throws IOException {
        var inputStream = createRequest();

        handler.handleRequest(inputStream, outputStream, CONTEXT);

        var response = GatewayResponse.fromOutputStream(outputStream, String.class);
        assertThat(response.getStatusCode(), is(HttpURLConnection.HTTP_OK));
        assertThat(response.getBody(), containsString("@prefix"));
        assertThat(response.getBody(), containsString("owl:Ontology"));
        assertThat(response.getBody(), containsString("https://nva.unit.no/approval#"));
    }

    @Test
    void shouldReturnExpectedOntologyContent() throws IOException {
        var inputStream = createRequest();

        handler.handleRequest(inputStream, outputStream, CONTEXT);

        var response = GatewayResponse.fromOutputStream(outputStream, String.class);
        assertThat(response.getBody(), is(EXPECTED_ONTOLOGY));
    }

    @Test
    void shouldContainApprovalClass() throws IOException {
        var inputStream = createRequest();

        handler.handleRequest(inputStream, outputStream, CONTEXT);

        var response = GatewayResponse.fromOutputStream(outputStream, String.class);
        assertThat(response.getBody(), containsString(":Approval rdf:type owl:Class"));
    }

    @Test
    void shouldContainIdentifierClass() throws IOException {
        var inputStream = createRequest();

        handler.handleRequest(inputStream, outputStream, CONTEXT);

        var response = GatewayResponse.fromOutputStream(outputStream, String.class);
        assertThat(response.getBody(), containsString(":Identifier rdf:type owl:Class"));
    }

    private InputStream createRequest() throws JsonProcessingException {
        return new HandlerRequestBuilder<Void>(JsonUtils.dtoObjectMapper).build();
    }
}
