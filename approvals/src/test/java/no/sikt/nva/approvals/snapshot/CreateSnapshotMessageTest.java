package no.sikt.nva.approvals.snapshot;

import static java.util.UUID.randomUUID;
import static no.sikt.nva.approvals.domain.ApprovalActivity.CREATE_APPROVAL;
import static no.sikt.nva.approvals.utils.TestUtils.randomApproval;
import static no.sikt.nva.approvals.utils.TestUtils.randomHandle;
import static no.sikt.nva.approvals.utils.TestUtils.randomRevision;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;
import no.sikt.nva.approvals.domain.ApprovalRevision;
import no.sikt.nva.approvals.domain.ApprovalRevisionTrigger;
import no.sikt.nva.approvals.persistence.ApprovalImage;
import no.unit.nva.identifiers.SortableIdentifier;
import org.junit.jupiter.api.Test;

class CreateSnapshotMessageTest {

  @Test
  void shouldParseSourceChangedMessageToCreateSnapshotMessage() {
    var sourceChangedMessage = randomSourceChangedMessage();

    var actual = CreateSnapshotMessage.fromString(sourceChangedMessage.toJsonString());

    assertThat(actual, equalTo(sourceChangedMessage));
  }

  @Test
  void shouldParseApprovalRevisionToCreateSnapshotMessage() {
    var expected = approvalRevisionMessage();

    var actual = CreateSnapshotMessage.fromString(expected.toJsonString());

    assertThat(actual, equalTo(expected));
  }

  @Test
  void shouldCreateSourceChangeWithSourceOfApprovalInRevision() {
    var revision = randomRevision(randomApproval(randomHandle()), CREATE_APPROVAL);

    var sourceChange = approvalRevisionMessage(revision).toCreateSnapshotRequest();

    assertThat(sourceChange.source(), equalTo(revision.approval().source()));
  }

  @Test
  void shouldCreateSourceChangeTriggeredByTheRevision() {
    var revision = randomRevision(randomApproval(randomHandle()), CREATE_APPROVAL);

    var sourceChange = approvalRevisionMessage(revision).toCreateSnapshotRequest();

    assertThat(
        sourceChange.trigger(),
        equalTo(new ApprovalRevisionTrigger(revision.identifier(), revision.activity())));
  }

  @Test
  void shouldCreateSourceChangeWithAgentThatChangedTheApproval() {
    var revision = randomRevision(randomApproval(randomHandle()), CREATE_APPROVAL);

    var sourceChange = approvalRevisionMessage(revision).toCreateSnapshotRequest();

    assertThat(sourceChange.agent(), equalTo(revision.agent()));
  }

  @Test
  void shouldThrowWithBodyInMessageWhenJsonIsInvalid() {
    var invalidJson = randomString();
    var exception =
        assertThrows(
            IllegalArgumentException.class, () -> CreateSnapshotMessage.fromString(invalidJson));

    assertThat(exception.getMessage(), containsString(invalidJson));
  }

  private static SourceChangedMessage randomSourceChangedMessage() {
    return new SourceChangedMessage(
        SortableIdentifier.next(),
        randomString(),
        randomUUID(),
        randomUUID(),
        randomString(),
        randomHandle().value(),
        randomUri(),
        Instant.now());
  }

  private static ApprovalRevisionMessage approvalRevisionMessage() {
    return approvalRevisionMessage(randomRevision(randomApproval(randomHandle()), CREATE_APPROVAL));
  }

  private static ApprovalRevisionMessage approvalRevisionMessage(ApprovalRevision revision) {
    var agent = revision.agent();
    return new ApprovalRevisionMessage(
        revision.identifier(),
        revision.approval().identifier(),
        agent.customerIdentifier(),
        agent.clientIdentifier(),
        revision.activity(),
        ApprovalImage.fromApprovalRevision(revision).toJsonString(),
        revision.createdDate());
  }
}
