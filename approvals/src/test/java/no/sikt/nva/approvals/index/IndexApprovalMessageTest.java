package no.sikt.nva.approvals.index;

import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class IndexApprovalMessageTest {

  @Test
  void shouldThrowIllegalArgumentExceptionWhenFailingOnParsingMessage() {
    assertThrows(
        IllegalArgumentException.class, () -> IndexApprovalMessage.fromString(randomString()));
  }
}
