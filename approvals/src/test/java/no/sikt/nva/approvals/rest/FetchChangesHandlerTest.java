package no.sikt.nva.approvals.rest;

import static java.net.HttpURLConnection.HTTP_NOT_ACCEPTABLE;
import static java.net.HttpURLConnection.HTTP_OK;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.startsWith;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
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

class FetchChangesHandlerTest {

  private static final String ACCEPT = "Accept";
  private static final String CONTENT_TYPE = "Content-Type";
  private static final String APPLICATION_JSON_LD = "application/ld+json";

  private FetchChangesHandler handler;
  private ByteArrayOutputStream output;

  @BeforeEach
  void setUp() {
    handler = new FetchChangesHandler(new Environment());
    output = new ByteArrayOutputStream();
  }

  @Test
  void shouldReturnEmptyChangeListWhenNoChangesAreRecorded() throws IOException {
    var response = send(Map.of());

    assertThat(response.getStatusCode(), equalTo(HTTP_OK));
    assertThat(response.getBodyObject(ChangeListResponse.class).changes(), empty());
  }

  @ParameterizedTest
  @ValueSource(strings = {"application/json", APPLICATION_JSON_LD})
  void shouldRespondWithRequestedMediaTypeWhenAcceptIsSupported(String mediaType)
      throws IOException {
    var response = send(Map.of(ACCEPT, mediaType));

    assertThat(response.getStatusCode(), equalTo(HTTP_OK));
    assertThat(response.getHeaders().get(CONTENT_TYPE), startsWith(mediaType));
  }

  @Test
  void shouldRespondWithJsonLdWhenAcceptIsMissing() throws IOException {
    var response = send(Map.of());

    assertThat(response.getHeaders().get(CONTENT_TYPE), startsWith(APPLICATION_JSON_LD));
  }

  @Test
  void shouldReturnNotAcceptableWhenAcceptIsUnsupported() throws IOException {
    var response = send(Map.of(ACCEPT, "text/html"));

    assertThat(response.getStatusCode(), equalTo(HTTP_NOT_ACCEPTABLE));
  }

  private GatewayResponse<ChangeListResponse> send(Map<String, String> headers) throws IOException {
    var request =
        new HandlerRequestBuilder<Void>(JsonUtils.dtoObjectMapper).withHeaders(headers).build();
    handler.handleRequest(request, output, new FakeContext());
    return GatewayResponse.fromOutputStream(output, ChangeListResponse.class);
  }
}
