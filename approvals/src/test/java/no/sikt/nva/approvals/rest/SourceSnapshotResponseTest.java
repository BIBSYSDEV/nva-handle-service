package no.sikt.nva.approvals.rest;

import static no.sikt.nva.approvals.utils.TestUtils.randomSourceSnapshot;
import static no.sikt.nva.approvals.utils.UriConstants.changeId;
import static no.sikt.nva.approvals.utils.UriConstants.clientId;
import static no.sikt.nva.approvals.utils.UriConstants.customerId;
import static no.sikt.nva.approvals.utils.UriConstants.eventId;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;

import com.fasterxml.jackson.core.JsonProcessingException;
import no.sikt.nva.approvals.domain.SourceSnapshot;
import no.unit.nva.commons.json.JsonUtils;
import org.junit.jupiter.api.Test;

class SourceSnapshotResponseTest {

  private static final String API_HOST = "api.unittest.nva.unit.no";
  private static final String SHA256 = "sha256";

  @Test
  void shouldIdentifySnapshotUnderChangesOfApproval() {
    var snapshot = randomSourceSnapshot();

    var response = SourceSnapshotResponse.create(snapshot, API_HOST);

    var expectedId = changeId(API_HOST, snapshot.approvalIdentifier(), snapshot.identifier());
    assertThat(response.id(), equalTo(expectedId));
  }

  @Test
  void shouldBeSpecializationOfSource() {
    var snapshot = randomSourceSnapshot();

    var response = SourceSnapshotResponse.create(snapshot, API_HOST);

    assertThat(response.specializationOf(), equalTo(snapshot.source()));
  }

  @Test
  void shouldBeGeneratedAtTimeSnapshotWasCreated() {
    var snapshot = randomSourceSnapshot();

    var response = SourceSnapshotResponse.create(snapshot, API_HOST);

    assertThat(response.generatedAtTime(), equalTo(snapshot.createdDate()));
  }

  @Test
  void shouldHaveMediaTypeOfContent() {
    var snapshot = randomSourceSnapshot();

    var response = SourceSnapshotResponse.create(snapshot, API_HOST);

    assertThat(response.mediaType(), equalTo(snapshot.content().type()));
  }

  @Test
  void shouldHaveSha256ChecksumOfContent() {
    var snapshot = randomSourceSnapshot();

    var response = SourceSnapshotResponse.create(snapshot, API_HOST);

    assertThat(response.checksum().algorithm(), equalTo(SHA256));
    assertThat(response.checksum().checksumValue(), equalTo(snapshot.content().hash()));
  }

  @Test
  void shouldBeGeneratedByIngestionThatUsedSource() {
    var snapshot = randomSourceSnapshot();

    var response = SourceSnapshotResponse.create(snapshot, API_HOST);

    assertThat(response.wasGeneratedBy().used(), equalTo(snapshot.source()));
  }

  @Test
  void shouldBeGeneratedByIngestionStartedBySourceChangedEvent() {
    var snapshot = randomSourceSnapshot();

    var response = SourceSnapshotResponse.create(snapshot, API_HOST);

    var event = response.wasGeneratedBy().wasStartedBy();
    assertThat(
        event.id(),
        equalTo(eventId(API_HOST, snapshot.approvalIdentifier(), snapshot.eventIdentifier())));
    assertThat(event.identifier(), equalTo(snapshot.eventIdentifier()));
  }

  @Test
  void shouldBeGeneratedByAgentActingForCustomerOfSourceChange() {
    var snapshot = randomSourceSnapshot();

    var response = SourceSnapshotResponse.create(snapshot, API_HOST);

    var customer = response.wasGeneratedBy().wasAssociatedWith().actedOnBehalfOf();
    assertThat(customer, equalTo(customerId(API_HOST, snapshot.customerIdentifier())));
  }

  @Test
  void shouldBeGeneratedByClientThatReportedTheSourceChange() {
    var snapshot = randomSourceSnapshot();

    var response = SourceSnapshotResponse.create(snapshot, API_HOST);

    var agent = response.wasGeneratedBy().wasAssociatedWith();
    assertThat(agent.id(), equalTo(clientId(API_HOST, snapshot.clientIdentifier())));
  }

  @Test
  void shouldNotNameClientWhenSnapshotWasStoredWithoutOne() {
    var stored = randomSourceSnapshot();
    var snapshot =
        new SourceSnapshot(
            stored.identifier(),
            stored.approvalIdentifier(),
            stored.customerIdentifier(),
            null,
            stored.eventIdentifier(),
            stored.source(),
            stored.content(),
            stored.createdDate());

    var response = SourceSnapshotResponse.create(snapshot, API_HOST);

    assertThat(response.wasGeneratedBy().wasAssociatedWith().id(), nullValue());
  }

  @Test
  void shouldRoundTripThroughJson() throws JsonProcessingException {
    var response = SourceSnapshotResponse.create(randomSourceSnapshot(), API_HOST);

    var json = JsonUtils.dtoObjectMapper.writeValueAsString(response);
    var deserialized = JsonUtils.dtoObjectMapper.readValue(json, SourceSnapshotResponse.class);

    assertThat(deserialized, equalTo(response));
  }
}
