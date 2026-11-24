package no.sikt.nva.approvals.rest;

import static java.net.HttpURLConnection.HTTP_BAD_REQUEST;
import static java.net.HttpURLConnection.HTTP_NOT_FOUND;
import static java.net.HttpURLConnection.HTTP_NOT_MODIFIED;
import static java.net.HttpURLConnection.HTTP_OK;
import static no.sikt.nva.approvals.utils.TestUtils.randomApproval;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static nva.commons.apigateway.ApiGatewayHandler.ALLOWED_ORIGIN_ENV;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.emptyOrNullString;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;

import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import no.sikt.nva.approvals.domain.Approval;
import no.sikt.nva.approvals.domain.FakeApprovalService;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.stubs.FakeContext;
import no.unit.nva.testutils.HandlerRequestBuilder;
import nva.commons.apigateway.GatewayResponse;
import nva.commons.apigateway.MediaType;
import nva.commons.core.Environment;
import org.apache.hc.core5.http.HttpHeaders;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.zalando.problem.Problem;

class FetchApprovalHandlerTest {

  private static final Context CONTEXT = new FakeContext();
  private static final String APPROVAL_ID_PATH_PARAMETER = "approvalId";
  private static final String ERRORS_PARAMETER = "errors";
  private static final String DETAIL_FIELD = "detail";
  private static final String POINTER_FIELD = "pointer";
  private static final String INVALID_APPROVAL_ID_MESSAGE =
      "Provided approval identifier is not valid!";
  private static final String API_HOST = "api.unittest.nva.unit.no";
  private static final String COGNITO_AUTHORIZER_URLS_ENV = "COGNITO_AUTHORIZER_URLS";
  private static final String API_HOST_ENV = "API_HOST";
  private FetchApprovalHandler handler;
  private ByteArrayOutputStream output;
  private Environment environment;

  @BeforeEach
  void setUp() {
    output = new ByteArrayOutputStream();
    environment = mock(Environment.class);
    lenient().when(environment.readEnv(ALLOWED_ORIGIN_ENV)).thenReturn("*");
    lenient()
        .when(environment.readEnv(COGNITO_AUTHORIZER_URLS_ENV))
        .thenReturn("http://localhost:3000");
    lenient().when(environment.readEnv(API_HOST_ENV)).thenReturn(API_HOST);
  }

  @Test
  void shouldReturnOkResponseWithApprovalOnSuccess() {
    var approvalId = UUID.randomUUID();
    var approval = randomApproval(approvalId, randomUri());
    handler = new FetchApprovalHandler(new FakeApprovalService(List.of(approval)), environment);
    var request = createRequestWithPathParameter(approvalId);

    var response = handleRequest(request);

    assertEquals(HTTP_OK, response.getStatusCode());
  }

  @Test
  void shouldReturnNotFoundWhenApprovalDoesNotExist() {
    var approvalId = UUID.randomUUID();
    handler = new FetchApprovalHandler(new FakeApprovalService(), environment);
    var request = createRequestWithPathParameter(approvalId);

    var response = handleRequest(request);

    assertEquals(HTTP_NOT_FOUND, response.getStatusCode());
  }

  @Test
  void shouldReturnBadRequestWhenApprovalIdIsInvalid() {
    handler = new FetchApprovalHandler(new FakeApprovalService(), environment);
    var request = createRequestWithInvalidId();

    var response = handleRequestAsProblem(request);

    assertEquals(HTTP_BAD_REQUEST, response.getStatusCode());
    assertEquals(
        List.of(
            Map.of(
                DETAIL_FIELD,
                INVALID_APPROVAL_ID_MESSAGE,
                POINTER_FIELD,
                APPROVAL_ID_PATH_PARAMETER)),
        problemErrors(response));
  }

  @Test
  void shouldReturnJsonWhenAcceptHeaderIsApplicationJson() {
    var approvalId = UUID.randomUUID();
    var approval = randomApproval(approvalId, randomUri());
    handler = new FetchApprovalHandler(new FakeApprovalService(List.of(approval)), environment);
    var request = createRequestWithAcceptHeader(approvalId, MediaType.JSON_UTF_8.toString());

    var response = handleRequest(request);

    assertEquals(HTTP_OK, response.getStatusCode());
    assertThat(
        response.getHeaders().get(HttpHeaders.CONTENT_TYPE), containsString("application/json"));
  }

  @Test
  void shouldReturnJsonWhenNoAcceptHeaderProvided() {
    var approvalId = UUID.randomUUID();
    var approval = randomApproval(approvalId, randomUri());
    handler = new FetchApprovalHandler(new FakeApprovalService(List.of(approval)), environment);
    var request = createRequestWithPathParameter(approvalId);

    var response = handleRequest(request);

    assertEquals(HTTP_OK, response.getStatusCode());
    assertThat(
        response.getHeaders().get(HttpHeaders.CONTENT_TYPE), containsString("application/json"));
  }

  @Test
  void shouldReturnJsonWhenBrowserAcceptHeaderProvided() {
    var browserAcceptHeader = "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8";
    var approvalId = UUID.randomUUID();
    var approval = randomApproval(approvalId, randomUri());
    handler = new FetchApprovalHandler(new FakeApprovalService(List.of(approval)), environment);
    var request = createRequestWithAcceptHeader(approvalId, browserAcceptHeader);

    var response = handleRequest(request);

    assertEquals(HTTP_OK, response.getStatusCode());
    assertThat(
        response.getHeaders().get(HttpHeaders.CONTENT_TYPE), containsString("application/json"));
  }

  @Test
  void shouldReturnNotAcceptableWhenAcceptHeaderIsUnsupported() {
    var approvalId = UUID.randomUUID();
    var approval = randomApproval(approvalId, randomUri());
    handler = new FetchApprovalHandler(new FakeApprovalService(List.of(approval)), environment);
    var request = createRequestWithAcceptHeader(approvalId, "application/xml");

    var response = handleRequest(request);

    assertEquals(HttpURLConnection.HTTP_NOT_ACCEPTABLE, response.getStatusCode());
  }

  @Test
  void shouldReturnEntityTagFromApprovalVersion() {
    var approval = approvalWithVersion(SortableIdentifier.next());
    handler = handlerFor(approval);

    var response = handleRequest(createRequestWithPathParameter(approval.identifier()));

    assertEquals(quoted(approval.version()), response.getHeaders().get(HttpHeaders.ETAG));
  }

  @ParameterizedTest
  @ValueSource(strings = {"\"%s\"", "W/\"%s\"", "\"other\", \"%s\"", "*"})
  void shouldReturnNotModifiedWithoutBodyWhenIfNoneMatchMatches(String ifNoneMatchTemplate) {
    var approval = approvalWithVersion(SortableIdentifier.next());
    handler = handlerFor(approval);
    var ifNoneMatch = ifNoneMatchTemplate.formatted(approval.version());

    var response =
        handleRequestAsString(createRequestWithIfNoneMatch(approval.identifier(), ifNoneMatch));

    assertEquals(HTTP_NOT_MODIFIED, response.getStatusCode());
    assertEquals(quoted(approval.version()), response.getHeaders().get(HttpHeaders.ETAG));
    assertThat(response.getBody(), is(emptyOrNullString()));
  }

  @ParameterizedTest
  @ValueSource(strings = {"\"unrelated\"", "W/\"unrelated\"", "\"unrelated,*,tag\""})
  void shouldReturnOkWhenIfNoneMatchDoesNotMatch(String ifNoneMatch) {
    var approval = approvalWithVersion(SortableIdentifier.next());
    handler = handlerFor(approval);

    var response = handleRequest(createRequestWithIfNoneMatch(approval.identifier(), ifNoneMatch));

    assertEquals(HTTP_OK, response.getStatusCode());
  }

  private GatewayResponse<ApprovalResponse> handleRequest(InputStream request) {
    try {
      handler.handleRequest(request, output, CONTEXT);
      return GatewayResponse.fromOutputStream(output, ApprovalResponse.class);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private GatewayResponse<Problem> handleRequestAsProblem(InputStream request) {
    try {
      handler.handleRequest(request, output, CONTEXT);
      return GatewayResponse.fromOutputStream(output, Problem.class);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private static Object problemErrors(GatewayResponse<Problem> response) {
    try {
      return response.getBodyObject(Problem.class).getParameters().get(ERRORS_PARAMETER);
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }

  private GatewayResponse<String> handleRequestAsString(InputStream request) {
    try {
      handler.handleRequest(request, output, CONTEXT);
      return GatewayResponse.fromOutputStream(output, String.class);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private InputStream createRequestWithPathParameter(UUID approvalId) {
    try {
      return new HandlerRequestBuilder<Void>(JsonUtils.dtoObjectMapper)
          .withPathParameters(Map.of(APPROVAL_ID_PATH_PARAMETER, approvalId.toString()))
          .build();
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }

  private InputStream createRequestWithInvalidId() {
    try {
      return new HandlerRequestBuilder<Void>(JsonUtils.dtoObjectMapper)
          .withPathParameters(Map.of(APPROVAL_ID_PATH_PARAMETER, "not-a-uuid"))
          .build();
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }

  private InputStream createRequestWithAcceptHeader(UUID approvalId, String acceptHeader) {
    return createRequestWithHeader(approvalId, HttpHeaders.ACCEPT, acceptHeader);
  }

  private InputStream createRequestWithIfNoneMatch(UUID approvalId, String ifNoneMatch) {
    return createRequestWithHeader(approvalId, HttpHeaders.IF_NONE_MATCH, ifNoneMatch);
  }

  private InputStream createRequestWithHeader(
      UUID approvalId, String headerName, String headerValue) {
    try {
      return new HandlerRequestBuilder<Void>(JsonUtils.dtoObjectMapper)
          .withPathParameters(Map.of(APPROVAL_ID_PATH_PARAMETER, approvalId.toString()))
          .withHeaders(Map.of(headerName, headerValue))
          .build();
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }

  private FetchApprovalHandler handlerFor(Approval approval) {
    return new FetchApprovalHandler(new FakeApprovalService(List.of(approval)), environment);
  }

  private static Approval approvalWithVersion(SortableIdentifier version) {
    var approval = randomApproval(UUID.randomUUID(), randomUri());
    return new Approval(
        approval.identifier(),
        approval.namedIdentifiers(),
        approval.source(),
        approval.handle(),
        approval.customerIdentifier(),
        version);
  }

  private static String quoted(SortableIdentifier version) {
    return "\"%s\"".formatted(version);
  }
}
