package no.sikt.nva.approvals.snapshot;

import static java.util.UUID.randomUUID;
import static no.sikt.nva.approvals.utils.TestUtils.randomSourceChange;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;

import org.junit.jupiter.api.Test;

class SourceSnapshotTest {

  @Test
  void shouldCreateSameSnapshotIdentifierForTheSameApprovalAndEvent() {
    var sourceChange = randomSourceChange();

    var first = SourceSnapshot.create(sourceChange);
    var redelivered = SourceSnapshot.create(sourceChange);

    assertThat(redelivered.identifier(), equalTo(first.identifier()));
  }

  @Test
  void shouldCreateDifferentIdentifiersForTheSameApprovalButDifferentEvents() {
    var approvalIdentifier = randomUUID();

    var first = SourceSnapshot.create(randomSourceChange(approvalIdentifier, randomString()));
    var second = SourceSnapshot.create(randomSourceChange(approvalIdentifier, randomString()));

    assertThat(second.identifier(), not(equalTo(first.identifier())));
  }

  @Test
  void shouldCreateDifferentIdentifiersForSameEventIdentifierOnDifferentApprovals() {
    var eventIdentifier = randomString();

    var first = SourceSnapshot.create(randomSourceChange(randomUUID(), eventIdentifier));
    var second = SourceSnapshot.create(randomSourceChange(randomUUID(), eventIdentifier));

    assertThat(second.identifier(), not(equalTo(first.identifier())));
  }
}
