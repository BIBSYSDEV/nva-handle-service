package no.sikt.nva.approvals.snapshot;

import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.net.URI;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class SourceChangedMessageTest {

  @Test
  void shouldParseSourceChangedMessageFromJson() {
    var expected =
        new SourceChangedMessage(
            randomString(),
            URI.create("https://hdl.handle.net/11250.1/1"),
            URI.create("https://example.org/source/1"),
            Instant.parse("2026-09-30T10:00:00Z"));

    var actual = SourceChangedMessage.fromString(expected.toJsonString());

    assertThat(actual, equalTo(expected));
  }

  @Test
  void shouldThrowWithBodyInMessageWhenJsonIsInvalid() {
    var exception =
        assertThrows(
            IllegalArgumentException.class, () -> SourceChangedMessage.fromString("not json"));

    assertThat(exception.getMessage(), containsString("not json"));
  }
}
