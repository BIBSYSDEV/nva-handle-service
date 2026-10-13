package no.sikt.nva.approvals.utils;

import static java.util.UUID.randomUUID;
import static no.sikt.nva.approvals.utils.UriConstants.approvalId;
import static no.sikt.nva.approvals.utils.UriConstants.changeId;
import static no.sikt.nva.approvals.utils.UriConstants.changeListId;
import static no.sikt.nva.approvals.utils.UriConstants.context;
import static no.sikt.nva.approvals.utils.UriConstants.ontology;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import java.net.URI;
import no.unit.nva.identifiers.SortableIdentifier;
import org.junit.jupiter.api.Test;

class UriConstantsTest {

  private static final String API_HOST = "api.unittest.nva.unit.no";
  private static final String APPROVAL_FORMAT = "https://api.unittest.nva.unit.no/approval/%s";
  private static final String CHANGE_LIST_FORMAT =
      "https://api.unittest.nva.unit.no/approval/%s/change";
  private static final String CHANGE_FORMAT =
      "https://api.unittest.nva.unit.no/approval/%s/change/%s";
  private static final URI VERSIONED_CONTEXT =
      URI.create("https://api.unittest.nva.unit.no/approval/context/v1");
  private static final URI ONTOLOGY =
      URI.create("https://api.unittest.nva.unit.no/approval/ontology");

  @Test
  void shouldBuildApprovalIdUnderApprovalPathOnApiHost() {
    var approvalIdentifier = randomUUID();

    assertThat(
        approvalId(API_HOST, approvalIdentifier),
        equalTo(URI.create(APPROVAL_FORMAT.formatted(approvalIdentifier))));
  }

  @Test
  void shouldBuildChangeListIdUnderApproval() {
    var approvalIdentifier = randomUUID();

    assertThat(
        changeListId(API_HOST, approvalIdentifier),
        equalTo(URI.create(CHANGE_LIST_FORMAT.formatted(approvalIdentifier))));
  }

  @Test
  void shouldBuildChangeIdUnderChangeListOfApproval() {
    var approvalIdentifier = randomUUID();
    var changeIdentifier = SortableIdentifier.next();

    assertThat(
        changeId(API_HOST, approvalIdentifier, changeIdentifier),
        equalTo(URI.create(CHANGE_FORMAT.formatted(approvalIdentifier, changeIdentifier))));
  }

  @Test
  void shouldBuildCurrentVersionOfContextUnderApprovalPath() {
    assertThat(context(API_HOST), equalTo(VERSIONED_CONTEXT));
  }

  @Test
  void shouldBuildOntologyUnderApprovalPath() {
    assertThat(ontology(API_HOST), equalTo(ONTOLOGY));
  }
}
