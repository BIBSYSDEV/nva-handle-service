package no.sikt.nva.approvals.rest;

import static java.net.HttpURLConnection.HTTP_ACCEPTED;
import static java.net.HttpURLConnection.HTTP_BAD_GATEWAY;
import static java.net.HttpURLConnection.HTTP_BAD_REQUEST;
import static java.net.HttpURLConnection.HTTP_FORBIDDEN;
import static java.net.HttpURLConnection.HTTP_NOT_FOUND;
import static no.sikt.nva.approvals.utils.TestUtils.randomApproval;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
  private static final String IDENTIFIERS_MANDATORY_MESSAGE =
      "At least one identifier is mandatory for approval update";
  private static final String SOURCE_MANDATORY_MESSAGE = "Source is mandatory for approval update";
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
    assertTrue(problemDetail(response).contains(IDENTIFIERS_MANDATORY_MESSAGE));
  }

  @Test
  void shouldReturnBadRequestWhenRequestBodyIsInvalidDueToMissingSource() throws IOException {
    var request = createRawJsonRequest(requestBodyWithoutSource(), approvalId);

    handler.handleRequest(request, output, context);

    var response = GatewayResponse.fromOutputStream(output, Problem.class);

    assertEquals(HTTP_BAD_REQUEST, response.getStatusCode());
    assertEquals(SOURCE_MANDATORY_MESSAGE, problemDetail(response));
  }

  @Test
  void shouldReturnBadRequestWhenIdentifierDoesNotMatchApprovalInPath() throws IOException {
    var otherIdentifier = UUID.randomUUID();
    var request = createRequest(updateApprovalRequest(null, otherIdentifier, null), approvalId);

    handler.handleRequest(request, output, context);

    var response = GatewayResponse.fromOutputStream(output, Problem.class);

    assertEquals(HTTP_BAD_REQUEST, response.getStatusCode());
    assertEquals(
        IDENTIFIER_MISMATCH_MESSAGE.formatted(otherIdentifier, approvalId),
        problemDetail(response));
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
