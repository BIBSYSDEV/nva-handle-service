package no.sikt.nva.approvals.rest;

import static java.net.HttpURLConnection.HTTP_ACCEPTED;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.amazonaws.services.lambda.runtime.Context;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.stubs.FakeContext;
import no.unit.nva.testutils.HandlerRequestBuilder;
import nva.commons.apigateway.GatewayResponse;
import nva.commons.core.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ReceiveEventHandlerTest {

  private static final Context CONTEXT = new FakeContext();
  private ByteArrayOutputStream output;
  private ReceiveEventHandler handler;

  @BeforeEach
  void setUp() {
    output = new ByteArrayOutputStream();
    handler = new ReceiveEventHandler(new Environment());
  }

  @Test
  void shouldReturnAcceptedForValidNotification() throws IOException {
    var request = new HandlerRequestBuilder<Void>(JsonUtils.dtoObjectMapper).build();

    handler.handleRequest(request, output, CONTEXT);

    var response = GatewayResponse.fromOutputStream(output, Void.class);
    assertEquals(HTTP_ACCEPTED, response.getStatusCode());
  }
}
