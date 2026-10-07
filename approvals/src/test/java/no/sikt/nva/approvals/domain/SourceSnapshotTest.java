package no.sikt.nva.approvals.domain;

import static java.util.UUID.randomUUID;
import static no.sikt.nva.approvals.utils.TestUtils.randomContent;
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

    var first = SourceSnapshot.create(sourceChange, randomContent());
    var redelivered = SourceSnapshot.create(sourceChange, randomContent());

    assertThat(redelivered.identifier(), equalTo(first.identifier()));
  }

  @Test
  void shouldCreateDifferentIdentifiersForTheSameApprovalButDifferentEvents() {
    var approvalIdentifier = randomUUID();

    var first =
        SourceSnapshot.create(
            randomSourceChange(approvalIdentifier, randomString()), randomContent());
    var second =
        SourceSnapshot.create(
            randomSourceChange(approvalIdentifier, randomString()), randomContent());

    assertThat(second.identifier(), not(equalTo(first.identifier())));
  }

  @Test
  void shouldCreateDifferentIdentifiersForSameEventIdentifierOnDifferentApprovals() {
    var eventIdentifier = randomString();

    var first =
        SourceSnapshot.create(randomSourceChange(randomUUID(), eventIdentifier), randomContent());
    var second =
        SourceSnapshot.create(randomSourceChange(randomUUID(), eventIdentifier), randomContent());

    assertThat(second.identifier(), not(equalTo(first.identifier())));
  }
}
