package no.sikt.nva.approvals.domain;

import static java.util.UUID.randomUUID;
import static no.sikt.nva.approvals.utils.TestUtils.randomApproval;
import static no.sikt.nva.approvals.utils.TestUtils.randomHandle;
import static no.sikt.nva.approvals.utils.TestUtils.randomIdentifier;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Collections;
import org.junit.jupiter.api.Test;

class ApprovalTest {

  @Test
  void shouldThrowExceptionWhenApprovalInitiatedWithoutSource() {
    assertThrows(NullPointerException.class, () -> randomApproval(randomUUID(), null));
  }

  @Test
  void shouldThrowExceptionWhenApprovalInitiatedWithoutHandle() {
    assertThrows(NullPointerException.class, () -> randomApproval(null, randomIdentifier()));
  }

  @Test
  void shouldThrowExceptionWhenApprovalInitiatedWithoutIdentifier() {
    assertThrows(NullPointerException.class, () -> randomApproval(null, randomUri()));
  }

  @Test
  void shouldThrowExceptionWhenApprovalInitiatedWithEmptyIdentifiers() {
    assertThrows(
        IllegalArgumentException.class,
        () -> randomApproval(Collections.emptyList(), randomUUID()));
  }

  @Test
  void shouldThrowCustomerMismatchWhenCustomerDiffers() {
    var approval = randomApproval(randomUri(), randomHandle(), randomUUID());

    assertThrows(CustomerMismatchException.class, () -> approval.ensureOwnedBy(randomUUID()));
  }

  @Test
  void shouldThrowSourceMismatchWhenSourceDiffers() {
    var approval = randomApproval(randomUri(), randomHandle(), randomUUID());

    assertThrows(SourceMismatchException.class, () -> approval.ensureSourceIs(randomUri()));
  }
}
