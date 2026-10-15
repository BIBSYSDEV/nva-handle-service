package no.sikt.nva.approvals.domain;

import static java.util.UUID.randomUUID;
import static no.sikt.nva.approvals.utils.TestUtils.randomContent;
import static no.sikt.nva.approvals.utils.TestUtils.randomSourceChange;
import static no.sikt.nva.approvals.utils.TestUtils.randomTimestamp;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class SourceSnapshotTest {

  @Test
  void shouldCreateSameSnapshotIdentifierForTheSameApprovalAndEvent() {
    var sourceChange = randomSourceChange();

    var first = SourceSnapshot.create(sourceChange, randomContent(), randomTimestamp());
    var redelivered = SourceSnapshot.create(sourceChange, randomContent(), randomTimestamp());

    assertThat(redelivered.identifier(), equalTo(first.identifier()));
  }

  @Test
  void shouldTruncateCreatedDateToMicroseconds() {
    var createdDate = Instant.parse("2026-10-14T10:00:00.123456789Z");

    var snapshot = SourceSnapshot.create(randomSourceChange(), randomContent(), createdDate);

    assertThat(snapshot.createdDate(), equalTo(Instant.parse("2026-10-14T10:00:00.123456Z")));
  }

  @Test
  void shouldReturnCustomer() {
    var sourceChange = randomSourceChange();

    var snapshot = SourceSnapshot.create(sourceChange, randomContent(), randomTimestamp());

    assertThat(snapshot.customerIdentifier(), equalTo(sourceChange.customerIdentifier()));
  }

  @Test
  void shouldCreateDifferentIdentifiersForTheSameApprovalButDifferentEvents() {
    var approvalIdentifier = randomUUID();

    var first =
        SourceSnapshot.create(
            randomSourceChange(approvalIdentifier, randomString()),
            randomContent(),
            randomTimestamp());
    var second =
        SourceSnapshot.create(
            randomSourceChange(approvalIdentifier, randomString()),
            randomContent(),
            randomTimestamp());

    assertThat(second.identifier(), not(equalTo(first.identifier())));
  }

  @Test
  void shouldCreateDifferentIdentifiersForSameEventIdentifierOnDifferentApprovals() {
    var eventIdentifier = randomString();

    var first =
        SourceSnapshot.create(
            randomSourceChange(randomUUID(), eventIdentifier), randomContent(), randomTimestamp());
    var second =
        SourceSnapshot.create(
            randomSourceChange(randomUUID(), eventIdentifier), randomContent(), randomTimestamp());

    assertThat(second.identifier(), not(equalTo(first.identifier())));
  }
}
