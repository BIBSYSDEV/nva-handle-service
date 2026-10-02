package no.sikt.nva.approvals.rest;

import static java.net.HttpURLConnection.HTTP_NOT_ACCEPTABLE;
import static java.net.HttpURLConnection.HTTP_NOT_FOUND;
import static java.util.UUID.randomUUID;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;

import com.fasterxml.jackson.core.JsonProcessingException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.stubs.FakeContext;
import no.unit.nva.testutils.HandlerRequestBuilder;
import nva.commons.apigateway.GatewayResponse;
import nva.commons.core.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.zalando.problem.Problem;

class FetchChangeHandlerTest {

  private static final FakeContext CONTEXT = new FakeContext();
  private FetchChangeHandler handler;
  private ByteArrayOutputStream output;

  @BeforeEach
  void setUp() {
    output = new ByteArrayOutputStream();
    handler = new FetchChangeHandler(new Environment());
  }

  @Test
  void shouldReturnNotFoundWhenChangeIsNotRecordedForApproval() throws IOException {
    var changeId = randomString();
    var request = createRequest(changeId);

    handler.handleRequest(request, output, CONTEXT);

    var response = GatewayResponse.fromOutputStream(output, Problem.class);

    assertThat(response.getStatusCode(), equalTo(HTTP_NOT_FOUND));
    assertThat(response.getBodyObject(Problem.class).getDetail(), containsString(changeId));
  }

  @ParameterizedTest
  @ValueSource(strings = {"application/json", "application/ld+json"})
  void shouldNotRejectRequestWhenAcceptIsSupported(String mediaType) throws IOException {
    handler.handleRequest(createRequest(randomString(), mediaType), output, CONTEXT);

    var response = GatewayResponse.fromOutputStream(output, Problem.class);

    assertThat(response.getStatusCode(), equalTo(HTTP_NOT_FOUND));
  }

  @Test
  void shouldReturnNotAcceptableWhenAcceptIsUnsupported() throws IOException {
    handler.handleRequest(createRequest(randomString(), "text/html"), output, CONTEXT);

    var response = GatewayResponse.fromOutputStream(output, Problem.class);

    assertThat(response.getStatusCode(), equalTo(HTTP_NOT_ACCEPTABLE));
  }

  private static InputStream createRequest(String changeId) throws JsonProcessingException {
    return new HandlerRequestBuilder<Void>(JsonUtils.dtoObjectMapper)
        .withPathParameters(Map.of("approvalId", randomUUID().toString(), "changeId", changeId))
        .build();
  }

  private static InputStream createRequest(String changeId, String accept)
      throws JsonProcessingException {
    return new HandlerRequestBuilder<Void>(JsonUtils.dtoObjectMapper)
        .withPathParameters(Map.of("approvalId", randomUUID().toString(), "changeId", changeId))
        .withHeaders(Map.of("Accept", accept))
        .build();
  }
}
