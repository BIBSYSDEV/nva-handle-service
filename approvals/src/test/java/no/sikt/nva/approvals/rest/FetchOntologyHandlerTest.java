package no.sikt.nva.approvals.rest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.StringContains.containsString;

import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.nio.file.Path;
import java.util.Map;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.stubs.FakeContext;
import no.unit.nva.testutils.HandlerRequestBuilder;
import nva.commons.apigateway.GatewayResponse;
import nva.commons.core.ioutils.IoUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class FetchOntologyHandlerTest {

  private static final String ONTOLOGY_IRI = "https://localhost/approval/ontology";
  private static final String ONTOLOGY_IRI_PLACEHOLDER = "__ONTOLOGY_IRI__";
  private static final String LEGACY_NAMESPACE = "https://nva.unit.no/approval#";
  private static final String VERSION_PATH_PARAMETER = "version";
  private static final String ONTOLOGY_VERSION = "1.0.0";
  private static final String UNKNOWN_VERSION = "0.0.1";
  private static final String CONTENT_LOCATION_HEADER = "Content-Location";
  private static final String VERSIONED_ONTOLOGY_URI = "https://localhost/approval/ontology/1.0.0";
  private static final String EXPECTED_ONTOLOGY =
      IoUtils.stringFromResources(Path.of("ontology/approval-ontology-1.0.0.ttl"))
          .replace(ONTOLOGY_IRI_PLACEHOLDER, ONTOLOGY_IRI);
  private static final Context CONTEXT = new FakeContext();
  private FetchOntologyHandler handler;
  private ByteArrayOutputStream outputStream;

  @BeforeEach
  void setUp() {
    handler = new FetchOntologyHandler();
    outputStream = new ByteArrayOutputStream();
  }

  @Test
  void shouldReturnLatestOntologyWhenVersionIsNotGiven() throws IOException {
    handler.handleRequest(createRequest(), outputStream, CONTEXT);

    var response = GatewayResponse.fromOutputStream(outputStream, String.class);
    assertThat(response.getStatusCode(), is(HttpURLConnection.HTTP_OK));
    assertThat(response.getBody(), is(EXPECTED_ONTOLOGY));
  }

  @Test
  void shouldReturnRequestedOntologyVersion() throws IOException {
    handler.handleRequest(createRequest(ONTOLOGY_VERSION), outputStream, CONTEXT);

    var response = GatewayResponse.fromOutputStream(outputStream, String.class);
    assertThat(response.getStatusCode(), is(HttpURLConnection.HTTP_OK));
    assertThat(response.getBody(), is(EXPECTED_ONTOLOGY));
  }

  @Test
  void shouldPointToVersionedOntologyInContentLocationWhenVersionIsNotGiven() throws IOException {
    handler.handleRequest(createRequest(), outputStream, CONTEXT);

    var response = GatewayResponse.fromOutputStream(outputStream, String.class);
    assertThat(response.getHeaders().get(CONTENT_LOCATION_HEADER), is(VERSIONED_ONTOLOGY_URI));
  }

  @Test
  void shouldReturnNotFoundForUnknownVersion() throws IOException {
    handler.handleRequest(createRequest(UNKNOWN_VERSION), outputStream, CONTEXT);

    var response = GatewayResponse.fromOutputStream(outputStream, String.class);
    assertThat(response.getStatusCode(), is(HttpURLConnection.HTTP_NOT_FOUND));
  }

  @Test
  void shouldDeclareOntologyWithVersionIri() throws IOException {
    handler.handleRequest(createRequest(), outputStream, CONTEXT);

    var response = GatewayResponse.fromOutputStream(outputStream, String.class);
    assertThat(response.getBody(), containsString("<%s> a owl:Ontology".formatted(ONTOLOGY_IRI)));
    assertThat(
        response.getBody(),
        containsString("owl:versionIRI <%s>".formatted(VERSIONED_ONTOLOGY_URI)));
    assertThat(
        response.getBody(), containsString("owl:versionInfo \"%s\"".formatted(ONTOLOGY_VERSION)));
  }

  @Test
  void shouldDeclarePreferredNamespace() throws IOException {
    handler.handleRequest(createRequest(), outputStream, CONTEXT);

    var response = GatewayResponse.fromOutputStream(outputStream, String.class);
    assertThat(response.getBody(), containsString("vann:preferredNamespacePrefix \"approval\""));
    assertThat(
        response.getBody(),
        containsString("vann:preferredNamespaceUri \"%s#\"".formatted(ONTOLOGY_IRI)));
  }

  @Test
  void shouldResolvePrefixToOntologyNamespace() throws IOException {
    handler.handleRequest(createRequest(), outputStream, CONTEXT);

    var response = GatewayResponse.fromOutputStream(outputStream, String.class);
    assertThat(response.getBody(), containsString("@prefix : <%s#> .".formatted(ONTOLOGY_IRI)));
  }

  @Test
  void shouldNotReturnUnresolvableNamespaceOrPlaceholder() throws IOException {
    handler.handleRequest(createRequest(), outputStream, CONTEXT);

    var response = GatewayResponse.fromOutputStream(outputStream, String.class);
    assertThat(response.getBody(), not(containsString(LEGACY_NAMESPACE)));
    assertThat(response.getBody(), not(containsString(ONTOLOGY_IRI_PLACEHOLDER)));
  }

  @Test
  void shouldContainApprovalClass() throws IOException {
    handler.handleRequest(createRequest(), outputStream, CONTEXT);

    var response = GatewayResponse.fromOutputStream(outputStream, String.class);
    assertThat(response.getBody(), containsString(":Approval a rdfs:Class"));
  }

  @Test
  void shouldContainIdentifierClass() throws IOException {
    handler.handleRequest(createRequest(), outputStream, CONTEXT);

    var response = GatewayResponse.fromOutputStream(outputStream, String.class);
    assertThat(response.getBody(), containsString(":Identifier a rdfs:Class"));
  }

  private InputStream createRequest() throws JsonProcessingException {
    return new HandlerRequestBuilder<Void>(JsonUtils.dtoObjectMapper).build();
  }

  private InputStream createRequest(String version) throws JsonProcessingException {
    return new HandlerRequestBuilder<Void>(JsonUtils.dtoObjectMapper)
        .withPathParameters(Map.of(VERSION_PATH_PARAMETER, version))
        .build();
  }
}
