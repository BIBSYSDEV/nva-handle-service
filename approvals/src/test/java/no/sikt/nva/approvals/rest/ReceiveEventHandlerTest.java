package no.sikt.nva.approvals.rest;

import static java.net.HttpURLConnection.HTTP_ACCEPTED;
import static java.net.HttpURLConnection.HTTP_BAD_GATEWAY;
import static java.net.HttpURLConnection.HTTP_BAD_REQUEST;
import static java.net.HttpURLConnection.HTTP_FORBIDDEN;
import static java.net.HttpURLConnection.HTTP_NOT_FOUND;
import static java.net.HttpURLConnection.HTTP_UNAUTHORIZED;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.UUID.randomUUID;
import static no.sikt.nva.approvals.events.CloudEvent.SOURCE_CHANGED_TYPE;
import static no.sikt.nva.approvals.persistence.DynamoDbConstants.PK0;
import static no.sikt.nva.approvals.persistence.DynamoDbConstants.SK0;
import static no.sikt.nva.approvals.persistence.DynamoDbLocal.dynamoDBLocal;
import static no.sikt.nva.approvals.rest.ApprovalScopes.APPROVAL_UPSERT_SCOPE;
import static no.sikt.nva.approvals.rest.ApprovalScopes.BACKEND_SCOPE;
import static no.sikt.nva.approvals.utils.TestUtils.randomApproval;
import static no.sikt.nva.approvals.utils.TestUtils.randomHandle;
import static no.sikt.nva.approvals.utils.TestUtils.randomIdentifiers;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;
import no.sikt.nva.approvals.domain.Approval;
import no.sikt.nva.approvals.events.ApprovalEventService;
import no.sikt.nva.approvals.events.CloudEvent;
import no.sikt.nva.approvals.events.FakePullQueue;
import no.sikt.nva.approvals.events.PullQueue;
import no.sikt.nva.approvals.events.PullRequest;
import no.sikt.nva.approvals.events.SourceSystem;
import no.sikt.nva.approvals.events.SourceSystemRegister;
import no.sikt.nva.approvals.persistence.ApprovalRepository;
import no.sikt.nva.approvals.persistence.DynamoDbApprovalRepository;
import no.sikt.nva.approvals.persistence.DynamoDbLocal;
import no.sikt.nva.approvals.persistence.EventDao;
import no.sikt.nva.approvals.persistence.PendingDao;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.stubs.FakeContext;
import no.unit.nva.testutils.HandlerRequestBuilder;
import nva.commons.apigateway.GatewayResponse;
import nva.commons.core.Environment;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullSource;
import org.zalando.problem.Problem;
import software.amazon.awssdk.enhanced.dynamodb.document.EnhancedDocument;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;

class ReceiveEventHandlerTest {

  private static final Context CONTEXT = new FakeContext();
  private static final Environment ENVIRONMENT = new Environment();
  private static final String TABLE_ENVIRONMENT_VARIABLE_NAME = "TABLE";
  private static final String TABLE = ENVIRONMENT.readEnv(TABLE_ENVIRONMENT_VARIABLE_NAME);
  private static final String SPEC_VERSION = "1.0";
  private static final String CLIENT_ID = "rek-client";
  private static final URI CUSTOMER_ID =
      URI.create("https://api.nva.unit.no/customer/").resolve(randomUUID().toString());
  private static final URI REGISTERED_BASE_URI = URI.create("https://rek.example.com/approvals/");
  private static final String OTHER_THIRD_PARTY_SCOPE =
      "https://api.nva.unit.no/scopes/third-party/publication-read";
  private static final String HASH_ALGORITHM = "SHA-256";
  private static final String EVENT_KEY_SEPARATOR = "|";
  private static final String LOCATION_HEADER = "Location";
  private static final String UNSUPPORTED_SPEC_VERSION_MESSAGE =
      "Unsupported specversion %s, only 1.0 is supported";
  private static final String UNSUPPORTED_TYPE_MESSAGE =
      "Unsupported event type %s, only no.sikt.nva.approval.source.changed is supported";
  private static final String INVALID_EVENT_ID_MESSAGE =
      "Event id must match ^[A-Za-z0-9_=-]{1,128}$";
  private static final String SOURCE_MANDATORY_MESSAGE = "Event source is mandatory";
  private static final String SUBJECT_NOT_HANDLE_MESSAGE =
      "Event subject must be the handle of an approval, but was %s";
  private static final String DATA_NOT_ALLOWED_MESSAGE = "Event must not carry data or data_base64";
  private static final String MISSING_BODY_MESSAGE = "Request body must be a CloudEvent";
  private static final String SOURCE_MISMATCH_MESSAGE =
      "Event source %s does not match the source of approval %s";
  private static final String UNREGISTERED_SOURCE_MESSAGE =
      "Event source %s does not belong to a registered source system";
  private static final String APPROVAL_NOT_FOUND_MESSAGE = "Approval not found for handle %s";
  private static final String MISSING_CLIENT_ID_MESSAGE = "Client id is missing from the token";
  private static final String MISSING_SCOPE_MESSAGE =
      "Client is missing a scope that allows sending approval events";
  private static final String BAD_GATEWAY_MESSAGE = "Something went wrong!";
  private static final String MALFORMED_JSON_BODY = "{\"specversion\": ";
  private static final String DATA_FIELD_VALUE = "changed";
  private static final int MAX_EVENT_ID_LENGTH = 128;
  private static final String VALID_ID_CHARACTER = "a";
  private static final String SCOPE_SEPARATOR = " ";
  private static final List<String> MALFORMED_EVENT_IDS =
      List.of("contains space", "contains/slash", "contains|pipe", "contains.dot", "ikke-æøå");
  private static final List<String> WELL_FORMED_EVENT_IDS =
      List.of("Az09_=-", "dGhpcyBpcyBhbiBpZA==");
  private static final List<String> UNSUPPORTED_TYPES = List.of("no.sikt.nva.approval.created");
  private static final List<String> UNSUPPORTED_SPEC_VERSIONS = List.of("0.3", "1", "2.0");
  private static final List<URI> SUBJECTS_THAT_ARE_NOT_HANDLES =
      List.of(
          URI.create("https://example.com/11250.1/12345"),
          URI.create("https://hdl.handle.net/11250.1"),
          URI.create("urn:uuid:" + randomUUID()));

  private ByteArrayOutputStream output;
  private DynamoDbLocal dynamoDbLocal;
  private ApprovalRepository approvalRepository;
  private FakePullQueue pullQueue;
  private ReceiveEventHandler handler;
  private Approval approval;

  @BeforeEach
  void setUp() {
    output = new ByteArrayOutputStream();
    dynamoDbLocal = dynamoDBLocal(TABLE);
    approvalRepository = new DynamoDbApprovalRepository(dynamoDbLocal.client(), ENVIRONMENT);
    pullQueue = new FakePullQueue();
    handler = handlerWithPullQueue(pullQueue);
    approval = persistApprovalWithSource(registeredSource());
  }

  @AfterEach
  void tearDown() {
    dynamoDbLocal.cleanTable(TABLE);
  }

  @Test
  void shouldReturnAcceptedForValidNotification() throws IOException {
    var response = send(validEvent(approval));

    assertEquals(HTTP_ACCEPTED, response.getStatusCode());
  }

  @Test
  void shouldEnqueueOnePullForTheNotifiedApproval() throws IOException {
    var cloudEvent = validEvent(approval);

    send(cloudEvent);

    var expectedPull = new PullRequest(approval.identifier(), expectedEventKey(cloudEvent));
    assertEquals(List.of(expectedPull), pullQueue.getPullRequests());
  }

  @Test
  void shouldReturnAcceptedWithoutSecondPullWhenSameSourceAndIdIsReceivedAgain()
      throws IOException {
    var cloudEvent = validEvent(approval);
    send(cloudEvent);
    output = new ByteArrayOutputStream();

    var response = send(cloudEvent);

    assertEquals(HTTP_ACCEPTED, response.getStatusCode());
    assertEquals(1, pullQueue.getPullRequests().size());
  }

  @Test
  void shouldNotEnqueueSecondPullWhenRepeatedNotificationDiffersInOtherAttributes()
      throws IOException {
    var cloudEvent = validEvent(approval);
    send(cloudEvent);
    output = new ByteArrayOutputStream();

    send(withTime(cloudEvent, Instant.now().plusSeconds(3600)));

    assertEquals(1, pullQueue.getPullRequests().size());
  }

  @Test
  void shouldEnqueueNewPullWhenNotificationHasNewId() throws IOException {
    send(validEvent(approval));
    output = new ByteArrayOutputStream();

    var response = send(validEvent(approval));

    assertEquals(HTTP_ACCEPTED, response.getStatusCode());
    assertEquals(2, pullQueue.getPullRequests().size());
  }

  @Test
  void shouldStoreEnvelopeWithReceiverMetadata() throws IOException {
    var cloudEvent = validEvent(approval);
    var before = Instant.now();

    send(cloudEvent);

    var storedEvent = fetchEvent(expectedEventKey(cloudEvent));
    assertEquals(cloudEvent, storedEvent.envelope());
    assertEquals(approval.identifier(), storedEvent.approvalIdentifier());
    assertEquals(CLIENT_ID, storedEvent.clientId());
    assertEquals(CUSTOMER_ID, storedEvent.customerId());
    assertFalse(storedEvent.receivedAt().isBefore(before));
  }

  @Test
  void shouldStorePendingMarkerForEnqueuedPull() throws IOException {
    var cloudEvent = validEvent(approval);

    send(cloudEvent);

    var pendingMarker = fetchPendingMarker(expectedEventKey(cloudEvent));
    assertEquals(
        new PendingDao(expectedEventKey(cloudEvent), approval.identifier()), pendingMarker);
  }

  @Test
  void shouldNeverUseEventIdOfThirdPartyAsKey() throws IOException {
    var cloudEvent = validEvent(approval);

    send(cloudEvent);

    var eventKey = pullQueue.getPullRequests().getFirst().eventKey();
    assertFalse(eventKey.contains(cloudEvent.id()));
    assertNotNull(fetchItem(EventDao.toDatabaseIdentifier(eventKey)));
  }

  @Test
  void shouldNotExposeEventIdInResponseHeaders() throws IOException {
    var response = send(validEvent(approval));

    assertFalse(response.getHeaders().containsKey(LOCATION_HEADER));
  }

  @Test
  void shouldReturnAcceptedForClientWithBackendScope() throws IOException {
    var response = sendWithScope(validEvent(approval), BACKEND_SCOPE);

    assertEquals(HTTP_ACCEPTED, response.getStatusCode());
    assertEquals(1, pullQueue.getPullRequests().size());
  }

  @Test
  void shouldReturnAcceptedWhenScopeClaimHoldsSeveralScopes() throws IOException {
    var scopes = String.join(SCOPE_SEPARATOR, OTHER_THIRD_PARTY_SCOPE, APPROVAL_UPSERT_SCOPE);

    var response = sendWithScope(validEvent(approval), scopes);

    assertEquals(HTTP_ACCEPTED, response.getStatusCode());
  }

  @Test
  void shouldReturnNotFoundWhenNoApprovalHasTheHandle() throws IOException {
    var unknownHandle = randomHandle();
    var cloudEvent = event(randomEventId(), approval.source(), unknownHandle.value());

    var response = send(cloudEvent);

    assertProblem(response, HTTP_NOT_FOUND, APPROVAL_NOT_FOUND_MESSAGE.formatted(unknownHandle));
    assertTrue(pullQueue.getPullRequests().isEmpty());
  }

  @Test
  void shouldReturnBadRequestWhenSourceDoesNotMatchSourceOfApproval() throws IOException {
    var otherSource = registeredSource();
    var cloudEvent = event(randomEventId(), otherSource, approval.handle().value());

    var response = send(cloudEvent);

    assertProblem(
        response,
        HTTP_BAD_REQUEST,
        SOURCE_MISMATCH_MESSAGE.formatted(otherSource, approval.identifier()));
    assertTrue(pullQueue.getPullRequests().isEmpty());
  }

  @Test
  void shouldReturnBadRequestWhenSourceIsNotInSourceSystemRegister() throws IOException {
    var unregisteredApproval = persistApprovalWithSource(randomUri());

    var response = send(validEvent(unregisteredApproval));

    assertProblem(
        response,
        HTTP_BAD_REQUEST,
        UNREGISTERED_SOURCE_MESSAGE.formatted(unregisteredApproval.source()));
    assertTrue(pullQueue.getPullRequests().isEmpty());
  }

  @ParameterizedTest
  @NullSource
  @MethodSource("malformedEventIds")
  void shouldReturnBadRequestWhenEventIdIsMalformed(String eventId) throws IOException {
    var cloudEvent = event(eventId, approval.source(), approval.handle().value());

    var response = send(cloudEvent);

    assertProblem(response, HTTP_BAD_REQUEST, INVALID_EVENT_ID_MESSAGE);
    assertTrue(pullQueue.getPullRequests().isEmpty());
  }

  @ParameterizedTest
  @MethodSource("wellFormedEventIds")
  void shouldReturnAcceptedWhenEventIdIsWellFormed(String eventId) throws IOException {
    var cloudEvent = event(eventId, approval.source(), approval.handle().value());

    var response = send(cloudEvent);

    assertEquals(HTTP_ACCEPTED, response.getStatusCode());
  }

  @ParameterizedTest
  @NullSource
  @MethodSource("unsupportedTypes")
  void shouldReturnBadRequestWhenTypeIsNotSourceChanged(String type) throws IOException {
    var cloudEvent =
        new CloudEvent(
            SPEC_VERSION,
            randomEventId(),
            approval.source(),
            type,
            approval.handle().value(),
            null,
            null,
            null,
            null,
            null);

    var response = send(cloudEvent);

    assertProblem(response, HTTP_BAD_REQUEST, UNSUPPORTED_TYPE_MESSAGE.formatted(type));
  }

  @ParameterizedTest
  @NullSource
  @MethodSource("unsupportedSpecVersions")
  void shouldReturnBadRequestWhenSpecVersionIsNotOnePointZero(String specVersion)
      throws IOException {
    var cloudEvent =
        new CloudEvent(
            specVersion,
            randomEventId(),
            approval.source(),
            SOURCE_CHANGED_TYPE,
            approval.handle().value(),
            null,
            null,
            null,
            null,
            null);

    var response = send(cloudEvent);

    assertProblem(
        response, HTTP_BAD_REQUEST, UNSUPPORTED_SPEC_VERSION_MESSAGE.formatted(specVersion));
  }

  @Test
  void shouldReturnBadRequestWhenSourceIsMissing() throws IOException {
    var cloudEvent = event(randomEventId(), null, approval.handle().value());

    var response = send(cloudEvent);

    assertProblem(response, HTTP_BAD_REQUEST, SOURCE_MANDATORY_MESSAGE);
  }

  @ParameterizedTest
  @NullSource
  @MethodSource("subjectsThatAreNotHandles")
  void shouldReturnBadRequestWhenSubjectIsNotHandle(URI subject) throws IOException {
    var cloudEvent = event(randomEventId(), approval.source(), subject);

    var response = send(cloudEvent);

    assertProblem(response, HTTP_BAD_REQUEST, SUBJECT_NOT_HANDLE_MESSAGE.formatted(subject));
  }

  @Test
  void shouldReturnBadRequestWhenEventCarriesData() throws IOException {
    var cloudEvent = withData(validEvent(approval), Map.of(DATA_FIELD_VALUE, true), null);

    var response = send(cloudEvent);

    assertProblem(response, HTTP_BAD_REQUEST, DATA_NOT_ALLOWED_MESSAGE);
    assertTrue(pullQueue.getPullRequests().isEmpty());
  }

  @Test
  void shouldReturnBadRequestWhenEventCarriesBase64Data() throws IOException {
    var cloudEvent = withData(validEvent(approval), null, DATA_FIELD_VALUE);

    var response = send(cloudEvent);

    assertProblem(response, HTTP_BAD_REQUEST, DATA_NOT_ALLOWED_MESSAGE);
  }

  @Test
  void shouldReturnBadRequestWhenBodyIsMissing() throws IOException {
    var request = authorizedRequestBuilder(APPROVAL_UPSERT_SCOPE).withClientId(CLIENT_ID).build();

    handler.handleRequest(request, output, CONTEXT);

    assertProblem(problemResponse(), HTTP_BAD_REQUEST, MISSING_BODY_MESSAGE);
  }

  @Test
  void shouldReturnBadRequestWhenBodyIsMalformedJson() throws IOException {
    var request =
        new HandlerRequestBuilder<String>(JsonUtils.dtoObjectMapper)
            .withBody(MALFORMED_JSON_BODY)
            .withScope(APPROVAL_UPSERT_SCOPE)
            .withClientId(CLIENT_ID)
            .withCurrentCustomer(CUSTOMER_ID)
            .build();

    handler.handleRequest(request, output, CONTEXT);

    assertEquals(HTTP_BAD_REQUEST, problemResponse().getStatusCode());
    assertTrue(pullQueue.getPullRequests().isEmpty());
  }

  @Test
  void shouldReturnUnauthorizedWhenCustomerOfClientCannotBeResolved() throws IOException {
    var request =
        new HandlerRequestBuilder<CloudEvent>(JsonUtils.dtoObjectMapper)
            .withBody(validEvent(approval))
            .withScope(APPROVAL_UPSERT_SCOPE)
            .withClientId(CLIENT_ID)
            .build();

    handler.handleRequest(request, output, CONTEXT);

    assertEquals(HTTP_UNAUTHORIZED, problemResponse().getStatusCode());
    assertTrue(pullQueue.getPullRequests().isEmpty());
  }

  @Test
  void shouldReturnUnauthorizedWhenClientIdIsMissing() throws IOException {
    var request =
        authorizedRequestBuilder(APPROVAL_UPSERT_SCOPE).withBody(validEvent(approval)).build();

    handler.handleRequest(request, output, CONTEXT);

    assertProblem(problemResponse(), HTTP_UNAUTHORIZED, MISSING_CLIENT_ID_MESSAGE);
    assertTrue(pullQueue.getPullRequests().isEmpty());
  }

  @Test
  void shouldReturnForbiddenWhenClientHasNoScope() throws IOException {
    var request =
        new HandlerRequestBuilder<CloudEvent>(JsonUtils.dtoObjectMapper)
            .withBody(validEvent(approval))
            .withClientId(CLIENT_ID)
            .withCurrentCustomer(CUSTOMER_ID)
            .build();

    handler.handleRequest(request, output, CONTEXT);

    assertProblem(problemResponse(), HTTP_FORBIDDEN, MISSING_SCOPE_MESSAGE);
    assertTrue(pullQueue.getPullRequests().isEmpty());
  }

  @Test
  void shouldReturnForbiddenWhenClientHasOnlyOtherThirdPartyScope() throws IOException {
    var response = sendWithScope(validEvent(approval), OTHER_THIRD_PARTY_SCOPE);

    assertProblem(response, HTTP_FORBIDDEN, MISSING_SCOPE_MESSAGE);
    assertTrue(pullQueue.getPullRequests().isEmpty());
  }

  @Test
  void shouldReturnForbiddenBeforeValidatingBody() throws IOException {
    var invalidEvent = event(null, approval.source(), approval.handle().value());

    var response = sendWithScope(invalidEvent, OTHER_THIRD_PARTY_SCOPE);

    assertProblem(response, HTTP_FORBIDDEN, MISSING_SCOPE_MESSAGE);
  }

  @Test
  void shouldReturnBadGatewayWhenPullCannotBeEnqueued() throws IOException {
    var failingPullQueue = mock(PullQueue.class);
    doThrow(new IllegalStateException(randomString())).when(failingPullQueue).enqueue(any());
    handler = handlerWithPullQueue(failingPullQueue);

    var response = send(validEvent(approval));

    assertProblem(response, HTTP_BAD_GATEWAY, BAD_GATEWAY_MESSAGE);
  }

  @Test
  void shouldReturnBadGatewayWhenEventCannotBeStored() throws IOException {
    var failingRepository = mock(ApprovalRepository.class);
    when(failingRepository.findByHandle(any())).thenReturn(Optional.of(approval));
    when(failingRepository.saveEventIfAbsent(any()))
        .thenThrow(new IllegalStateException(randomString()));
    handler =
        new ReceiveEventHandler(
            new ApprovalEventService(failingRepository, sourceSystemRegister(), pullQueue),
            ENVIRONMENT);

    var response = send(validEvent(approval));

    assertProblem(response, HTTP_BAD_GATEWAY, BAD_GATEWAY_MESSAGE);
    assertTrue(pullQueue.getPullRequests().isEmpty());
  }

  private static Stream<String> malformedEventIds() {
    return Stream.concat(
        Stream.of(VALID_ID_CHARACTER.repeat(MAX_EVENT_ID_LENGTH + 1)),
        MALFORMED_EVENT_IDS.stream());
  }

  private static Stream<String> wellFormedEventIds() {
    return Stream.concat(
        Stream.of(VALID_ID_CHARACTER, VALID_ID_CHARACTER.repeat(MAX_EVENT_ID_LENGTH)),
        WELL_FORMED_EVENT_IDS.stream());
  }

  private static Stream<String> unsupportedTypes() {
    return Stream.concat(
        Stream.of(SOURCE_CHANGED_TYPE.toUpperCase(Locale.ROOT)), UNSUPPORTED_TYPES.stream());
  }

  private static Stream<String> unsupportedSpecVersions() {
    return UNSUPPORTED_SPEC_VERSIONS.stream();
  }

  private static Stream<URI> subjectsThatAreNotHandles() {
    return SUBJECTS_THAT_ARE_NOT_HANDLES.stream();
  }

  private static URI registeredSource() {
    return REGISTERED_BASE_URI.resolve(randomString());
  }

  private static SourceSystemRegister sourceSystemRegister() {
    return new SourceSystemRegister(List.of(new SourceSystem(randomString(), REGISTERED_BASE_URI)));
  }

  private static String randomEventId() {
    return randomUUID().toString();
  }

  private static CloudEvent validEvent(Approval approval) {
    return event(randomEventId(), approval.source(), approval.handle().value());
  }

  private static CloudEvent event(String eventId, URI source, URI subject) {
    return new CloudEvent(
        SPEC_VERSION,
        eventId,
        source,
        SOURCE_CHANGED_TYPE,
        subject,
        Instant.now(),
        null,
        null,
        null,
        null);
  }

  private static CloudEvent withTime(CloudEvent cloudEvent, Instant time) {
    return new CloudEvent(
        cloudEvent.specversion(),
        cloudEvent.id(),
        cloudEvent.source(),
        cloudEvent.type(),
        cloudEvent.subject(),
        time,
        cloudEvent.datacontenttype(),
        cloudEvent.dataschema(),
        cloudEvent.data(),
        cloudEvent.dataBase64());
  }

  private static CloudEvent withData(CloudEvent cloudEvent, Object data, Object dataBase64) {
    return new CloudEvent(
        cloudEvent.specversion(),
        cloudEvent.id(),
        cloudEvent.source(),
        cloudEvent.type(),
        cloudEvent.subject(),
        cloudEvent.time(),
        cloudEvent.datacontenttype(),
        cloudEvent.dataschema(),
        data,
        dataBase64);
  }

  private static String expectedEventKey(CloudEvent cloudEvent) {
    try {
      var keyMaterial = cloudEvent.source() + EVENT_KEY_SEPARATOR + cloudEvent.id();
      var digest = MessageDigest.getInstance(HASH_ALGORITHM).digest(keyMaterial.getBytes(UTF_8));
      return HexFormat.of().formatHex(digest);
    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalStateException(exception);
    }
  }

  private static void assertProblem(
      GatewayResponse<Problem> response, int expectedStatus, String expectedDetail)
      throws JsonProcessingException {
    assertEquals(expectedStatus, response.getStatusCode());
    assertEquals(expectedDetail, response.getBodyObject(Problem.class).getDetail());
  }

  private ReceiveEventHandler handlerWithPullQueue(PullQueue queue) {
    return new ReceiveEventHandler(
        new ApprovalEventService(approvalRepository, sourceSystemRegister(), queue), ENVIRONMENT);
  }

  private Approval persistApprovalWithSource(URI source) {
    var persistedApproval =
        randomApproval(randomUUID(), randomIdentifiers(), source, randomHandle(), CUSTOMER_ID);
    approvalRepository.save(persistedApproval);
    return persistedApproval;
  }

  private GatewayResponse<Problem> send(CloudEvent cloudEvent) throws IOException {
    return sendWithScope(cloudEvent, APPROVAL_UPSERT_SCOPE);
  }

  private GatewayResponse<Problem> sendWithScope(CloudEvent cloudEvent, String scope)
      throws IOException {
    var request =
        authorizedRequestBuilder(scope).withBody(cloudEvent).withClientId(CLIENT_ID).build();
    handler.handleRequest(request, output, CONTEXT);
    return problemResponse();
  }

  private static HandlerRequestBuilder<CloudEvent> authorizedRequestBuilder(String scope) {
    return new HandlerRequestBuilder<CloudEvent>(JsonUtils.dtoObjectMapper)
        .withScope(scope)
        .withCurrentCustomer(CUSTOMER_ID);
  }

  private GatewayResponse<Problem> problemResponse() throws JsonProcessingException {
    return GatewayResponse.fromOutputStream(output, Problem.class);
  }

  private EventDao fetchEvent(String eventKey) throws JsonProcessingException {
    return JsonUtils.dtoObjectMapper.readValue(
        fetchItem(EventDao.toDatabaseIdentifier(eventKey)), EventDao.class);
  }

  private PendingDao fetchPendingMarker(String eventKey) throws JsonProcessingException {
    return JsonUtils.dtoObjectMapper.readValue(
        fetchItem(PendingDao.toDatabaseIdentifier(eventKey)), PendingDao.class);
  }

  private String fetchItem(String databaseIdentifier) {
    var key =
        Map.of(
            PK0, AttributeValue.fromS(databaseIdentifier),
            SK0, AttributeValue.fromS(databaseIdentifier));
    var item =
        dynamoDbLocal
            .client()
            .getItem(GetItemRequest.builder().tableName(TABLE).key(key).build())
            .item();
    return EnhancedDocument.fromAttributeValueMap(item).toJson();
  }
}
