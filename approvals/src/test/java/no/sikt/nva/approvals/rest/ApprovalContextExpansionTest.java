package no.sikt.nva.approvals.rest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;

import com.apicatalog.jsonld.JsonLd;
import com.apicatalog.jsonld.JsonLdError;
import com.apicatalog.jsonld.document.JsonDocument;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.json.JsonObject;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringReader;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.stubs.FakeContext;
import no.unit.nva.testutils.HandlerRequestBuilder;
import nva.commons.apigateway.GatewayResponse;
import org.junit.jupiter.api.Test;

class ApprovalContextExpansionTest {

  private static final String ONTOLOGY_NAMESPACE = "https://localhost/approval/ontology#";
  private static final String APPROVAL_ID_PROPERTY = "%sapprovalId".formatted(ONTOLOGY_NAMESPACE);
  private static final String IDENTIFIERS_PROPERTY = "%sidentifiers".formatted(ONTOLOGY_NAMESPACE);
  private static final String SOURCE_PROPERTY = "%ssource".formatted(ONTOLOGY_NAMESPACE);
  private static final String HANDLE_PROPERTY = "%shandle".formatted(ONTOLOGY_NAMESPACE);
  private static final String NAMESPACE_PROPERTY = "%snamespace".formatted(ONTOLOGY_NAMESPACE);
  private static final String VALUE_PROPERTY = "%svalue".formatted(ONTOLOGY_NAMESPACE);
  private static final String ID_KEYWORD = "@id";
  private static final String TYPE_KEYWORD = "@type";
  private static final String APPROVAL_DOCUMENT =
      """
      {
        "type": "Approval",
        "id": "https://localhost/approval/6ff5f1b5-97c1-40f0-86ad-2cbd9006eee2",
        "identifier": "6ff5f1b5-97c1-40f0-86ad-2cbd9006eee2",
        "identifiers": [{ "type": "Identifier", "name": "REK", "value": "2024/123" }],
        "source": "https://example.com/source/12345",
        "handle": "https://hdl.handle.net/11250.1/98765"
      }
      """;

  @Test
  void shouldExpandEveryApprovalFieldToItsOwnOntologyProperty() throws IOException, JsonLdError {
    var expanded = expandWithServedContext();

    assertThat(
        expanded.keySet(),
        containsInAnyOrder(
            ID_KEYWORD,
            TYPE_KEYWORD,
            APPROVAL_ID_PROPERTY,
            IDENTIFIERS_PROPERTY,
            SOURCE_PROPERTY,
            HANDLE_PROPERTY));
  }

  @Test
  void shouldExpandIdentifierNameToNamespaceProperty() throws IOException, JsonLdError {
    var expandedIdentifier =
        expandWithServedContext().getJsonArray(IDENTIFIERS_PROPERTY).getJsonObject(0);

    assertThat(
        expandedIdentifier.keySet(),
        containsInAnyOrder(TYPE_KEYWORD, NAMESPACE_PROPERTY, VALUE_PROPERTY));
  }

  private static JsonObject expandWithServedContext() throws IOException, JsonLdError {
    var document = (ObjectNode) JsonUtils.dtoObjectMapper.readTree(APPROVAL_DOCUMENT);
    document.setAll((ObjectNode) JsonUtils.dtoObjectMapper.readTree(fetchServedContext()));
    var jsonDocument =
        JsonDocument.of(new StringReader(JsonUtils.dtoObjectMapper.writeValueAsString(document)));
    return JsonLd.expand(jsonDocument).get().getJsonObject(0);
  }

  private static String fetchServedContext() throws IOException {
    var outputStream = new ByteArrayOutputStream();
    var request = new HandlerRequestBuilder<Void>(JsonUtils.dtoObjectMapper).build();
    new FetchContextHandler().handleRequest(request, outputStream, new FakeContext());
    return GatewayResponse.fromOutputStream(outputStream, String.class).getBody();
  }
}
