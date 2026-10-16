package no.sikt.nva.approvals.rest;

import static no.sikt.nva.approvals.domain.ApprovalActivity.CREATE_APPROVAL;
import static no.sikt.nva.approvals.domain.ApprovalActivity.UPDATE_APPROVAL;
import static no.sikt.nva.approvals.utils.TestUtils.randomApproval;
import static no.sikt.nva.approvals.utils.TestUtils.randomHandle;
import static no.sikt.nva.approvals.utils.TestUtils.randomRevision;
import static no.sikt.nva.approvals.utils.UriConstants.approvalId;
import static no.sikt.nva.approvals.utils.UriConstants.changeId;
import static no.sikt.nva.approvals.utils.UriConstants.customerId;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.nullValue;

import com.fasterxml.jackson.core.JsonProcessingException;
import no.unit.nva.commons.json.JsonUtils;
import org.junit.jupiter.api.Test;

class ApprovalRevisionResponseTest {

  private static final String API_HOST = "api.unittest.nva.unit.no";

  @Test
  void shouldIdentifyRevisionUnderChangesOfApproval() {
    var revision = randomRevision(randomApproval(randomHandle()), CREATE_APPROVAL);

    var response = ApprovalRevisionResponse.create(revision, API_HOST);

    var expectedId = changeId(API_HOST, revision.approval().identifier(), revision.identifier());
    assertThat(response.id(), equalTo(expectedId));
  }

  @Test
  void shouldBeSpecializationOfApproval() {
    var revision = randomRevision(randomApproval(randomHandle()), CREATE_APPROVAL);

    var response = ApprovalRevisionResponse.create(revision, API_HOST);

    var expectedApproval = approvalId(API_HOST, revision.approval().identifier());
    assertThat(response.specializationOf(), equalTo(expectedApproval));
  }

  @Test
  void shouldBeGeneratedAtTimeRevisionWasCreated() {
    var revision = randomRevision(randomApproval(randomHandle()), CREATE_APPROVAL);

    var response = ApprovalRevisionResponse.create(revision, API_HOST);

    assertThat(response.generatedAtTime(), equalTo(revision.createdDate()));
  }

  @Test
  void shouldBeGeneratedByApprovalCreationWhenApprovalWasCreated() {
    var revision = randomRevision(randomApproval(randomHandle()), CREATE_APPROVAL);

    var response = ApprovalRevisionResponse.create(revision, API_HOST);

    assertThat(response.wasGeneratedBy(), instanceOf(ApprovalCreation.class));
  }

  @Test
  void shouldBeGeneratedByApprovalUpdateWhenApprovalWasUpdated() {
    var revision = randomRevision(randomApproval(randomHandle()), UPDATE_APPROVAL);

    var response = ApprovalRevisionResponse.create(revision, API_HOST);

    assertThat(response.wasGeneratedBy(), instanceOf(ApprovalUpdate.class));
  }

  @Test
  void shouldBeGeneratedByAgentActingForCustomerOwningApproval() {
    var revision = randomRevision(randomApproval(randomHandle()), CREATE_APPROVAL);

    var response = ApprovalRevisionResponse.create(revision, API_HOST);

    var customer = response.wasGeneratedBy().wasAssociatedWith().actedOnBehalfOf();
    var expectedCustomer = customerId(API_HOST, revision.approval().customerIdentifier());
    assertThat(customer, equalTo(expectedCustomer));
  }

  @Test
  void shouldReturnEmptySoftwareAgentWhenClientIdentifierIsMissing() {
    var revision = randomRevision(randomApproval(randomHandle()), CREATE_APPROVAL);

    var response = ApprovalRevisionResponse.create(revision, API_HOST);

    assertThat(response.wasGeneratedBy().wasAssociatedWith().id(), nullValue());
  }

  @Test
  void shouldRoundTripThroughJson() throws JsonProcessingException {
    var revision = randomRevision(randomApproval(randomHandle()), UPDATE_APPROVAL);
    var response = ApprovalRevisionResponse.create(revision, API_HOST);

    var json = JsonUtils.dtoObjectMapper.writeValueAsString(response);
    var deserialized = JsonUtils.dtoObjectMapper.readValue(json, ApprovalRevisionResponse.class);

    assertThat(deserialized, equalTo(response));
  }
}
