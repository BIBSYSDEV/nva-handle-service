package no.sikt.nva.approvals.rest;

import static java.net.HttpURLConnection.HTTP_BAD_REQUEST;
import static java.net.HttpURLConnection.HTTP_NOT_ACCEPTABLE;
import static java.net.HttpURLConnection.HTTP_NOT_FOUND;
import static java.net.HttpURLConnection.HTTP_OK;
import static java.util.UUID.randomUUID;
import static no.sikt.nva.approvals.domain.ApprovalActivity.CREATE_APPROVAL;
import static no.sikt.nva.approvals.persistence.DynamoDbLocal.dynamoDBLocal;
import static no.sikt.nva.approvals.utils.TestUtils.randomApproval;
import static no.sikt.nva.approvals.utils.TestUtils.randomContent;
import static no.sikt.nva.approvals.utils.TestUtils.randomHandle;
import static no.sikt.nva.approvals.utils.TestUtils.randomRevision;
import static no.sikt.nva.approvals.utils.TestUtils.randomTimestamp;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.startsWith;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import no.sikt.nva.approvals.domain.ApprovalRevision;
import no.sikt.nva.approvals.domain.ChangeServiceImpl;
import no.sikt.nva.approvals.domain.SourceSnapshot;
import no.sikt.nva.approvals.persistence.DynamoDbApprovalRepository;
import no.sikt.nva.approvals.persistence.DynamoDbChangeRepository;
import no.sikt.nva.approvals.persistence.DynamoDbConstants;
import no.sikt.nva.approvals.persistence.DynamoDbLocal;
import no.sikt.nva.approvals.snapshot.SourceChange;
import no.unit.nva.commons.json.JsonUtils;
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

class FetchApprovalChangesHandlerTest {

  private static final Environment ENVIRONMENT = new Environment();
  private static final String TABLE = ENVIRONMENT.readEnv(DynamoDbConstants.TABLE);
  private static final FakeContext CONTEXT = new FakeContext();
  private static final String ACCEPT = "Accept";
  private static final String CONTENT_TYPE = "Content-Type";
  private static final String APPLICATION_JSON_LD = "application/ld+json";
  private static final String CURSOR = "cursor";
  private static final int PAGE_SIZE = 100;

  private DynamoDbLocal dynamoDbLocal;
  private DynamoDbApprovalRepository approvalRepository;
  private FetchApprovalChangesHandler handler;
  private ByteArrayOutputStream output;

  @BeforeEach
  void setUp() {
    dynamoDbLocal = dynamoDBLocal(TABLE);
    approvalRepository = new DynamoDbApprovalRepository(dynamoDbLocal.client(), ENVIRONMENT);
    output = new ByteArrayOutputStream();
    handler =
        new FetchApprovalChangesHandler(
            new ChangeServiceImpl(
                approvalRepository,
                new DynamoDbChangeRepository(dynamoDbLocal.client(), ENVIRONMENT)),
            ENVIRONMENT);
  }

  @AfterEach
  void tearDown() {
    dynamoDbLocal.cleanTable(TABLE);
  }

  @Test
  void shouldReturnChangesOfApproval() throws IOException {
    var revision = saveRevision();
    saveSnapshotOfSourceChangedAt(revision.approval().identifier(), randomTimestamp());

    var response = send(request(revision.approval().identifier(), Map.of(), Map.of()));

    assertThat(response.getStatusCode(), equalTo(HTTP_OK));
    assertThat(response.getBodyObject(ChangeListResponse.class).changes(), hasSize(2));
  }

  @Test
  void shouldNotReturnNextWhenAllChangesFitOnPage() throws IOException {
    var revision = saveRevision();

    var response = send(request(revision.approval().identifier(), Map.of(), Map.of()));

    assertThat(response.getBodyObject(ChangeListResponse.class).next(), nullValue());
  }

  @Test
  void shouldReturnNextWhenApprovalHasMoreChangesThanPageSize() throws IOException {
    var approvalIdentifier = saveRevision().approval().identifier();
    var start = randomTimestamp();
    saveSnapshotOfSourceChangedAt(approvalIdentifier, start.minus(Duration.ofDays(1)));
    IntStream.range(0, PAGE_SIZE - 1)
        .forEach(
            index -> saveSnapshotOfSourceChangedAt(approvalIdentifier, start.plusSeconds(index)));

    var response = send(request(approvalIdentifier, Map.of(), Map.of()));

    var next = response.getBodyObject(ChangeListResponse.class).next();
    assertThat(next.getQuery(), startsWith(CURSOR + "="));
  }

  @Test
  void shouldReturnEveryChangeExactlyOnceWhenFollowingNext() throws IOException {
    var approvalIdentifier = saveRevision().approval().identifier();
    IntStream.range(0, PAGE_SIZE)
        .forEach(index -> saveSnapshotOfSourceChangedAt(approvalIdentifier, randomTimestamp()));
    var firstPage =
        send(request(approvalIdentifier, Map.of(), Map.of()))
            .getBodyObject(ChangeListResponse.class);
    var cursor = firstPage.next().getQuery().replaceFirst(CURSOR + "=", "");

    output = new ByteArrayOutputStream();
    var secondPage =
        send(request(approvalIdentifier, Map.of(CURSOR, cursor), Map.of()))
            .getBodyObject(ChangeListResponse.class);

    var changes =
        Stream.concat(firstPage.changes().stream(), secondPage.changes().stream()).toList();
    assertThat(changes, hasSize(PAGE_SIZE + 1));
    assertThat(Set.copyOf(changes), hasSize(PAGE_SIZE + 1));
  }

  @Test
  void shouldReturnChangesAfterCursorWhenCursorIsChangeOfApproval() throws IOException {
    var revision = saveRevision();
    var cursor = Map.of(CURSOR, revision.identifier().toString());

    var response = send(request(revision.approval().identifier(), cursor, Map.of()));

    assertThat(response.getStatusCode(), equalTo(HTTP_OK));
  }

  @Test
  void shouldReturnNotFoundWhenApprovalDoesNotExist() throws IOException {
    var response = sendForProblem(request(randomUUID(), Map.of(), Map.of()));

    assertThat(response.getStatusCode(), equalTo(HTTP_NOT_FOUND));
  }

  @Test
  void shouldReturnBadRequestWhenCursorIsNotChangeOfApproval() throws IOException {
    var approvalIdentifier = saveRevision().approval().identifier();
    var changeOfOtherApproval = saveRevision().identifier().toString();

    var response =
        sendForProblem(
            request(approvalIdentifier, Map.of(CURSOR, changeOfOtherApproval), Map.of()));

    assertThat(response.getStatusCode(), equalTo(HTTP_BAD_REQUEST));
  }

  @Test
  void shouldReturnBadRequestWhenCursorIsNotChangeIdentifier() throws IOException {
    var approvalIdentifier = saveRevision().approval().identifier();

    var response =
        sendForProblem(request(approvalIdentifier, Map.of(CURSOR, randomString()), Map.of()));

    assertThat(response.getStatusCode(), equalTo(HTTP_BAD_REQUEST));
  }

  @Test
  void shouldReturnBadRequestWhenApprovalIdentifierIsNotUuid() throws IOException {
    var request =
        new HandlerRequestBuilder<Void>(JsonUtils.dtoObjectMapper)
            .withPathParameters(Map.of("approvalId", randomString()))
            .build();

    var response = sendForProblem(request);

    assertThat(response.getStatusCode(), equalTo(HTTP_BAD_REQUEST));
  }

  @Test
  void shouldReturnNotAcceptableWhenAcceptHeaderIsUnsupported() throws IOException {
    var response = sendForProblem(request(randomUUID(), Map.of(), Map.of(ACCEPT, "text/html")));

    assertThat(response.getStatusCode(), equalTo(HTTP_NOT_ACCEPTABLE));
  }

  @ParameterizedTest
  @ValueSource(strings = {"application/json", APPLICATION_JSON_LD})
  void shouldRespondWithRequestedMediaTypeWhenAcceptIsSupported(String mediaType)
      throws IOException {
    var revision = saveRevision();

    var response =
        send(request(revision.approval().identifier(), Map.of(), Map.of(ACCEPT, mediaType)));

    assertThat(response.getStatusCode(), equalTo(HTTP_OK));
    assertThat(response.getHeaders().get(CONTENT_TYPE), startsWith(mediaType));
  }

  @Test
  void shouldRespondWithJsonLdWhenAcceptIsMissing() throws IOException {
    var revision = saveRevision();

    var response = send(request(revision.approval().identifier(), Map.of(), Map.of()));

    assertThat(response.getHeaders().get(CONTENT_TYPE), startsWith(APPLICATION_JSON_LD));
  }

  private ApprovalRevision saveRevision() {
    var revision = randomRevision(randomApproval(randomHandle()), CREATE_APPROVAL);
    approvalRepository.save(revision);
    return revision;
  }

  private SourceSnapshot saveSnapshotOfSourceChangedAt(UUID approvalIdentifier, Instant timestamp) {
    var snapshot =
        SourceSnapshot.create(
            new SourceChange(
                approvalIdentifier, randomUUID(), randomString(), randomUri(), timestamp),
            randomContent(),
            timestamp);
    approvalRepository.save(snapshot);
    return snapshot;
  }

  private GatewayResponse<ChangeListResponse> send(InputStream request) throws IOException {
    handler.handleRequest(request, output, CONTEXT);
    return GatewayResponse.fromOutputStream(output, ChangeListResponse.class);
  }

  private GatewayResponse<Problem> sendForProblem(InputStream request) throws IOException {
    handler.handleRequest(request, output, CONTEXT);
    return GatewayResponse.fromOutputStream(output, Problem.class);
  }

  private static InputStream request(
      UUID approvalIdentifier, Map<String, String> queryParameters, Map<String, String> headers)
      throws IOException {
    return new HandlerRequestBuilder<Void>(JsonUtils.dtoObjectMapper)
        .withPathParameters(Map.of("approvalId", approvalIdentifier.toString()))
        .withQueryParameters(queryParameters)
        .withHeaders(headers)
        .build();
  }
}
