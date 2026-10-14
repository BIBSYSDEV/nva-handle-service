package no.sikt.nva.approvals.rest;

import static no.sikt.nva.approvals.utils.TestUtils.randomApproval;
import static no.sikt.nva.approvals.utils.TestUtils.randomHandle;
import static no.sikt.nva.approvals.utils.TestUtils.randomSourceSnapshot;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import java.net.URI;
import java.util.List;
import no.sikt.nva.approvals.domain.Approval;
import no.sikt.nva.approvals.domain.ChangeList;
import org.junit.jupiter.api.Test;

class ChangeHistoryResponseTest {

  private static final String API_HOST = "api.unittest.nva.unit.no";

  @Test
  void shouldIdentifyApprovalTheChangesAreAbout() {
    var approval = randomApproval(randomHandle());

    var response = ChangeHistoryResponse.fromChangeList(changeListOf(approval), API_HOST);

    var expectedId = URI.create("https://" + API_HOST + "/approval/" + approval.identifier());
    assertThat(response.about().id(), equalTo(expectedId));
  }

  @Test
  void shouldIncludeHandleOfApprovalTheChangesAreAbout() {
    var handle = randomHandle();

    var response =
        ChangeHistoryResponse.fromChangeList(changeListOf(randomApproval(handle)), API_HOST);

    assertThat(response.about().handle(), equalTo(handle.value()));
  }

  @Test
  void shouldReferenceVersionedContext() {
    var response =
        ChangeHistoryResponse.fromChangeList(
            changeListOf(randomApproval(randomHandle())), API_HOST);

    var expectedContext = URI.create("https://" + API_HOST + "/approval/context/v1");
    assertThat(response.context(), equalTo(expectedContext));
  }

  private static ChangeList changeListOf(Approval approval) {
    return new ChangeList(approval, List.of(randomSourceSnapshot(approval.identifier())), false);
  }
}
