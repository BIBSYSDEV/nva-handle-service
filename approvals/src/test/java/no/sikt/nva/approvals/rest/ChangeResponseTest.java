package no.sikt.nva.approvals.rest;

import static no.sikt.nva.approvals.domain.ApprovalActivity.CREATE_APPROVAL;
import static no.sikt.nva.approvals.domain.ApprovalActivity.UPDATE_APPROVAL;
import static no.sikt.nva.approvals.utils.TestUtils.randomApproval;
import static no.sikt.nva.approvals.utils.TestUtils.randomHandle;
import static no.sikt.nva.approvals.utils.TestUtils.randomSourceSnapshot;
import static no.unit.nva.testutils.RandomDataGenerator.randomInstant;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import com.fasterxml.jackson.core.JsonProcessingException;
import java.net.URI;
import no.sikt.nva.approvals.domain.ApprovalActivity;
import no.sikt.nva.approvals.domain.ApprovalRevision;
import no.sikt.nva.approvals.domain.SourceSnapshot;
import no.unit.nva.commons.json.JsonUtils;
import org.junit.jupiter.api.Test;

class ChangeResponseTest {

  private static final String API_HOST = "api.unittest.nva.unit.no";
  private static final URI CONTEXT =
      URI.create("https://api.unittest.nva.unit.no/approval/context/v1");
  private static final String CONTEXT_KEYWORD = "@context";
  private static final String UPDATE_APPROVAL_ACTIVITY_JSON = "{\"type\":\"UpdateApproval\"}";
  private static final String CREATE_APPROVAL_ACTIVITY_JSON = "{\"type\":\"CreateApproval\"}";
  private static final String HARVEST_SOURCE_JSON =
      """
      {"type": "HarvestSource", "trigger": "SourceChangeEvent", "eventId": "%s"}
      """;

  @Test
  void shouldDescribeCreatedRevisionAsGeneratedByCreateApproval() {
    var response = revisionResponse(revision(CREATE_APPROVAL));

    assertThat(response.wasGeneratedBy(), equalTo(new CreateApprovalActivity()));
  }

  @Test
  void shouldDescribeUpdatedRevisionAsGeneratedByUpdateApproval() {
    var response = revisionResponse(revision(UPDATE_APPROVAL));

    assertThat(response.wasGeneratedBy(), equalTo(new UpdateApprovalActivity()));
  }

  @Test
  void shouldSerializeActivityNameAsTypeOfGeneratingActivity() throws JsonProcessingException {
    var response = revisionResponse(revision(UPDATE_APPROVAL));

    var serialized = JsonUtils.dtoObjectMapper.writeValueAsString(response.wasGeneratedBy());

    assertThat(
        JsonUtils.dtoObjectMapper.readTree(serialized),
        equalTo(JsonUtils.dtoObjectMapper.readTree(UPDATE_APPROVAL_ACTIVITY_JSON)));
  }

  @Test
  void shouldSerializeCreateApprovalAsTypeOfGeneratingActivity() throws JsonProcessingException {
    var response = revisionResponse(revision(CREATE_APPROVAL));

    var serialized = JsonUtils.dtoObjectMapper.writeValueAsString(response.wasGeneratedBy());

    assertThat(
        JsonUtils.dtoObjectMapper.readTree(serialized),
        equalTo(JsonUtils.dtoObjectMapper.readTree(CREATE_APPROVAL_ACTIVITY_JSON)));
  }

  @Test
  void shouldSerializeHarvestOfSourceWithTriggerAndEventId() throws JsonProcessingException {
    var eventId = randomString();

    var serialized = JsonUtils.dtoObjectMapper.writeValueAsString(HarvestSource.create(eventId));

    assertThat(
        JsonUtils.dtoObjectMapper.readTree(serialized),
        equalTo(JsonUtils.dtoObjectMapper.readTree(HARVEST_SOURCE_JSON.formatted(eventId))));
  }

  @Test
  void shouldDescribeRevisionAsGeneratedAtItsCreatedDate() {
    var revision = revision(UPDATE_APPROVAL);

    var response = revisionResponse(revision);

    assertThat(response.generatedAtTime(), equalTo(revision.createdDate()));
  }

  @Test
  void shouldPointRevisionToContextOnApiHost() {
    var response = revisionResponse(revision(UPDATE_APPROVAL));

    assertThat(response.context(), equalTo(CONTEXT));
  }

  @Test
  void shouldDescribeSnapshotAsGeneratedByHarvestOfSourceChangeEvent() {
    var snapshot = randomSourceSnapshot();

    var response = snapshotResponse(snapshot);

    assertThat(
        response.wasGeneratedBy(),
        equalTo(new HarvestSource("SourceChangeEvent", snapshot.eventIdentifier())));
  }

  @Test
  void shouldDescribeSnapshotAsRetrievedAtItsCreatedDate() {
    var snapshot = randomSourceSnapshot();

    var response = snapshotResponse(snapshot);

    assertThat(response.retrievedAt(), equalTo(snapshot.createdDate()));
  }

  @Test
  void shouldDescribeSourceAndContentTypeOfSnapshot() {
    var snapshot = randomSourceSnapshot();

    var response = snapshotResponse(snapshot);

    assertThat(response.source(), equalTo(snapshot.source()));
    assertThat(response.contentType(), equalTo(snapshot.content().type()));
  }

  @Test
  void shouldPrefixContentHashOfSnapshotWithItsAlgorithm() {
    var snapshot = randomSourceSnapshot();

    var response = snapshotResponse(snapshot);

    assertThat(response.contentHash(), equalTo("sha256:" + snapshot.content().hash()));
  }

  @Test
  void shouldPointSnapshotToContextOnApiHost() {
    var response = snapshotResponse(randomSourceSnapshot());

    assertThat(response.context(), equalTo(CONTEXT));
  }

  @Test
  void shouldSerializeContextOfRevisionAsJsonLdKeyword() throws JsonProcessingException {
    var response = ChangeResponse.fromChange(revision(UPDATE_APPROVAL), API_HOST);

    var serialized = JsonUtils.dtoObjectMapper.writeValueAsString(response);

    assertThat(JsonUtils.dtoObjectMapper.readTree(serialized).has(CONTEXT_KEYWORD), equalTo(true));
  }

  @Test
  void shouldSerializeContextOfSnapshotAsJsonLdKeyword() throws JsonProcessingException {
    var response = ChangeResponse.fromChange(randomSourceSnapshot(), API_HOST);

    var serialized = JsonUtils.dtoObjectMapper.writeValueAsString(response);

    assertThat(JsonUtils.dtoObjectMapper.readTree(serialized).has(CONTEXT_KEYWORD), equalTo(true));
  }

  private static ApprovalRevision revision(ApprovalActivity activity) {
    return ApprovalRevision.create(
        randomApproval(randomHandle()), activity, randomUri(), randomUri(), randomInstant());
  }

  private static ApprovalRevisionResponse revisionResponse(ApprovalRevision revision) {
    return (ApprovalRevisionResponse) ChangeResponse.fromChange(revision, API_HOST);
  }

  private static SourceSnapshotResponse snapshotResponse(SourceSnapshot snapshot) {
    return (SourceSnapshotResponse) ChangeResponse.fromChange(snapshot, API_HOST);
  }
}
