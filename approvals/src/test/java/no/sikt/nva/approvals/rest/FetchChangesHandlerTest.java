package no.sikt.nva.approvals.rest;

import static java.net.HttpURLConnection.HTTP_OK;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.stubs.FakeContext;
import no.unit.nva.testutils.HandlerRequestBuilder;
import nva.commons.apigateway.GatewayResponse;
import nva.commons.core.Environment;
import org.junit.jupiter.api.Test;

class FetchChangesHandlerTest {

  @Test
  void shouldReturnEmptyChangeListWhenNoChangesAreRecorded() throws IOException {
    var output = new ByteArrayOutputStream();
    var request = new HandlerRequestBuilder<Void>(JsonUtils.dtoObjectMapper).build();

    new FetchChangesHandler(new Environment()).handleRequest(request, output, new FakeContext());

    var response = GatewayResponse.fromOutputStream(output, ChangeListResponse.class);
    assertThat(response.getStatusCode(), equalTo(HTTP_OK));
    assertThat(response.getBodyObject(ChangeListResponse.class).changes(), empty());
  }
}
