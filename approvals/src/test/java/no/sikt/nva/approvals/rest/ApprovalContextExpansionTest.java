package no.sikt.nva.approvals.rest;

import static no.sikt.nva.approvals.utils.JsonLdTestUtils.APPROVAL_ONTOLOGY_NAMESPACE;
import static no.sikt.nva.approvals.utils.JsonLdTestUtils.expandWithServedContext;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;

import com.apicatalog.jsonld.JsonLdError;
import java.io.IOException;
import org.junit.jupiter.api.Test;

class ApprovalContextExpansionTest {

  private static final String APPROVAL_ID_PROPERTY =
      "%sapprovalId".formatted(APPROVAL_ONTOLOGY_NAMESPACE);
  private static final String IDENTIFIERS_PROPERTY =
      "%sidentifiers".formatted(APPROVAL_ONTOLOGY_NAMESPACE);
  private static final String SOURCE_PROPERTY = "%ssource".formatted(APPROVAL_ONTOLOGY_NAMESPACE);
  private static final String HANDLE_PROPERTY = "%shandle".formatted(APPROVAL_ONTOLOGY_NAMESPACE);
  private static final String NAMESPACE_PROPERTY =
      "%snamespace".formatted(APPROVAL_ONTOLOGY_NAMESPACE);
  private static final String VALUE_PROPERTY = "%svalue".formatted(APPROVAL_ONTOLOGY_NAMESPACE);
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
    var expanded = expandWithServedContext(APPROVAL_DOCUMENT);

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
        expandWithServedContext(APPROVAL_DOCUMENT)
            .getJsonArray(IDENTIFIERS_PROPERTY)
            .getJsonObject(0);

    assertThat(
        expandedIdentifier.keySet(),
        containsInAnyOrder(TYPE_KEYWORD, NAMESPACE_PROPERTY, VALUE_PROPERTY));
  }
}
