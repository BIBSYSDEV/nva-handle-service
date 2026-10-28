package no.sikt.nva.approvals.rest;

import static java.util.UUID.randomUUID;
import static no.sikt.nva.approvals.domain.ApprovalActivity.CREATE_APPROVAL;
import static no.sikt.nva.approvals.domain.ApprovalActivity.UPDATE_APPROVAL;
import static no.sikt.nva.approvals.utils.TestUtils.randomContent;
import static no.sikt.nva.approvals.utils.TestUtils.randomRevisionChange;
import static no.sikt.nva.approvals.utils.TestUtils.randomSourceSnapshot;
import static no.sikt.nva.approvals.utils.TestUtils.randomTimestamp;
import static no.sikt.nva.approvals.utils.UriConstants.changeId;
import static no.sikt.nva.approvals.utils.UriConstants.clientId;
import static no.sikt.nva.approvals.utils.UriConstants.customerId;
import static no.sikt.nva.approvals.utils.UriConstants.eventId;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;

import com.fasterxml.jackson.core.JsonProcessingException;
import no.sikt.nva.approvals.domain.ApprovalActivity;
import no.sikt.nva.approvals.domain.SourceChangedEventTrigger;
import no.sikt.nva.approvals.domain.SourceSnapshot;
import no.unit.nva.commons.json.JsonUtils;
import org.junit.jupiter.api.Test;

class SourceSnapshotResponseTest {

  private static final String API_HOST = "api.unittest.nva.unit.no";
  private static final String SHA256 = "sha256";

  private static SourceSnapshot snapshotTriggeredBy(ApprovalActivity activity) {
    return SourceSnapshot.create(
        randomRevisionChange(randomUUID(), randomTimestamp(), activity),
        randomContent(),
        randomTimestamp());
  }

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

    var ingestion = (SourceIngestion) response.wasGeneratedBy();
    assertThat(ingestion.used(), equalTo(snapshot.source()));
  }

  @Test
  void shouldBeGeneratedByIngestionStartedBySourceChangedEvent() {
    var snapshot = randomSourceSnapshot();

    var response = SourceSnapshotResponse.create(snapshot, API_HOST);

    var trigger = (SourceChangedEventTrigger) snapshot.activity();
    var event = ((SourceIngestion) response.wasGeneratedBy()).wasStartedBy();
    assertThat(
        event.id(),
        equalTo(eventId(API_HOST, snapshot.approvalIdentifier(), trigger.identifier())));
    assertThat(event.cloudEventIdentifier(), equalTo(trigger.cloudEventIdentifier()));
  }

  @Test
  void shouldBeGeneratedByAgentActingForCustomerOfSourceChange() {
    var snapshot = randomSourceSnapshot();

    var response = SourceSnapshotResponse.create(snapshot, API_HOST);

    var customer = response.wasGeneratedBy().wasAssociatedWith().actedOnBehalfOf();
    assertThat(customer, equalTo(customerId(API_HOST, snapshot.agent().customerIdentifier())));
  }

  @Test
  void shouldBeGeneratedByClientThatReportedTheSourceChange() {
    var snapshot = randomSourceSnapshot();

    var response = SourceSnapshotResponse.create(snapshot, API_HOST);

    var agent = response.wasGeneratedBy().wasAssociatedWith();
    assertThat(agent.id(), equalTo(clientId(API_HOST, snapshot.agent().clientIdentifier())));
  }

  @Test
  void shouldBeGeneratedByApprovalCreationWhenApprovalWasCreated() {
    var snapshot = snapshotTriggeredBy(CREATE_APPROVAL);

    var response = SourceSnapshotResponse.create(snapshot, API_HOST);

    assertThat(response.wasGeneratedBy(), instanceOf(ApprovalCreation.class));
  }

  @Test
  void shouldBeGeneratedByApprovalUpdateWhenApprovalWasUpdated() {
    var snapshot = snapshotTriggeredBy(UPDATE_APPROVAL);

    var response = SourceSnapshotResponse.create(snapshot, API_HOST);

    assertThat(response.wasGeneratedBy(), instanceOf(ApprovalUpdate.class));
  }

  @Test
  void shouldBeGeneratedByClientThatChangedTheApproval() {
    var snapshot = snapshotTriggeredBy(CREATE_APPROVAL);

    var response = SourceSnapshotResponse.create(snapshot, API_HOST);

    assertThat(
        response.wasGeneratedBy().wasAssociatedWith().id(),
        equalTo(clientId(API_HOST, snapshot.agent().clientIdentifier())));
  }

  @Test
  void shouldRoundTripThroughJson() throws JsonProcessingException {
    var response = SourceSnapshotResponse.create(randomSourceSnapshot(), API_HOST);

    var json = JsonUtils.dtoObjectMapper.writeValueAsString(response);
    var deserialized = JsonUtils.dtoObjectMapper.readValue(json, SourceSnapshotResponse.class);

    assertThat(deserialized, equalTo(response));
  }
}
