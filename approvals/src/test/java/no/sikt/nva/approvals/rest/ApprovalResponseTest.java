package no.sikt.nva.approvals.rest;

import static no.sikt.nva.approvals.utils.TestUtils.randomApproval;
import static no.sikt.nva.approvals.utils.TestUtils.randomHandle;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.core.JsonProcessingException;
import java.net.URI;
import java.time.Instant;
import java.util.List;
import no.sikt.nva.approvals.domain.ApprovalSourceView;
import no.sikt.nva.approvals.domain.ClinicalTrialView;
import no.unit.nva.commons.json.JsonUtils;
import org.junit.jupiter.api.Test;

class ApprovalResponseTest {

  private static final String API_HOST = "api.unittest.nva.unit.no";
  private static final Instant VIEW_MODIFIED = Instant.parse("2026-11-20T10:00:00Z");
  private static final Instant SOURCE_MODIFIED = Instant.parse("2026-11-19T10:00:00Z");
  private static final String TRIAL_TITLE = "A trial";
  private static final String CLINICAL_TRIAL_TYPE = "ClinicalTrialView";

  @Test
  void shouldSerializeSourceViewWithItsTypeAndModifiedDates() throws JsonProcessingException {
    var view =
        new ClinicalTrialView(null, TRIAL_TITLE, null, null, List.of(), List.of(), List.of());
    var sourceView = new ApprovalSourceView(view, VIEW_MODIFIED, SOURCE_MODIFIED);
    var response =
        ApprovalResponse.fromApproval(randomApproval(randomHandle()), sourceView, API_HOST);

    var json =
        JsonUtils.dtoObjectMapper.readTree(JsonUtils.dtoObjectMapper.writeValueAsString(response));

    assertEquals(CLINICAL_TRIAL_TYPE, json.at("/sourceView/type").asText());
    assertEquals(TRIAL_TITLE, json.at("/sourceView/title").asText());
    assertEquals(VIEW_MODIFIED.toString(), json.get("sourceViewModified").asText());
    assertEquals(SOURCE_MODIFIED.toString(), json.get("sourceModified").asText());
  }

  @Test
  void shouldConvertApprovalToResponse() {
    var handle = randomHandle();
    var approval = randomApproval(handle);

    var response = ApprovalResponse.fromApproval(approval, ApprovalSourceView.NONE, API_HOST);

    assertEquals(approval.identifier(), response.identifier());
    assertEquals(approval.namedIdentifiers(), response.identifiers());
    assertEquals(approval.source(), response.source());
    assertEquals(handle.value().toString(), response.handle());
  }

  @Test
  void shouldGenerateIdFromApiHostAndIdentifier() {
    var handle = randomHandle();
    var approval = randomApproval(handle);

    var response = ApprovalResponse.fromApproval(approval, ApprovalSourceView.NONE, API_HOST);

    var expectedId = URI.create("https://" + API_HOST + "/approval/" + approval.identifier());
    assertEquals(expectedId, response.id());
  }

  @Test
  void shouldReferenceVersionedContext() {
    var approval = randomApproval(randomHandle());

    var response = ApprovalResponse.fromApproval(approval, ApprovalSourceView.NONE, API_HOST);

    var expectedContext = URI.create("https://%s/approval/context/v1".formatted(API_HOST));
    assertEquals(expectedContext, response.context());
  }

  @Test
  void shouldSerializeHandleAsString() throws JsonProcessingException {
    var handle = randomHandle();
    var approval = randomApproval(handle);
    var response = ApprovalResponse.fromApproval(approval, ApprovalSourceView.NONE, API_HOST);

    var json = JsonUtils.dtoObjectMapper.writeValueAsString(response);
    var jsonNode = JsonUtils.dtoObjectMapper.readTree(json);

    assertTrue(jsonNode.has("handle"));
    assertInstanceOf(String.class, jsonNode.get("handle").textValue());
    assertEquals(handle.value().toString(), jsonNode.get("handle").textValue());
  }

  @Test
  void shouldRoundTripThroughJson() throws JsonProcessingException {
    var handle = randomHandle();
    var approval = randomApproval(handle);
    var response = ApprovalResponse.fromApproval(approval, ApprovalSourceView.NONE, API_HOST);

    var json = JsonUtils.dtoObjectMapper.writeValueAsString(response);
    var deserialized = JsonUtils.dtoObjectMapper.readValue(json, ApprovalResponse.class);

    assertEquals(response, deserialized);
  }
}
