package no.sikt.nva.approvals.rest;

import static java.util.Objects.isNull;
import static no.sikt.nva.approvals.domain.ApprovalActivity.CREATE_APPROVAL;
import static no.sikt.nva.approvals.utils.TestUtils.randomApproval;
import static no.sikt.nva.approvals.utils.TestUtils.randomHandle;
import static no.sikt.nva.approvals.utils.TestUtils.randomRevision;
import static no.sikt.nva.approvals.utils.TestUtils.randomSourceSnapshot;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.core.JsonProcessingException;
import java.net.URI;
import java.util.List;
import no.sikt.nva.approvals.domain.Approval;
import no.sikt.nva.approvals.domain.ChangeList;
import no.unit.nva.commons.json.JsonUtils;
import org.junit.jupiter.api.Test;

class ChangeListResponseTest {

  private static final String API_HOST = "api.unittest.nva.unit.no";

  @Test
  void shouldIdentifyApprovalTheChangesAreAbout() {
    var approval = randomApproval(randomHandle());

    var response = ChangeListResponse.fromChangeList(changeListOf(approval), API_HOST);

    var expectedId = URI.create("https://" + API_HOST + "/approval/" + approval.identifier());
    assertThat(response.about().id(), equalTo(expectedId));
  }

  @Test
  void shouldListHandleAsOtherUriOfApprovalTheChangesAreAbout() {
    var handle = randomHandle();

    var response =
        ChangeListResponse.fromChangeList(changeListOf(randomApproval(handle)), API_HOST);

    assertThat(response.about().sameAs(), equalTo(List.of(handle.value())));
  }

  @Test
  void shouldReferenceVersionedContext() {
    var response =
        ChangeListResponse.fromChangeList(changeListOf(randomApproval(randomHandle())), API_HOST);

    var expectedContext = URI.create("https://" + API_HOST + "/approval/context/v1");
    assertThat(response.context(), equalTo(expectedContext));
  }

  @Test
  void shouldNotReturnContextForChangeInChangeList() throws JsonProcessingException {
    var response =
        ChangeListResponse.fromChangeList(changeListOf(randomApproval(randomHandle())), API_HOST);

    var json = JsonUtils.dtoObjectMapper.writeValueAsString(response);

    var changes = JsonUtils.dtoObjectMapper.readValue(json, ChangeListResponse.class);

    assertTrue(
        changes.changes().stream().allMatch(changeResponse -> isNull(changeResponse.context())));
  }

  private static ChangeList changeListOf(Approval approval) {
    return new ChangeList(
        approval,
        List.of(
            randomRevision(approval, CREATE_APPROVAL), randomSourceSnapshot(approval.identifier())),
        false);
  }
}
