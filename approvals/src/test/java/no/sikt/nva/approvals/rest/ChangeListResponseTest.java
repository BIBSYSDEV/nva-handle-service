package no.sikt.nva.approvals.rest;

import static java.util.UUID.randomUUID;
import static no.sikt.nva.approvals.utils.TestUtils.randomSourceSnapshot;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import java.net.URI;
import java.util.List;
import no.sikt.nva.approvals.domain.ChangeList;
import org.junit.jupiter.api.Test;

class ChangeListResponseTest {

  private static final String API_HOST = "api.unittest.nva.unit.no";
  private static final String CHANGE_LIST_FORMAT =
      "https://api.unittest.nva.unit.no/approval/%s/change";

  @Test
  void shouldIdentifyChangeListAsChangesOfItsApproval() {
    var approvalIdentifier = randomUUID();
    var changeList = new ChangeList(List.of(randomSourceSnapshot(approvalIdentifier)), false);

    var response = ChangeListResponse.fromChangeList(changeList, approvalIdentifier, API_HOST);

    assertThat(
        response.id(), equalTo(URI.create(CHANGE_LIST_FORMAT.formatted(approvalIdentifier))));
  }
}
