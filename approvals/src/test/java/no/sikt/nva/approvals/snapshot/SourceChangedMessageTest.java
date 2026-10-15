package no.sikt.nva.approvals.snapshot;

import static java.util.UUID.randomUUID;
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
            randomString(),
            randomUUID(),
            randomUUID(),
            randomString(),
            randomHandle().value(),
            randomUri(),
            Instant.now());

    var actual = SourceChangedMessage.fromString(expected.toJsonString());

    assertThat(actual, equalTo(expected));
  }

  @Test
  void shouldParseClientIdentifierFromKeysThePipeEmits() {
    var clientIdentifier = randomString();
    var pipeMessage =
        """
        {"eventIdentifier": "%s", "approvalIdentifier": "%s", "customerIdentifier": "%s", \
        "clientIdentifier": "%s", "handle": "%s", "source": "%s", "timestamp": "%s"}
        """
            .formatted(
                randomString(),
                randomUUID(),
                randomUUID(),
                clientIdentifier,
                randomHandle().value(),
                randomUri(),
                Instant.now());

    var message = SourceChangedMessage.fromString(pipeMessage);

    assertThat(message.clientIdentifier(), equalTo(clientIdentifier));
  }

  @Test
  void shouldThrowWithBodyInMessageWhenJsonIsInvalid() {
    var invalidJson = randomString();
    var exception =
        assertThrows(
            IllegalArgumentException.class, () -> SourceChangedMessage.fromString(invalidJson));

    assertThat(exception.getMessage(), containsString(invalidJson));
  }
}
