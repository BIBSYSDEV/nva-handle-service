package no.sikt.nva.approvals.snapshot;

import static java.util.UUID.randomUUID;
import static no.sikt.nva.approvals.utils.TestUtils.randomSourceChange;
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

    var first = SourceSnapshot.create(sourceChange, Instant.now());
    var redelivered = SourceSnapshot.create(sourceChange, Instant.now().plusSeconds(1));

    assertThat(redelivered.identifier(), equalTo(first.identifier()));
  }

  @Test
  void shouldCreateDifferentIdentifiersForTheSameApprovalButDifferentEvents() {
    var approvalIdentifier = randomUUID();

    var first =
        SourceSnapshot.create(
            randomSourceChange(approvalIdentifier, randomString()), Instant.now());
    var second =
        SourceSnapshot.create(
            randomSourceChange(approvalIdentifier, randomString()), Instant.now());

    assertThat(second.identifier(), not(equalTo(first.identifier())));
  }

  @Test
  void shouldCreateDifferentIdentifiersForSameEventIdentifierOnDifferentApprovals() {
    var eventIdentifier = randomString();

    var first =
        SourceSnapshot.create(randomSourceChange(randomUUID(), eventIdentifier), Instant.now());
    var second =
        SourceSnapshot.create(randomSourceChange(randomUUID(), eventIdentifier), Instant.now());

    assertThat(second.identifier(), not(equalTo(first.identifier())));
  }
}
