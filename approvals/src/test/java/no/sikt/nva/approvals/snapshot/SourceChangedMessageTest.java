package no.sikt.nva.approvals.snapshot;

import static no.sikt.nva.approvals.utils.TestUtils.randomHandle;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class SourceChangedMessageTest {

  @Test
  void shouldParseSourceChangedMessageFromJson() {
    var expected =
        new SourceChangedMessage(
            randomString(), randomHandle().value(), randomUri(), Instant.now());

    var actual = SourceChangedMessage.fromString(expected.toJsonString());

    assertThat(actual, equalTo(expected));
  }

  @Test
  void shouldThrowWithBodyInMessageWhenJsonIsInvalid() {
    var invalidJson = "not json";
    var exception =
        assertThrows(
            IllegalArgumentException.class, () -> SourceChangedMessage.fromString(invalidJson));

    assertThat(exception.getMessage(), containsString(invalidJson));
  }
}
