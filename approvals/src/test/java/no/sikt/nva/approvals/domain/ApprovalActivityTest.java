package no.sikt.nva.approvals.domain;

import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class ApprovalActivityTest {

  @Test
  void shouldReturnActivityWithValue() {
    var activity = ApprovalActivity.fromValue(ApprovalActivity.UPDATE_APPROVAL.getValue());

    assertThat(activity, equalTo(ApprovalActivity.UPDATE_APPROVAL));
  }

  @Test
  void shouldThrowWithValueWhenActivityIsUnknown() {
    var unknownValue = randomString();

    var exception =
        assertThrows(
            IllegalArgumentException.class, () -> ApprovalActivity.fromValue(unknownValue));

    assertThat(exception.getMessage(), containsString(unknownValue));
  }
}
