package no.sikt.nva.approvals.rest;

import static java.net.HttpURLConnection.HTTP_BAD_REQUEST;
import static java.net.HttpURLConnection.HTTP_INTERNAL_ERROR;
import static java.net.HttpURLConnection.HTTP_NOT_FOUND;
import static java.net.HttpURLConnection.HTTP_NOT_MODIFIED;
import static java.net.HttpURLConnection.HTTP_OK;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Objects.nonNull;
import static no.sikt.nva.approvals.utils.TestUtils.randomApproval;
import static no.sikt.nva.approvals.utils.TestUtils.randomHandle;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static nva.commons.apigateway.ApiGatewayHandler.ALLOWED_ORIGIN_ENV;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.emptyOrNullString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;

import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.sun.net.httpserver.HttpServer;
import gg.jte.ContentType;
import gg.jte.TemplateEngine;
import gg.jte.resolve.ResourceCodeResolver;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import no.sikt.nva.approvals.dmp.model.ClinicalTrial;
import no.sikt.nva.approvals.dmp.model.Investigator;
import no.sikt.nva.approvals.dmp.model.Sponsor;
import no.sikt.nva.approvals.dmp.model.TrialEvent;
import no.sikt.nva.approvals.dmp.model.TrialSite;
import no.sikt.nva.approvals.domain.Approval;
import no.sikt.nva.approvals.domain.FakeApprovalService;
import no.sikt.nva.approvals.domain.IdentifierPolicy;
import no.sikt.nva.approvals.domain.IdentifierPolicyService;
import no.sikt.nva.approvals.domain.NoAuthentication;
import no.sikt.nva.approvals.domain.SourceConfig;
import no.sikt.nva.approvals.source.SourceClient;
import no.sikt.nva.approvals.source.SourceCredentials;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.stubs.FakeContext;
import no.unit.nva.testutils.HandlerRequestBuilder;
import nva.commons.apigateway.GatewayResponse;
import nva.commons.apigateway.MediaType;
import nva.commons.core.Environment;
import org.apache.hc.core5.http.HttpHeaders;
import org.junit.jupiter.api.AfterEach;
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
  private static final String CLINICAL_TRIAL_TITLE = "Test Clinical Trial";
  private static final String NOT_A_CLINICAL_TRIAL = "[]";
  private static final String SOURCE_PATH = "/trials";
  private static final String INITIAL_ENTITY_TAG = "\"0\"";
  private FetchApprovalHandler handler;
  private SourceClient sourceClient;
  private HttpServer server;
  private ByteArrayOutputStream output;
  private Environment environment;
  private TemplateEngine templateEngine;

  @BeforeEach
  void setUp() {
    output = new ByteArrayOutputStream();
    environment = mock(Environment.class);
    templateEngine = createTemplateEngine();
    sourceClient = sourceClientFor(customerIdentifier -> IdentifierPolicy.DENY_ALL);
    lenient().when(environment.readEnv(ALLOWED_ORIGIN_ENV)).thenReturn("*");
    lenient()
        .when(environment.readEnv(COGNITO_AUTHORIZER_URLS_ENV))
        .thenReturn("http://localhost:3000");
    lenient().when(environment.readEnv(API_HOST_ENV)).thenReturn(API_HOST);
  }

  @AfterEach
  void tearDown() {
    if (nonNull(server)) {
      server.stop(0);
    }
  }

  @Test
  void shouldReturnOkResponseWithApprovalOnSuccess() {
    var approvalId = UUID.randomUUID();
    var approval = randomApproval(approvalId, randomUri());
    handler =
        new FetchApprovalHandler(
            new FakeApprovalService(List.of(approval)), environment, templateEngine, sourceClient);
    var request = createRequestWithPathParameter(approvalId);

    var response = handleRequest(request);

    assertEquals(HTTP_OK, response.getStatusCode());
  }

  @Test
  void shouldReturnNotFoundWhenApprovalDoesNotExist() {
    var approvalId = UUID.randomUUID();
    handler =
        new FetchApprovalHandler(
            new FakeApprovalService(), environment, templateEngine, sourceClient);
    var request = createRequestWithPathParameter(approvalId);

    var response = handleRequest(request);

    assertEquals(HTTP_NOT_FOUND, response.getStatusCode());
  }

  @Test
  void shouldReturnBadRequestWhenApprovalIdIsInvalid() {
    handler =
        new FetchApprovalHandler(
            new FakeApprovalService(), environment, templateEngine, sourceClient);
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
  void shouldReturnHtmlWhenAcceptHeaderIsTextHtml() {
    var approvalId = UUID.randomUUID();
    var approval = randomApproval(approvalId, randomUri());
    handler =
        new FetchApprovalHandler(
            new FakeApprovalService(List.of(approval)), environment, templateEngine, sourceClient);
    var request = createRequestWithAcceptHeader(approvalId, MediaType.HTML_UTF_8.toString());

    var response = handleRequestAsString(request);

    assertEquals(HTTP_OK, response.getStatusCode());
    assertThat(response.getHeaders().get(HttpHeaders.CONTENT_TYPE), containsString("text/html"));
    assertThat(response.getBody(), containsString("<!DOCTYPE html>"));
    assertThat(response.getBody(), containsString("Approval"));
  }

  @Test
  void shouldReturnJsonWhenAcceptHeaderIsApplicationJson() {
    var approvalId = UUID.randomUUID();
    var approval = randomApproval(approvalId, randomUri());
    handler =
        new FetchApprovalHandler(
            new FakeApprovalService(List.of(approval)), environment, templateEngine, sourceClient);
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
    handler =
        new FetchApprovalHandler(
            new FakeApprovalService(List.of(approval)), environment, templateEngine, sourceClient);
    var request = createRequestWithPathParameter(approvalId);

    var response = handleRequest(request);

    assertEquals(HTTP_OK, response.getStatusCode());
    assertThat(
        response.getHeaders().get(HttpHeaders.CONTENT_TYPE), containsString("application/json"));
  }

  @Test
  void shouldReturnHtmlWhenBrowserAcceptHeaderProvided() {
    var browserAcceptHeader = "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8";
    var approvalId = UUID.randomUUID();
    var approval = randomApproval(approvalId, randomUri());
    handler =
        new FetchApprovalHandler(
            new FakeApprovalService(List.of(approval)), environment, templateEngine, sourceClient);
    var request = createRequestWithAcceptHeader(approvalId, browserAcceptHeader);

    var response = handleRequestAsString(request);

    assertEquals(HTTP_OK, response.getStatusCode());
    assertThat(response.getHeaders().get(HttpHeaders.CONTENT_TYPE), containsString("text/html"));
    assertThat(response.getBody(), containsString("<!DOCTYPE html>"));
  }

  @Test
  void shouldReturnNotAcceptableWhenAcceptHeaderIsUnsupported() {
    var approvalId = UUID.randomUUID();
    var approval = randomApproval(approvalId, randomUri());
    handler =
        new FetchApprovalHandler(
            new FakeApprovalService(List.of(approval)), environment, templateEngine, sourceClient);
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

  @Test
  void shouldReturnInitialEntityTagWhenApprovalHasNoVersion() {
    var approval = randomApproval(UUID.randomUUID(), randomUri());
    handler = handlerFor(approval);

    var response = handleRequest(createRequestWithPathParameter(approval.identifier()));

    assertEquals(INITIAL_ENTITY_TAG, response.getHeaders().get(HttpHeaders.ETAG));
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

  @Test
  void shouldReturnOkWhenIfNoneMatchDoesNotMatch() {
    var approval = approvalWithVersion(SortableIdentifier.next());
    handler = handlerFor(approval);
    var staleEntityTag = quoted(SortableIdentifier.next());

    var response =
        handleRequest(createRequestWithIfNoneMatch(approval.identifier(), staleEntityTag));

    assertEquals(HTTP_OK, response.getStatusCode());
  }

  @Test
  void shouldNotReturnEntityTagForHtmlAfterJsonRequest() {
    var approval = approvalWithVersion(SortableIdentifier.next());
    handler = handlerFor(approval);
    handleRequest(createRequestWithPathParameter(approval.identifier()));
    output = new ByteArrayOutputStream();

    var response =
        handleRequestAsString(
            createRequestWithAcceptHeader(approval.identifier(), MediaType.HTML_UTF_8.toString()));

    assertThat(response.getHeaders().get(HttpHeaders.ETAG), is(nullValue()));
  }

  @Test
  void shouldEnrichHtmlWithClinicalTrialWhenCustomerHasSourceConfigForSource() throws IOException {
    var source = serveSource(HTTP_OK, clinicalTrialJson());
    var approval = approvalWithSource(source);
    handler = handlerForCustomerWithSourceConfig(approval);

    var response = requestHtml(approval);

    assertThat(response.getBody(), containsString(CLINICAL_TRIAL_TITLE));
  }

  @Test
  void shouldRenderBasicHtmlWhenCustomerHasNoSourceConfigForSource() throws IOException {
    var source = serveSource(HTTP_OK, clinicalTrialJson());
    var approval = approvalWithSource(source);
    handler =
        new FetchApprovalHandler(
            new FakeApprovalService(List.of(approval)), environment, templateEngine, sourceClient);

    var response = requestHtml(approval);

    assertThat(response.getBody(), not(containsString(CLINICAL_TRIAL_TITLE)));
  }

  @Test
  void shouldRenderBasicHtmlWhenSourceFails() throws IOException {
    var source = serveSource(HTTP_INTERNAL_ERROR, clinicalTrialJson());
    var approval = approvalWithSource(source);
    handler = handlerForCustomerWithSourceConfig(approval);

    var response = requestHtml(approval);

    assertEquals(HTTP_OK, response.getStatusCode());
    assertThat(response.getBody(), not(containsString(CLINICAL_TRIAL_TITLE)));
  }

  @Test
  void shouldRenderBasicHtmlWhenSourceIsNotFound() throws IOException {
    var source = serveSource(HTTP_NOT_FOUND, clinicalTrialJson());
    var approval = approvalWithSource(source);
    handler = handlerForCustomerWithSourceConfig(approval);

    var response = requestHtml(approval);

    assertEquals(HTTP_OK, response.getStatusCode());
    assertThat(response.getBody(), containsString("<!DOCTYPE html>"));
  }

  @Test
  void shouldRenderBasicHtmlWhenSourceIsNotClinicalTrial() throws IOException {
    var source = serveSource(HTTP_OK, NOT_A_CLINICAL_TRIAL);
    var approval = approvalWithSource(source);
    handler = handlerForCustomerWithSourceConfig(approval);

    var response = requestHtml(approval);

    assertEquals(HTTP_OK, response.getStatusCode());
    assertThat(response.getBody(), not(containsString(CLINICAL_TRIAL_TITLE)));
  }

  private URI serveSource(int status, String body) throws IOException {
    server = HttpServer.create(new InetSocketAddress(0), 0);
    server.createContext(
        SOURCE_PATH,
        exchange -> {
          var bytes = body.getBytes(UTF_8);
          exchange.sendResponseHeaders(status, bytes.length);
          try (var responseBody = exchange.getResponseBody()) {
            responseBody.write(bytes);
          }
        });
    server.start();
    return URI.create(
        "http://localhost:" + server.getAddress().getPort() + SOURCE_PATH + "/" + randomString());
  }

  private static Approval approvalWithSource(URI source) {
    return randomApproval(source, randomHandle(), UUID.randomUUID());
  }

  private FetchApprovalHandler handlerForCustomerWithSourceConfig(Approval approval) {
    var baseUri = approval.source().resolve(SOURCE_PATH);
    var identifierPolicy =
        new IdentifierPolicy(Set.of(), List.of(new SourceConfig(baseUri, new NoAuthentication())));
    return new FetchApprovalHandler(
        new FakeApprovalService(List.of(approval)),
        environment,
        templateEngine,
        sourceClientFor(
            customerIdentifier ->
                approval.customerIdentifier().equals(customerIdentifier)
                    ? identifierPolicy
                    : IdentifierPolicy.DENY_ALL));
  }

  private static SourceClient sourceClientFor(IdentifierPolicyService identifierPolicyService) {
    return new SourceClient(
        identifierPolicyService, () -> new SourceCredentials(Map.of()), HttpClient.newHttpClient());
  }

  private GatewayResponse<String> requestHtml(Approval approval) {
    return handleRequestAsString(
        createRequestWithAcceptHeader(approval.identifier(), MediaType.HTML_UTF_8.toString()));
  }

  private String clinicalTrialJson() throws IOException {
    return JsonUtils.dtoObjectMapper.writeValueAsString(createClinicalTrial(randomString()));
  }

  private ClinicalTrial createClinicalTrial(String identifier) {
    var events = List.of(new TrialEvent("TrialStart", "Norway", LocalDate.of(2022, 10, 5)));
    var sponsors = List.of(new Sponsor("Sponsor", "Test Hospital", "8", "Hospital", null));
    var investigator =
        new Investigator(
            "Investigator",
            "123",
            "Prof.",
            "John",
            "Doe",
            "Oncology",
            null,
            URI.create("https://api.nva.unit.no/cristin/person/12345"));
    var trialSites =
        List.of(
            new TrialSite(
                "TrialSite", "456", "Test Department", "Test Location", null, null, investigator));

    return new ClinicalTrial(
        randomUri(),
        URI.create("https://api.example.com/clinical-trial/" + identifier),
        identifier,
        URI.create("https://hdl.handle.net/11250.1/12345"),
        CLINICAL_TRIAL_TITLE,
        events,
        sponsors,
        trialSites,
        null);
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
    return new FetchApprovalHandler(
        new FakeApprovalService(List.of(approval)), environment, templateEngine, sourceClient);
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

  private static TemplateEngine createTemplateEngine() {
    var codeResolver = new ResourceCodeResolver("jte");
    return TemplateEngine.create(codeResolver, ContentType.Html);
  }
}
