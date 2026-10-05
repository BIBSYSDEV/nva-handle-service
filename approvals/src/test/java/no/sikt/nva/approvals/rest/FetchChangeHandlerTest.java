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
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.UUID;
import no.sikt.nva.approvals.domain.ApprovalRevision;
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
  private static final String ACCEPT = "Accept";

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
            new DynamoDbChangeRepository(dynamoDbLocal.client(), ENVIRONMENT), ENVIRONMENT);
  }

  @AfterEach
  void tearDown() {
    dynamoDbLocal.cleanTable(TABLE);
  }

  @Test
  void shouldReturnApprovalRevisionWhenChangeIsRevisionOfApproval() throws IOException {
    var revision = saveRevision();

    var response =
        send(request(revision.approval().identifier(), revision.identifier().toString(), Map.of()));

    assertThat(response.getStatusCode(), equalTo(HTTP_OK));
    assertThat(
        response.getBodyObject(ChangeResponse.class), instanceOf(ApprovalRevisionResponse.class));
  }

  @Test
  void shouldReturnSourceSnapshotWhenChangeIsSnapshotOfApprovalSource() throws IOException {
    var snapshot = SourceSnapshot.create(randomSourceChange());
    approvalRepository.save(snapshot);

    var response =
        send(request(snapshot.approvalIdentifier(), snapshot.identifier().toString(), Map.of()));

    assertThat(
        response.getBodyObject(ChangeResponse.class), instanceOf(SourceSnapshotResponse.class));
  }

  @Test
  void shouldReturnNotFoundWhenChangeIsNotRecordedForApproval() throws IOException {
    var changeIdentifier = SortableIdentifier.next().toString();

    var response = sendForProblem(request(randomUUID(), changeIdentifier, Map.of()));

    assertThat(response.getStatusCode(), equalTo(HTTP_NOT_FOUND));
    assertThat(response.getBodyObject(Problem.class).getDetail(), containsString(changeIdentifier));
  }

  @Test
  void shouldReturnBadRequestWhenChangeIdentifierIsMalformed() throws IOException {
    var response = sendForProblem(request(randomUUID(), randomString(), Map.of()));

    assertThat(response.getStatusCode(), equalTo(HTTP_BAD_REQUEST));
  }

  @ParameterizedTest
  @ValueSource(strings = {"application/json", "application/ld+json"})
  void shouldReturnChangeWhenAcceptHeaderIsSupported(String mediaType) throws IOException {
    var revision = saveRevision();

    var response =
        send(
            request(
                revision.approval().identifier(),
                revision.identifier().toString(),
                Map.of(ACCEPT, mediaType)));

    assertThat(response.getStatusCode(), equalTo(HTTP_OK));
  }

  @Test
  void shouldReturnNotAcceptableWhenAcceptHeaderIsUnsupported() throws IOException {
    var response =
        sendForProblem(
            request(
                randomUUID(), SortableIdentifier.next().toString(), Map.of(ACCEPT, "text/html")));

    assertThat(response.getStatusCode(), equalTo(HTTP_NOT_ACCEPTABLE));
  }

  private ApprovalRevision saveRevision() {
    var revision = randomRevision(randomApproval(randomHandle()), CREATE_APPROVAL);
    approvalRepository.save(revision);
    return revision;
  }

  private GatewayResponse<ChangeResponse> send(InputStream request) throws IOException {
    handler.handleRequest(request, output, CONTEXT);
    return GatewayResponse.fromOutputStream(output, ChangeResponse.class);
  }

  private GatewayResponse<Problem> sendForProblem(InputStream request) throws IOException {
    handler.handleRequest(request, output, CONTEXT);
    return GatewayResponse.fromOutputStream(output, Problem.class);
  }

  private static InputStream request(
      UUID approvalIdentifier, String changeIdentifier, Map<String, String> headers)
      throws IOException {
    return new HandlerRequestBuilder<Void>(JsonUtils.dtoObjectMapper)
        .withPathParameters(
            Map.of("approvalId", approvalIdentifier.toString(), "changeId", changeIdentifier))
        .withHeaders(headers)
        .build();
  }
}
