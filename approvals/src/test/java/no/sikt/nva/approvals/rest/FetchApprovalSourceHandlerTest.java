package no.sikt.nva.approvals.rest;

import static java.net.HttpURLConnection.HTTP_NOT_ACCEPTABLE;
import static java.net.HttpURLConnection.HTTP_NOT_FOUND;
import static java.net.HttpURLConnection.HTTP_NOT_MODIFIED;
import static java.net.HttpURLConnection.HTTP_OK;
import static java.util.UUID.randomUUID;
import static no.sikt.nva.approvals.persistence.DynamoDbLocal.dynamoDBLocal;
import static no.sikt.nva.approvals.utils.TestUtils.randomSourceSnapshotWithContent;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.emptyOrNullString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.UUID;
import no.sikt.nva.approvals.domain.ChangeServiceImpl;
import no.sikt.nva.approvals.domain.Content;
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
import org.zalando.problem.Problem;

class FetchApprovalSourceHandlerTest {

  private static final Environment ENVIRONMENT = new Environment();
  private static final String TABLE = ENVIRONMENT.readEnv(DynamoDbConstants.TABLE);
  private static final FakeContext CONTEXT = new FakeContext();
  private static final String ACCEPT_HEADER = "Accept";
  private static final String CONTENT_TYPE_HEADER = "Content-Type";
  private static final String ETAG_HEADER = "ETag";
  private static final String IF_NONE_MATCH_HEADER = "If-None-Match";
  private static final String QUOTED = "\"%s\"";
  private static final String SOURCE_MEDIA_TYPE = "application/json";

  private DynamoDbLocal dynamoDbLocal;
  private DynamoDbApprovalRepository approvalRepository;
  private FetchApprovalSourceHandler handler;
  private ByteArrayOutputStream output;

  @BeforeEach
  void setUp() {
    dynamoDbLocal = dynamoDBLocal(TABLE);
    approvalRepository = new DynamoDbApprovalRepository(dynamoDbLocal.client(), ENVIRONMENT);
    output = new ByteArrayOutputStream();
    var changeService =
        new ChangeServiceImpl(
            approvalRepository, new DynamoDbChangeRepository(dynamoDbLocal.client(), ENVIRONMENT));
    handler = new FetchApprovalSourceHandler(changeService, ENVIRONMENT);
  }

  @AfterEach
  void tearDown() {
    dynamoDbLocal.cleanTable(TABLE);
  }

  @Test
  void shouldReturnSourceDocumentOfLatestSnapshot() throws IOException {
    var snapshot = saveSnapshot();

    var response = send(request(snapshot.approvalIdentifier(), Map.of()));

    assertThat(response.getStatusCode(), equalTo(HTTP_OK));
    assertThat(response.getBody(), equalTo(snapshot.content().body()));
  }

  @Test
  void shouldReturnStoredMediaTypeOfLatestSnapshot() throws IOException {
    var snapshot = saveSnapshot();

    var response = send(request(snapshot.approvalIdentifier(), Map.of()));

    assertThat(response.getHeaders().get(CONTENT_TYPE_HEADER), equalTo(SOURCE_MEDIA_TYPE));
  }

  @Test
  void shouldReturnEntityTagOfLatestSnapshot() throws IOException {
    var snapshot = saveSnapshot();

    var response = send(request(snapshot.approvalIdentifier(), Map.of()));

    assertThat(response.getHeaders().get(ETAG_HEADER), equalTo(quoted(snapshot.identifier())));
  }

  @Test
  void shouldReturnNotModifiedWithoutBodyWhenIfNoneMatchMatchesEntityTag() throws IOException {
    var snapshot = saveSnapshot();

    var response =
        send(
            request(
                snapshot.approvalIdentifier(),
                Map.of(IF_NONE_MATCH_HEADER, quoted(snapshot.identifier()))));

    assertThat(response.getStatusCode(), equalTo(HTTP_NOT_MODIFIED));
    assertThat(response.getBody(), is(emptyOrNullString()));
  }

  @Test
  void shouldReturnNotFoundWhenApprovalHasNoSourceSnapshot() throws IOException {
    var response = sendForProblem(request(randomUUID(), Map.of()));

    assertThat(response.getStatusCode(), equalTo(HTTP_NOT_FOUND));
  }

  @Test
  void shouldReturnNotAcceptableWhenAcceptHeaderIsUnsupported() throws IOException {
    var response = sendForProblem(request(randomUUID(), Map.of(ACCEPT_HEADER, "text/html")));

    assertThat(response.getStatusCode(), equalTo(HTTP_NOT_ACCEPTABLE));
  }

  private static String quoted(SortableIdentifier identifier) {
    return QUOTED.formatted(identifier);
  }

  private SourceSnapshot saveSnapshot() {
    var snapshot =
        randomSourceSnapshotWithContent(Content.create(SOURCE_MEDIA_TYPE, randomString()));
    approvalRepository.save(snapshot);
    return snapshot;
  }

  private GatewayResponse<String> send(InputStream request) throws IOException {
    handler.handleRequest(request, output, CONTEXT);
    return GatewayResponse.fromOutputStream(output, String.class);
  }

  private GatewayResponse<Problem> sendForProblem(InputStream request) throws IOException {
    handler.handleRequest(request, output, CONTEXT);
    return GatewayResponse.fromOutputStream(output, Problem.class);
  }

  private static InputStream request(UUID approvalIdentifier, Map<String, String> headers)
      throws IOException {
    return new HandlerRequestBuilder<Void>(JsonUtils.dtoObjectMapper)
        .withPathParameters(Map.of("approvalId", approvalIdentifier.toString()))
        .withHeaders(headers)
        .build();
  }
}
