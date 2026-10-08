package no.sikt.nva.approvals.rest;

import static no.sikt.nva.approvals.domain.ApprovalActivity.CREATE_APPROVAL;
import static no.sikt.nva.approvals.domain.ApprovalActivity.UPDATE_APPROVAL;
import static no.sikt.nva.approvals.utils.TestUtils.randomApproval;
import static no.sikt.nva.approvals.utils.TestUtils.randomHandle;
import static no.sikt.nva.approvals.utils.TestUtils.randomSourceSnapshot;
import static no.unit.nva.testutils.RandomDataGenerator.randomInstant;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import java.net.URI;
import no.sikt.nva.approvals.domain.ApprovalActivity;
import no.sikt.nva.approvals.domain.ApprovalRevision;
import no.sikt.nva.approvals.domain.SourceSnapshot;
import no.sikt.nva.approvals.rest.HarvestSource.Trigger;
import org.junit.jupiter.api.Test;

class ChangeResponseTest {

  private static final String API_HOST = "api.unittest.nva.unit.no";
  private static final URI CONTEXT =
      URI.create("https://api.unittest.nva.unit.no/approval/context/v1");

  @Test
  void shouldDescribeCreatedRevisionAsGeneratedByCreateApproval() {
    var response = revisionResponse(revision(CREATE_APPROVAL));

    assertThat(response.wasGeneratedBy(), equalTo(new CreateApprovalActivity()));
  }

  @Test
  void shouldDescribeUpdatedRevisionAsGeneratedByUpdateApproval() {
    var response = revisionResponse(revision(UPDATE_APPROVAL));

    assertThat(response.wasGeneratedBy(), equalTo(new UpdateApprovalActivity()));
  }

  @Test
  void shouldDescribeRevisionAsGeneratedAtItsCreatedDate() {
    var revision = revision(UPDATE_APPROVAL);

    var response = revisionResponse(revision);

    assertThat(response.generatedAtTime(), equalTo(revision.createdDate()));
  }

  @Test
  void shouldPointRevisionToContextOnApiHost() {
    var response = revisionResponse(revision(UPDATE_APPROVAL));

    assertThat(response.context(), equalTo(CONTEXT));
  }

  @Test
  void shouldDescribeSnapshotAsGeneratedByHarvestTriggeredBySourceChangedEvent() {
    var snapshot = randomSourceSnapshot();

    var response = snapshotResponse(snapshot);

    assertThat(
        response.wasGeneratedBy(),
        equalTo(new HarvestSource(new Trigger(snapshot.eventIdentifier()))));
  }

  @Test
  void shouldDescribeSnapshotAsRetrievedAtItsCreatedDate() {
    var snapshot = randomSourceSnapshot();

    var response = snapshotResponse(snapshot);

    assertThat(response.retrievedAt(), equalTo(snapshot.createdDate()));
  }

  @Test
  void shouldDescribeSourceAndContentTypeOfSnapshot() {
    var snapshot = randomSourceSnapshot();

    var response = snapshotResponse(snapshot);

    assertThat(response.source(), equalTo(snapshot.source()));
    assertThat(response.contentType(), equalTo(snapshot.content().type()));
  }

  @Test
  void shouldPrefixContentHashOfSnapshotWithItsAlgorithm() {
    var snapshot = randomSourceSnapshot();

    var response = snapshotResponse(snapshot);

    assertThat(response.contentHash(), equalTo("sha256:" + snapshot.content().hash()));
  }

  @Test
  void shouldPointSnapshotToContextOnApiHost() {
    var response = snapshotResponse(randomSourceSnapshot());

    assertThat(response.context(), equalTo(CONTEXT));
  }

  private static ApprovalRevision revision(ApprovalActivity activity) {
    return ApprovalRevision.create(
        randomApproval(randomHandle()), activity, randomUri(), randomUri(), randomInstant());
  }

  private static ApprovalRevisionResponse revisionResponse(ApprovalRevision revision) {
    return (ApprovalRevisionResponse) ChangeResponse.fromChange(revision, API_HOST);
  }

  private static SourceSnapshotResponse snapshotResponse(SourceSnapshot snapshot) {
    return (SourceSnapshotResponse) ChangeResponse.fromChange(snapshot, API_HOST);
  }
}
