package no.sikt.nva.approvals.rest;

import static java.net.HttpURLConnection.HTTP_BAD_REQUEST;
import static java.net.HttpURLConnection.HTTP_NOT_ACCEPTABLE;
import static java.net.HttpURLConnection.HTTP_NOT_FOUND;
import static java.net.HttpURLConnection.HTTP_OK;
import static java.util.UUID.randomUUID;
import static no.sikt.nva.approvals.domain.ApprovalActivity.CREATE_APPROVAL;
import static no.sikt.nva.approvals.persistence.DynamoDbLocal.dynamoDBLocal;
import static no.sikt.nva.approvals.utils.TestUtils.randomApproval;
import static no.sikt.nva.approvals.utils.TestUtils.randomHandle;
import static no.sikt.nva.approvals.utils.TestUtils.randomRevision;
import static no.sikt.nva.approvals.utils.TestUtils.randomSourceChange;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Map;
import java.util.UUID;
import no.sikt.nva.approvals.domain.Approval;
import no.sikt.nva.approvals.domain.SourceSnapshot;
import no.sikt.nva.approvals.persistence.DynamoDbApprovalRepository;
import no.sikt.nva.approvals.persistence.DynamoDbChangeRepository;
import no.sikt.nva.approvals.persistence.DynamoDbConstants;
import no.sikt.nva.approvals.persistence.DynamoDbLocal;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.stubs.FakeContext;
import no.unit.nva.testutils.HandlerRequestBuilder;
import nva.commons.apigateway.GatewayResponse;
import nva.commons.core.Environment;
import nva.commons.core.paths.UriWrapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.zalando.problem.Problem;

class FetchChangeHandlerTest {

  private static final Environment ENVIRONMENT = new Environment();
  private static final String TABLE = ENVIRONMENT.readEnv(DynamoDbConstants.TABLE);
  private static final FakeContext CONTEXT = new FakeContext();
  private static final String BACKEND_SCOPE = "https://api.nva.unit.no/scopes/backend";
  private static final String THIRD_PARTY_SCOPE =
      "https://api.nva.unit.no/scopes/third-party/approval-upsert";

  private DynamoDbLocal dynamoDbLocal;
  private DynamoDbApprovalRepository approvalRepository;
  private FetchChangeHandler handler;
  private ByteArrayOutputStream output;

  @BeforeEach
  void setUp() {
    dynamoDbLocal = dynamoDBLocal(TABLE);
    approvalRepository = new DynamoDbApprovalRepository(dynamoDbLocal.client(), ENVIRONMENT);
    output = new ByteArrayOutputStream();
    handler =
        new FetchChangeHandler(
            approvalRepository,
            new DynamoDbChangeRepository(dynamoDbLocal.client(), ENVIRONMENT),
            ENVIRONMENT);
  }

  @AfterEach
  void tearDown() {
    dynamoDbLocal.cleanTable(TABLE);
  }

  @Test
  void shouldReturnApprovalRevisionWhenChangeIsRevisionOfApproval() throws IOException {
    var approval = randomApproval(randomHandle());
    var revision = randomRevision(approval, CREATE_APPROVAL);
    approvalRepository.save(revision);

    var response = send(backendRequest(approval.identifier(), revision.identifier().toString()));

    assertThat(response.getStatusCode(), equalTo(HTTP_OK));
    assertThat(
        response.getBodyObject(ChangeResponse.class), instanceOf(ApprovalRevisionResponse.class));
  }

  @Test
  void shouldReturnSourceSnapshotWhenChangeIsSnapshotOfApprovalSource() throws IOException {
    var approval = saveApproval();
    var snapshot = SourceSnapshot.create(randomSourceChange(approval.identifier(), randomString()));
    approvalRepository.save(snapshot);

    var response = send(backendRequest(approval.identifier(), snapshot.identifier().toString()));

    assertThat(
        response.getBodyObject(ChangeResponse.class), instanceOf(SourceSnapshotResponse.class));
  }

  @Test
  void shouldReturnNotFoundWhenChangeIsNotRecordedForApproval() throws IOException {
    var approval = saveApproval();
    var changeIdentifier = SortableIdentifier.next().toString();

    var response = sendForProblem(backendRequest(approval.identifier(), changeIdentifier));

    assertThat(response.getStatusCode(), equalTo(HTTP_NOT_FOUND));
    assertThat(response.getBodyObject(Problem.class).getDetail(), containsString(changeIdentifier));
  }

  @Test
  void shouldReturnChangeWhenCustomerOwnsApproval() throws IOException {
    var customerIdentifier = randomUUID();
    var approval = randomApproval(randomUri(), randomHandle(), customerIdentifier);
    var revision = randomRevision(approval, CREATE_APPROVAL);
    approvalRepository.save(revision);

    var response =
        send(customerRequest(approval.identifier(), revision.identifier(), customerIdentifier));

    assertThat(response.getStatusCode(), equalTo(HTTP_OK));
  }

  @Test
  void shouldReturnNotFoundWhenApprovalBelongsToOtherCustomer() throws IOException {
    var approval = randomApproval(randomUri(), randomHandle(), randomUUID());
    var revision = randomRevision(approval, CREATE_APPROVAL);
    approvalRepository.save(revision);

    var response =
        send(customerRequest(approval.identifier(), revision.identifier(), randomUUID()));

    assertThat(response.getStatusCode(), equalTo(HTTP_NOT_FOUND));
  }

  @Test
  void shouldReturnNotFoundWhenApprovalDoesNotExist() throws IOException {
    var response = send(backendRequest(randomUUID(), SortableIdentifier.next().toString()));

    assertThat(response.getStatusCode(), equalTo(HTTP_NOT_FOUND));
  }

  @Test
  void shouldReturnBadRequestWhenChangeIdentifierIsMalformed() throws IOException {
    var response = send(backendRequest(randomUUID(), randomString()));

    assertThat(response.getStatusCode(), equalTo(HTTP_BAD_REQUEST));
  }

  @ParameterizedTest
  @ValueSource(strings = {"application/json", "application/ld+json"})
  void shouldReturnChangeWhenAcceptIsSupported(String mediaType) throws IOException {
    var approval = randomApproval(randomHandle());
    var revision = randomRevision(approval, CREATE_APPROVAL);
    approvalRepository.save(revision);

    var response =
        send(backendRequest(approval.identifier(), revision.identifier().toString(), mediaType));

    assertThat(response.getStatusCode(), equalTo(HTTP_OK));
  }

  @Test
  void shouldReturnNotAcceptableWhenAcceptIsUnsupported() throws IOException {
    var response =
        send(backendRequest(randomUUID(), SortableIdentifier.next().toString(), "text/html"));

    assertThat(response.getStatusCode(), equalTo(HTTP_NOT_ACCEPTABLE));
  }

  private Approval saveApproval() {
    var approval = randomApproval(randomHandle());
    approvalRepository.save(randomRevision(approval, CREATE_APPROVAL));
    return approval;
  }

  private GatewayResponse<ChangeResponse> send(InputStream request) throws IOException {
    handler.handleRequest(request, output, CONTEXT);
    return GatewayResponse.fromOutputStream(output, ChangeResponse.class);
  }

  private GatewayResponse<Problem> sendForProblem(InputStream request) throws IOException {
    handler.handleRequest(request, output, CONTEXT);
    return GatewayResponse.fromOutputStream(output, Problem.class);
  }

  private static InputStream backendRequest(UUID approvalIdentifier, String changeIdentifier)
      throws IOException {
    return new HandlerRequestBuilder<Void>(JsonUtils.dtoObjectMapper)
        .withPathParameters(pathParameters(approvalIdentifier, changeIdentifier))
        .withScope(BACKEND_SCOPE)
        .build();
  }

  private static InputStream backendRequest(
      UUID approvalIdentifier, String changeIdentifier, String accept) throws IOException {
    return new HandlerRequestBuilder<Void>(JsonUtils.dtoObjectMapper)
        .withPathParameters(pathParameters(approvalIdentifier, changeIdentifier))
        .withScope(BACKEND_SCOPE)
        .withHeaders(Map.of("Accept", accept))
        .build();
  }

  private static InputStream customerRequest(
      UUID approvalIdentifier, SortableIdentifier changeIdentifier, UUID customerIdentifier)
      throws IOException {
    return new HandlerRequestBuilder<Void>(JsonUtils.dtoObjectMapper)
        .withPathParameters(pathParameters(approvalIdentifier, changeIdentifier.toString()))
        .withScope(THIRD_PARTY_SCOPE)
        .withCurrentCustomer(customerUri(customerIdentifier))
        .build();
  }

  private static Map<String, String> pathParameters(
      UUID approvalIdentifier, String changeIdentifier) {
    return Map.of("approvalId", approvalIdentifier.toString(), "changeId", changeIdentifier);
  }

  private static URI customerUri(UUID customerIdentifier) {
    return UriWrapper.fromUri(randomUri()).addChild(customerIdentifier.toString()).getUri();
  }
}
