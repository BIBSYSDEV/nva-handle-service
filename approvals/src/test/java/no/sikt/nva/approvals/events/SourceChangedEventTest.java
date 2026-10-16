package no.sikt.nva.approvals.events;

import static no.sikt.nva.approvals.utils.TestUtils.randomAgent;
import static no.sikt.nva.approvals.utils.TestUtils.randomHandle;
import static no.sikt.nva.approvals.utils.TestUtils.randomTimestamp;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;

import org.junit.jupiter.api.Test;

class SourceChangedEventTest {

  private static final String SORTABLE_TIMESTAMP_PREFIX = "%012x-";

  @Test
  void shouldCreateSameIdentifierWhenEventIsRedelivered() {
    var eventIdentifier = randomString();
    var source = randomUri();
    var handle = randomHandle();
    var timestamp = randomTimestamp();
    var agent = randomAgent();

    var first = SourceChangedEvent.create(eventIdentifier, source, handle, timestamp, agent);
    var redelivered = SourceChangedEvent.create(eventIdentifier, source, handle, timestamp, agent);

    assertThat(redelivered.identifier(), equalTo(first.identifier()));
  }

  @Test
  void shouldCreateDifferentIdentifiersForSameEventIdentifierOnDifferentHandles() {
    var eventIdentifier = randomString();
    var timestamp = randomTimestamp();
    var agent = randomAgent();

    var first =
        SourceChangedEvent.create(eventIdentifier, randomUri(), randomHandle(), timestamp, agent);
    var second =
        SourceChangedEvent.create(eventIdentifier, randomUri(), randomHandle(), timestamp, agent);

    assertThat(second.identifier(), not(equalTo(first.identifier())));
  }

  @Test
  void shouldCreateDifferentIdentifiersForSameEventIdentifierOnDifferentCustomers() {
    var eventIdentifier = randomString();
    var handle = randomHandle();
    var timestamp = randomTimestamp();

    var first =
        SourceChangedEvent.create(eventIdentifier, randomUri(), handle, timestamp, randomAgent());
    var second =
        SourceChangedEvent.create(eventIdentifier, randomUri(), handle, timestamp, randomAgent());

    assertThat(second.identifier(), not(equalTo(first.identifier())));
  }

  @Test
  void shouldPlaceEventTimestampInIdentifier() {
    var timestamp = randomTimestamp();

    var event =
        SourceChangedEvent.create(
            randomString(), randomUri(), randomHandle(), timestamp, randomAgent());

    assertThat(
        event.identifier().toString(),
        startsWith(SORTABLE_TIMESTAMP_PREFIX.formatted(timestamp.toEpochMilli())));
  }
}
