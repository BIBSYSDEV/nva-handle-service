package no.sikt.nva.approvals.rest;

import static java.net.HttpURLConnection.HTTP_ACCEPTED;
import static java.net.HttpURLConnection.HTTP_BAD_GATEWAY;
import static java.net.HttpURLConnection.HTTP_BAD_REQUEST;
import static java.net.HttpURLConnection.HTTP_FORBIDDEN;
import static java.net.HttpURLConnection.HTTP_NOT_FOUND;
import static no.sikt.nva.approvals.utils.TestUtils.randomApproval;
import static no.sikt.nva.approvals.utils.TestUtils.randomIdentifier;
import static no.sikt.nva.approvals.utils.TestUtils.randomIdentifiers;
import static no.sikt.nva.approvals.validation.RequestConstraints.MAX_IDENTIFIERS;
import static no.sikt.nva.approvals.validation.RequestConstraints.MAX_IDENTIFIER_VALUE_BYTES;
import static no.sikt.nva.approvals.validation.RequestConstraints.MAX_URI_LENGTH;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import no.sikt.nva.approvals.domain.ApprovalNotFoundException;
import no.sikt.nva.approvals.domain.ApprovalService;
import no.sikt.nva.approvals.domain.ApprovalServiceException;
import no.sikt.nva.approvals.domain.CustomerMismatchException;
import no.sikt.nva.approvals.domain.FakeApprovalService;
import no.sikt.nva.approvals.domain.NamedIdentifier;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.stubs.FakeContext;
import no.unit.nva.testutils.HandlerRequestBuilder;
import nva.commons.apigateway.GatewayResponse;
import nva.commons.core.Environment;
import nva.commons.core.paths.UriWrapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.zalando.problem.Problem;

class UpdateApprovalHandlerTest {

  private static final Context context = new FakeContext();
  private static final String APPROVAL_ID_PATH_PARAMETER = "approvalId";
  private static final String APPROVAL_PATH = "approval";
  private static final String API_HOST = "API_HOST";
  private static final String REK = "REK";
  private static final String MANDATORY_MESSAGE = "Is mandatory";
  private static final String SOURCE_MANDATORY_DETAIL = "/source: Is mandatory";
  private static final String IDENTIFIERS_SIZE_MESSAGE =
      "Between 1 and 20 identifiers are required";
  private static final String VALUE_TOO_LONG_MESSAGE = "Must be at most 900 bytes long in UTF-8";
  private static final String IDENTIFIERS_POINTER = "/identifiers";
  private static final String SOURCE_POINTER = "/source";
  private static final String IDENTIFIER_POINTER = "/identifier";
  private static final String ERRORS_PARAMETER = "errors";
  private static final String DETAIL_FIELD = "detail";
  private static final String POINTER_FIELD = "pointer";
  private static final String CHARACTER = "a";
  private static final String BASE_URI = "https://hdl.handle.net/11250.1/";
  private static final String URI_TOO_LONG_MESSAGE = "Must be at most 1024 characters long";
  private static final String HANDLE_POINTER = "/handle";
  private static final String ID_MISMATCH_MESSAGE = "Provided id %s does not address approval %s";
  private static final String IDENTIFIER_MISMATCH_MESSAGE =
      "Provided identifier %s does not match approval %s";
  private static final String INVALID_ID_MESSAGE = "Provided id is invalid %s";
  private static final String OPAQUE_URI_TEMPLATE = "urn:uuid:%s";
  private static final String CUSTOMER_MISMATCH_MESSAGE =
      "Customer id does not match requested approval customer id";
  private static final String INVALID_APPROVAL_ID_MESSAGE =
      "Provided approval identifier is not valid!";
  private UpdateApprovalHandler handler;
  private ByteArrayOutputStream output;
  private UUID approvalId;
  private IdentifierAuthorizer identifierAuthorizer;

  @BeforeEach
  void setUp() {
    this.output = new ByteArrayOutputStream();
    this.approvalId = UUID.randomUUID();
    this.identifierAuthorizer = mock(IdentifierAuthorizer.class);
    handler =
        new UpdateApprovalHandler(
            new FakeApprovalService(), identifierAuthorizer, new Environment());
  }

  @Test
  void shouldReturnAcceptedResponseOnSuccess() throws IOException {
    var request = createRequest(randomUpdateApprovalRequest(), approvalId);

    handler.handleRequest(request, output, context);

    var response = GatewayResponse.fromOutputStream(output, Void.class);

    assertEquals(HTTP_ACCEPTED, response.getStatusCode());
  }

  @Test
  void shouldSetLocationHeaderOnSuccess() throws IOException {
    var request = createRequest(randomUpdateApprovalRequest(), approvalId);

    handler.handleRequest(request, output, context);

    var response = GatewayResponse.fromOutputStream(output, Void.class);

    var locationHeader = response.getHeaders().get("Location");
    var expectedLocation = createExpectedLocationHeader(approvalId);

    assertEquals(expectedLocation, locationHeader);
  }

  @Test
  void shouldSetRetryAfterHeaderOnSuccessWith5SecondsValue() throws IOException {
    var request = createRequest(randomUpdateApprovalRequest(), approvalId);

    handler.handleRequest(request, output, context);

    var response = GatewayResponse.fromOutputStream(output, Void.class);

    var retryAfterHeader = response.getHeaders().get("Retry-After");

    assertEquals("5", retryAfterHeader);
  }

  @Test
  void shouldReturnBadRequestWhenRequestBodyIsInvalidDueToMissingIdentifiers() throws IOException {
    var request = createRawJsonRequest(requestBodyWithoutIdentifiers(), approvalId);

    handler.handleRequest(request, output, context);

    var response = GatewayResponse.fromOutputStream(output, Problem.class);

    assertEquals(HTTP_BAD_REQUEST, response.getStatusCode());
    assertEquals(List.of(error(MANDATORY_MESSAGE, IDENTIFIERS_POINTER)), problemErrors(response));
  }

  @Test
  void shouldReturnBadRequestWhenRequestBodyIsInvalidDueToMissingSource() throws IOException {
    var request = createRawJsonRequest(requestBodyWithoutSource(), approvalId);

    handler.handleRequest(request, output, context);

    var response = GatewayResponse.fromOutputStream(output, Problem.class);

    assertEquals(HTTP_BAD_REQUEST, response.getStatusCode());
    assertEquals(SOURCE_MANDATORY_DETAIL, problemDetail(response));
    assertEquals(List.of(error(MANDATORY_MESSAGE, SOURCE_POINTER)), problemErrors(response));
  }

  @Test
  void shouldAcceptMaximumNumberOfIdentifiers() throws IOException {
    var request =
        createRequest(requestWithIdentifiers(randomIdentifiers(MAX_IDENTIFIERS)), approvalId);

    handler.handleRequest(request, output, context);

    var response = GatewayResponse.fromOutputStream(output, Void.class);

    assertEquals(HTTP_ACCEPTED, response.getStatusCode());
  }

  @Test
  void shouldReturnBadRequestWhenMoreThanMaximumNumberOfIdentifiers() throws IOException {
    var request =
        createRequest(requestWithIdentifiers(randomIdentifiers(MAX_IDENTIFIERS + 1)), approvalId);

    handler.handleRequest(request, output, context);

    var response = GatewayResponse.fromOutputStream(output, Problem.class);

    assertEquals(HTTP_BAD_REQUEST, response.getStatusCode());
    assertEquals(
        List.of(error(IDENTIFIERS_SIZE_MESSAGE, IDENTIFIERS_POINTER)), problemErrors(response));
    verifyNoInteractions(identifierAuthorizer);
  }

  @Test
  void shouldReturnBadRequestPointingToIdentifierValueThatIsTooLong() throws IOException {
    var tooLongValue = CHARACTER.repeat(MAX_IDENTIFIER_VALUE_BYTES + 1);
    var identifiers = List.of(randomIdentifier(), new NamedIdentifier(REK, tooLongValue));
    var request = createRequest(requestWithIdentifiers(identifiers), approvalId);

    handler.handleRequest(request, output, context);

    var response = GatewayResponse.fromOutputStream(output, Problem.class);

    assertEquals(HTTP_BAD_REQUEST, response.getStatusCode());
    assertEquals(
        List.of(error(VALUE_TOO_LONG_MESSAGE, "/identifiers/1/value")), problemErrors(response));
  }

  @Test
  void shouldReturnBadRequestWhenIdentifierDoesNotMatchApprovalInPath() throws IOException {
    var otherIdentifier = UUID.randomUUID();
    var request = createRequest(updateApprovalRequest(null, otherIdentifier, null), approvalId);

    handler.handleRequest(request, output, context);

    var response = GatewayResponse.fromOutputStream(output, Problem.class);

    assertEquals(HTTP_BAD_REQUEST, response.getStatusCode());
    var expectedMessage = IDENTIFIER_MISMATCH_MESSAGE.formatted(otherIdentifier, approvalId);
    assertEquals(expectedMessage, problemDetail(response));
    assertEquals(List.of(error(expectedMessage, IDENTIFIER_POINTER)), problemErrors(response));
  }

  @Test
  void shouldReturnBadRequestWhenIdDoesNotAddressApprovalInPath() throws IOException {
    var otherApprovalUri = approvalUri(UUID.randomUUID());
    var request = createRequest(updateApprovalRequest(otherApprovalUri, null, null), approvalId);

    handler.handleRequest(request, output, context);

    var response = GatewayResponse.fromOutputStream(output, Problem.class);

    assertEquals(HTTP_BAD_REQUEST, response.getStatusCode());
    assertEquals(
        ID_MISMATCH_MESSAGE.formatted(otherApprovalUri, approvalId), problemDetail(response));
  }

  @Test
  void shouldReturnBadRequestWhenIdIsNotAnApprovalUri() throws IOException {
    var unrelatedUri = randomUri();
    var request = createRequest(updateApprovalRequest(unrelatedUri, null, null), approvalId);

    handler.handleRequest(request, output, context);

    var response = GatewayResponse.fromOutputStream(output, Problem.class);

    assertEquals(HTTP_BAD_REQUEST, response.getStatusCode());
    assertEquals(ID_MISMATCH_MESSAGE.formatted(unrelatedUri, approvalId), problemDetail(response));
  }

  @Test
  void shouldReturnBadRequestWhenIdIsInvalidUri() throws IOException {
    var invalidId = URI.create(OPAQUE_URI_TEMPLATE.formatted(approvalId));
    var request = createRequest(updateApprovalRequest(invalidId, null, null), approvalId);

    handler.handleRequest(request, output, context);

    var response = GatewayResponse.fromOutputStream(output, Problem.class);

    assertEquals(HTTP_BAD_REQUEST, response.getStatusCode());
    assertEquals(INVALID_ID_MESSAGE.formatted(invalidId), problemDetail(response));
  }

  @Test
  void shouldAcceptRoundTrippedRequestWhenIdAndIdentifierAddressApprovalInPath()
      throws IOException {
    var request =
        createRequest(updateApprovalRequest(approvalUri(approvalId), approvalId, null), approvalId);

    handler.handleRequest(request, output, context);

    var response = GatewayResponse.fromOutputStream(output, Void.class);

    assertEquals(HTTP_ACCEPTED, response.getStatusCode());
  }

  @Test
  void shouldNotAuthorizeIdentifiersWhenRequestBodyContradictsApprovalInPath() throws Exception {
    var request = createRequest(updateApprovalRequest(null, UUID.randomUUID(), null), approvalId);

    handler.handleRequest(request, output, context);

    verifyNoInteractions(identifierAuthorizer);
  }

  @Test
  void shouldReturnBadRequestWhenApprovalIdIsInvalid() throws IOException {
    var request = createRequestWithInvalidApprovalId(randomUpdateApprovalRequest());

    handler.handleRequest(request, output, context);

    var response = GatewayResponse.fromOutputStream(output, Problem.class);

    assertEquals(HTTP_BAD_REQUEST, response.getStatusCode());
    assertEquals(INVALID_APPROVAL_ID_MESSAGE, problemDetail(response));
    assertEquals(
        List.of(error(INVALID_APPROVAL_ID_MESSAGE, APPROVAL_ID_PATH_PARAMETER)),
        problemErrors(response));
  }

  @Test
  void shouldReturnNotFoundWhenApprovalDoesNotExistDuringUpdate() throws IOException {
    handler =
        new UpdateApprovalHandler(
            new FakeApprovalService(new ApprovalNotFoundException(approvalId)),
            identifierAuthorizer,
            new Environment());
    var request = createRequest(randomUpdateApprovalRequest(), approvalId);

    handler.handleRequest(request, output, context);

    var response = GatewayResponse.fromOutputStream(output, Void.class);

    assertEquals(HTTP_NOT_FOUND, response.getStatusCode());
  }

  @Test
  void shouldReturnBadGatewayWhenApprovalServiceThrowsApprovalServiceException()
      throws IOException {
    handler =
        new UpdateApprovalHandler(
            new FakeApprovalService(new ApprovalServiceException("error")),
            identifierAuthorizer,
            new Environment());
    var request = createRequest(randomUpdateApprovalRequest(), approvalId);

    handler.handleRequest(request, output, context);

    var response = GatewayResponse.fromOutputStream(output, Void.class);

    assertEquals(HTTP_BAD_GATEWAY, response.getStatusCode());
  }

  @Test
  void shouldReturnForbiddenWhenIdentifierNameIsNotAllowedForCustomer() throws Exception {
    doThrow(new DisallowedIdentifierNamesException(Set.of(REK)))
        .when(identifierAuthorizer)
        .authorizeIdentifiers(any(), any());
    var request = createRequest(randomUpdateApprovalRequest(), approvalId);

    handler.handleRequest(request, output, context);

    var response = GatewayResponse.fromOutputStream(output, Problem.class);

    assertEquals(HTTP_FORBIDDEN, response.getStatusCode());
    assertEquals(
        "Identifier names not allowed for customer: [%s]".formatted(REK), problemDetail(response));
  }

  @Test
  void shouldReturnForbiddenWhenCustomerIdentifierDoesNotMatchApprovalCustomer() throws Exception {
    handler =
        new UpdateApprovalHandler(
            new FakeApprovalService(new CustomerMismatchException()),
            identifierAuthorizer,
            new Environment());
    var request = createRequest(randomUpdateApprovalRequest(), approvalId);

    handler.handleRequest(request, output, context);

    var response = GatewayResponse.fromOutputStream(output, Problem.class);

    assertEquals(HTTP_FORBIDDEN, response.getStatusCode());
    assertEquals(CUSTOMER_MISMATCH_MESSAGE, problemDetail(response));
  }

  @Test
  void shouldPassSourceFromRequestToApprovalService() throws Exception {
    var approvalService = mock(ApprovalService.class);
    when(approvalService.updateApproval(any(), any(), any(), any()))
        .thenReturn(randomApproval(approvalId, randomUri()));
    var customerIdentifier = UUID.randomUUID();
    when(identifierAuthorizer.authorizeIdentifiers(any(), any())).thenReturn(customerIdentifier);
    handler = new UpdateApprovalHandler(approvalService, identifierAuthorizer, new Environment());
    var updateApprovalRequest = randomUpdateApprovalRequest();

    handler.handleRequest(createRequest(updateApprovalRequest, approvalId), output, context);

    verify(approvalService)
        .updateApproval(
            eq(approvalId),
            eq(updateApprovalRequest.identifiers()),
            eq(updateApprovalRequest.source()),
            eq(customerIdentifier));
  }

  @Test
  void shouldAcceptRequestBodyContainingIdentifiersAndSourceOnly() throws IOException {
    var request = createRawJsonRequest(requestBodyWithIdentifiersAndSourceOnly(), approvalId);

    handler.handleRequest(request, output, context);

    var response = GatewayResponse.fromOutputStream(output, Void.class);

    assertEquals(HTTP_ACCEPTED, response.getStatusCode());
  }

  @Test
  void shouldReturnBadRequestPointingToHandleThatIsTooLong() throws IOException {
    var tooLongHandle = URI.create(BASE_URI + CHARACTER.repeat(MAX_URI_LENGTH));
    var request = createRequest(updateApprovalRequest(null, null, tooLongHandle), approvalId);

    handler.handleRequest(request, output, context);

    var response = GatewayResponse.fromOutputStream(output, Problem.class);

    assertEquals(HTTP_BAD_REQUEST, response.getStatusCode());
    assertEquals(List.of(error(URI_TOO_LONG_MESSAGE, HANDLE_POINTER)), problemErrors(response));
  }

  @Test
  void shouldIgnoreHandleInRequestBody() throws IOException {
    var request = createRequest(updateApprovalRequest(null, null, randomUri()), approvalId);

    handler.handleRequest(request, output, context);

    var response = GatewayResponse.fromOutputStream(output, Void.class);

    assertEquals(HTTP_ACCEPTED, response.getStatusCode());
  }

  @Test
  void shouldAuthorizeIdentifiersFromRequest() throws Exception {
    var updateApprovalRequest = randomUpdateApprovalRequest();
    var request = createRequest(updateApprovalRequest, approvalId);

    handler.handleRequest(request, output, context);

    verify(identifierAuthorizer)
        .authorizeIdentifiers(any(), eq(updateApprovalRequest.identifiers()));
  }

  private static String requestBodyWithoutIdentifiers() {
    return """
    {
      "source": "https://example.com/source/12345"
    }
    """;
  }

  private static String problemDetail(GatewayResponse<Problem> response)
      throws JsonProcessingException {
    return response.getBodyObject(Problem.class).getDetail();
  }

  private static Object problemErrors(GatewayResponse<Problem> response)
      throws JsonProcessingException {
    return response.getBodyObject(Problem.class).getParameters().get(ERRORS_PARAMETER);
  }

  private static Map<String, String> error(String detail, String pointer) {
    return Map.of(DETAIL_FIELD, detail, POINTER_FIELD, pointer);
  }

  private static UpdateApprovalRequest requestWithIdentifiers(
      Collection<NamedIdentifier> namedIdentifiers) {
    return new UpdateApprovalRequest(null, null, namedIdentifiers, randomUri(), null);
  }

  private static String requestBodyWithIdentifiersAndSourceOnly() {
    return """
    {
      "identifiers": [
        {
          "type": "Identifier",
          "name": "REK",
          "value": "123"
        }
      ],
      "source": "https://example.com/source/12345"
    }
    """;
  }

  private static String requestBodyWithoutSource() {
    return """
    {
      "identifiers": [
        {
          "type": "Identifier",
          "name": "REK",
          "value": "123"
        }
      ]
    }
    """;
  }

  private static UpdateApprovalRequest randomUpdateApprovalRequest() {
    return updateApprovalRequest(null, null, null);
  }

  private static UpdateApprovalRequest updateApprovalRequest(URI id, UUID identifier, URI handle) {
    return new UpdateApprovalRequest(
        id,
        identifier,
        List.of(new NamedIdentifier(randomString(), randomString())),
        randomUri(),
        handle);
  }

  private static URI approvalUri(UUID identifier) {
    return UriWrapper.fromHost(new Environment().readEnv(API_HOST))
        .addChild(APPROVAL_PATH)
        .addChild(identifier.toString())
        .getUri();
  }

  private String createExpectedLocationHeader(UUID identifier) {
    return UriWrapper.fromHost(new Environment().readEnv("API_HOST"))
        .addChild("approval")
        .addChild(identifier.toString())
        .toString();
  }

  private InputStream createRequest(UpdateApprovalRequest request, UUID approvalId)
      throws JsonProcessingException {
    return new HandlerRequestBuilder<UpdateApprovalRequest>(JsonUtils.dtoObjectMapper)
        .withBody(request)
        .withPathParameters(Map.of(APPROVAL_ID_PATH_PARAMETER, approvalId.toString()))
        .build();
  }

  private InputStream createRequestWithInvalidApprovalId(UpdateApprovalRequest request)
      throws JsonProcessingException {
    return new HandlerRequestBuilder<UpdateApprovalRequest>(JsonUtils.dtoObjectMapper)
        .withBody(request)
        .withPathParameters(Map.of(APPROVAL_ID_PATH_PARAMETER, "invalid-uuid"))
        .build();
  }

  private InputStream createRawJsonRequest(String jsonBody, UUID approvalId)
      throws JsonProcessingException {
    return new HandlerRequestBuilder<String>(JsonUtils.dtoObjectMapper)
        .withBody(jsonBody)
        .withPathParameters(Map.of(APPROVAL_ID_PATH_PARAMETER, approvalId.toString()))
        .build();
  }
}
