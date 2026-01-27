package no.sikt.nva.approvals.rest;

import static java.net.HttpURLConnection.HTTP_BAD_REQUEST;
import static java.net.HttpURLConnection.HTTP_NOT_FOUND;
import static java.net.HttpURLConnection.HTTP_OK;
import static no.sikt.nva.approvals.utils.TestUtils.randomApproval;
import static no.sikt.nva.approvals.utils.TestUtils.randomHandle;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static nva.commons.apigateway.ApiGatewayHandler.ALLOWED_ORIGIN_ENV;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.net.HttpHeaders;
import com.google.common.net.MediaType;
import gg.jte.ContentType;
import gg.jte.TemplateEngine;
import gg.jte.resolve.ResourceCodeResolver;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import no.sikt.nva.approvals.dmp.DmpClientException;
import no.sikt.nva.approvals.dmp.FakeDmpClient;
import no.sikt.nva.approvals.dmp.model.ClinicalTrial;
import no.sikt.nva.approvals.dmp.model.Investigator;
import no.sikt.nva.approvals.dmp.model.Sponsor;
import no.sikt.nva.approvals.dmp.model.TrialEvent;
import no.sikt.nva.approvals.dmp.model.TrialSite;
import no.sikt.nva.approvals.domain.Approval;
import no.sikt.nva.approvals.domain.FakeApprovalService;
import no.sikt.nva.approvals.domain.Handle;
import no.sikt.nva.approvals.domain.NamedIdentifier;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.stubs.FakeContext;
import no.unit.nva.testutils.HandlerRequestBuilder;
import nva.commons.apigateway.GatewayResponse;
import nva.commons.core.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class FetchApprovalHandlerTest {

    private static final Context CONTEXT = new FakeContext();
    private static final String APPROVAL_ID_PATH_PARAMETER = "approvalId";
    private static final String HANDLE_QUERY_PARAMETER = "handle";
    private static final String NAME_QUERY_PARAMETER = "name";
    private static final String VALUE_QUERY_PARAMETER = "value";
    private static final String VALID_HANDLE = "https://hdl.handle.net/11250.1/12345";
    private static final String API_HOST = "api.unittest.nva.unit.no";
    private static final String COGNITO_AUTHORIZER_URLS_ENV = "COGNITO_AUTHORIZER_URLS";
    private static final String API_HOST_ENV = "API_HOST";
    private FetchApprovalHandler handler;
    private ByteArrayOutputStream output;
    private Environment environment;
    private TemplateEngine templateEngine;

    @BeforeEach
    void setUp() {
        output = new ByteArrayOutputStream();
        environment = mock(Environment.class);
        templateEngine = createTemplateEngine();
        lenient().when(environment.readEnv(ALLOWED_ORIGIN_ENV)).thenReturn("*");
        lenient().when(environment.readEnv(COGNITO_AUTHORIZER_URLS_ENV)).thenReturn("http://localhost:3000");
        lenient().when(environment.readEnv(API_HOST_ENV)).thenReturn(API_HOST);
    }

    @Test
    void shouldReturnOkResponseWithApprovalOnSuccess() {
        var approvalId = UUID.randomUUID();
        var approval = new Approval(approvalId, List.of(new NamedIdentifier("test", "value")), randomUri(), randomHandle());
        handler = new FetchApprovalHandler(new FakeApprovalService(List.of(approval)), environment, templateEngine, new FakeDmpClient());
        var request = createRequestWithPathParameter(approvalId);

        var response = handleRequest(request);

        assertEquals(HTTP_OK, response.getStatusCode());
    }

    @Test
    void shouldReturnNotFoundWhenApprovalDoesNotExist() {
        var approvalId = UUID.randomUUID();
        handler = new FetchApprovalHandler(new FakeApprovalService(), environment, templateEngine, new FakeDmpClient());
        var request = createRequestWithPathParameter(approvalId);

        var response = handleRequest(request);

        assertEquals(HTTP_NOT_FOUND, response.getStatusCode());
    }

    @Test
    void shouldReturnBadRequestWhenApprovalIdIsInvalid() {
        handler = new FetchApprovalHandler(new FakeApprovalService(), environment, templateEngine, new FakeDmpClient());
        var request = createRequestWithInvalidId();

        var response = handleRequest(request);

        assertEquals(HTTP_BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void shouldReturnOkWhenLookingUpByHandle() {
        var handle = new Handle(java.net.URI.create(VALID_HANDLE));
        var approval = randomApproval(handle);
        handler = new FetchApprovalHandler(new FakeApprovalService(List.of(approval)), environment, templateEngine, new FakeDmpClient());
        var request = createRequestWithHandleQuery(VALID_HANDLE);

        var response = handleRequest(request);

        assertEquals(HTTP_OK, response.getStatusCode());
    }

    @Test
    void shouldReturnNotFoundWhenHandleLookupFindsNothing() {
        handler = new FetchApprovalHandler(new FakeApprovalService(), environment, templateEngine, new FakeDmpClient());
        var request = createRequestWithHandleQuery(VALID_HANDLE);

        var response = handleRequest(request);

        assertEquals(HTTP_NOT_FOUND, response.getStatusCode());
    }

    @Test
    void shouldReturnBadRequestWhenHandleIsInvalid() {
        handler = new FetchApprovalHandler(new FakeApprovalService(), environment, templateEngine, new FakeDmpClient());
        var request = createRequestWithHandleQuery("not-a-valid-handle");

        var response = handleRequest(request);

        assertEquals(HTTP_BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void shouldReturnOkWhenLookingUpByNamedIdentifier() {
        var namedIdentifier = new NamedIdentifier("doi", "10.1234/5678");
        var approval = randomApproval(namedIdentifier);
        handler = new FetchApprovalHandler(new FakeApprovalService(List.of(approval)), environment, templateEngine, new FakeDmpClient());
        var request = createRequestWithNamedIdentifierQuery("doi", "10.1234/5678");

        var response = handleRequest(request);

        assertEquals(HTTP_OK, response.getStatusCode());
    }

    @Test
    void shouldReturnNotFoundWhenNamedIdentifierLookupFindsNothing() {
        handler = new FetchApprovalHandler(new FakeApprovalService(), environment, templateEngine, new FakeDmpClient());
        var request = createRequestWithNamedIdentifierQuery("doi", "10.1234/5678");

        var response = handleRequest(request);

        assertEquals(HTTP_NOT_FOUND, response.getStatusCode());
    }

    @Test
    void shouldReturnBadRequestWhenOnlyNameIsProvided() {
        handler = new FetchApprovalHandler(new FakeApprovalService(), environment, templateEngine, new FakeDmpClient());
        var request = createRequestWithQueryParameters(Map.of(NAME_QUERY_PARAMETER, "doi"));

        var response = handleRequest(request);

        assertEquals(HTTP_BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void shouldReturnBadRequestWhenOnlyValueIsProvided() {
        handler = new FetchApprovalHandler(new FakeApprovalService(), environment, templateEngine, new FakeDmpClient());
        var request = createRequestWithQueryParameters(Map.of(VALUE_QUERY_PARAMETER, "10.1234/5678"));

        var response = handleRequest(request);

        assertEquals(HTTP_BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void shouldReturnBadRequestWhenNoQueryParametersProvided() {
        handler = new FetchApprovalHandler(new FakeApprovalService(), environment, templateEngine, new FakeDmpClient());
        var request = createRequestWithQueryParameters(Map.of());

        var response = handleRequest(request);

        assertEquals(HTTP_BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void shouldReturnBadRequestWhenBothPathParameterAndQueryParametersProvided() {
        handler = new FetchApprovalHandler(new FakeApprovalService(), environment, templateEngine, new FakeDmpClient());
        var request = createRequestWithPathAndQueryParameters(UUID.randomUUID(), VALID_HANDLE);

        var response = handleRequest(request);

        assertEquals(HTTP_BAD_REQUEST, response.getStatusCode());
    }

    @Test
    void shouldReturnHtmlWhenAcceptHeaderIsTextHtml() {
        var approvalId = UUID.randomUUID();
        var approval = new Approval(approvalId, List.of(new NamedIdentifier("CTIS", "CT-123")), randomUri(), randomHandle());
        handler = new FetchApprovalHandler(new FakeApprovalService(List.of(approval)), environment, templateEngine, new FakeDmpClient());
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
        var approval = new Approval(approvalId, List.of(new NamedIdentifier("test", "value")), randomUri(), randomHandle());
        handler = new FetchApprovalHandler(new FakeApprovalService(List.of(approval)), environment, templateEngine, new FakeDmpClient());
        var request = createRequestWithAcceptHeader(approvalId, MediaType.JSON_UTF_8.toString());

        var response = handleRequest(request);

        assertEquals(HTTP_OK, response.getStatusCode());
        assertThat(response.getHeaders().get(HttpHeaders.CONTENT_TYPE), containsString("application/json"));
    }

    @Test
    void shouldReturnJsonWhenNoAcceptHeaderProvided() {
        var approvalId = UUID.randomUUID();
        var approval = new Approval(approvalId, List.of(new NamedIdentifier("test", "value")), randomUri(), randomHandle());
        handler = new FetchApprovalHandler(new FakeApprovalService(List.of(approval)), environment, templateEngine, new FakeDmpClient());
        var request = createRequestWithPathParameter(approvalId);

        var response = handleRequest(request);

        assertEquals(HTTP_OK, response.getStatusCode());
        assertThat(response.getHeaders().get(HttpHeaders.CONTENT_TYPE), containsString("application/json"));
    }

    @Test
    void shouldReturnHtmlWhenBrowserAcceptHeaderProvided() {
        var browserAcceptHeader = "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8";
        var approvalId = UUID.randomUUID();
        var approval = new Approval(approvalId, List.of(new NamedIdentifier("test", "value")), randomUri(), randomHandle());
        handler = new FetchApprovalHandler(new FakeApprovalService(List.of(approval)), environment, templateEngine, new FakeDmpClient());
        var request = createRequestWithAcceptHeader(approvalId, browserAcceptHeader);

        var response = handleRequestAsString(request);

        assertEquals(HTTP_OK, response.getStatusCode());
        assertThat(response.getHeaders().get(HttpHeaders.CONTENT_TYPE), containsString("text/html"));
        assertThat(response.getBody(), containsString("<!DOCTYPE html>"));
    }

    @Test
    void shouldReturnOkWhenQueryParametersAreNull() {
        var approvalId = UUID.randomUUID();
        var approval = new Approval(approvalId, List.of(new NamedIdentifier("test", "value")), randomUri(), randomHandle());
        handler = new FetchApprovalHandler(new FakeApprovalService(List.of(approval)), environment, templateEngine, new FakeDmpClient());
        var request = createRequestWithPathParameterAndNullQueryParams(approvalId);

        var response = handleRequest(request);

        assertEquals(HTTP_OK, response.getStatusCode());
    }

    @Test
    void shouldReturnUnsupportedMediaTypeWhenAcceptHeaderIsUnsupported() {
        var approvalId = UUID.randomUUID();
        var approval = new Approval(approvalId, List.of(new NamedIdentifier("test", "value")), randomUri(), randomHandle());
        handler = new FetchApprovalHandler(new FakeApprovalService(List.of(approval)), environment, templateEngine, new FakeDmpClient());
        var request = createRequestWithAcceptHeader(approvalId, "application/xml");

        var response = handleRequest(request);

        assertEquals(415, response.getStatusCode());
    }

    @Test
    void shouldEnrichHtmlWithClinicalTrialDataWhenDmpIdentifierPresent() {
        var approvalId = UUID.randomUUID();
        var dmpIdentifier = "2022-500027-76-00";
        var approval = new Approval(approvalId, List.of(new NamedIdentifier("DMP", dmpIdentifier)), randomUri(), randomHandle());
        var clinicalTrial = createClinicalTrial(dmpIdentifier);
        var dmpClient = new FakeDmpClient(Map.of(dmpIdentifier, clinicalTrial));
        handler = new FetchApprovalHandler(new FakeApprovalService(List.of(approval)), environment, templateEngine, dmpClient);
        var request = createRequestWithAcceptHeader(approvalId, MediaType.HTML_UTF_8.toString());

        var response = handleRequestAsString(request);

        assertEquals(HTTP_OK, response.getStatusCode());
        assertThat(response.getBody(), containsString("Test Clinical Trial"));
        assertThat(response.getBody(), containsString("Test Hospital"));
        assertThat(response.getBody(), containsString("John"));
        assertThat(response.getBody(), containsString("Doe"));
    }

    @Test
    void shouldNotEnrichHtmlWhenNoDmpIdentifierPresent() {
        var approvalId = UUID.randomUUID();
        var approval = new Approval(approvalId, List.of(new NamedIdentifier("CTIS", "CT-123")), randomUri(), randomHandle());
        var clinicalTrial = createClinicalTrial("some-other-id");
        var dmpClient = new FakeDmpClient(Map.of("some-other-id", clinicalTrial));
        handler = new FetchApprovalHandler(new FakeApprovalService(List.of(approval)), environment, templateEngine, dmpClient);
        var request = createRequestWithAcceptHeader(approvalId, MediaType.HTML_UTF_8.toString());

        var response = handleRequestAsString(request);

        assertEquals(HTTP_OK, response.getStatusCode());
        assertThat(response.getBody(), not(containsString("Test Clinical Trial")));
    }

    @Test
    void shouldRenderBasicHtmlWhenDmpClientFails() {
        var approvalId = UUID.randomUUID();
        var dmpIdentifier = "2022-500027-76-00";
        var approval = new Approval(approvalId, List.of(new NamedIdentifier("DMP", dmpIdentifier)), randomUri(), randomHandle());
        var dmpClient = new FakeDmpClient(new DmpClientException("Connection failed"));
        handler = new FetchApprovalHandler(new FakeApprovalService(List.of(approval)), environment, templateEngine, dmpClient);
        var request = createRequestWithAcceptHeader(approvalId, MediaType.HTML_UTF_8.toString());

        var response = handleRequestAsString(request);

        assertEquals(HTTP_OK, response.getStatusCode());
        assertThat(response.getBody(), containsString("<!DOCTYPE html>"));
        assertThat(response.getBody(), not(containsString("Test Clinical Trial")));
    }

    @Test
    void shouldRenderBasicHtmlWhenClinicalTrialNotFoundInDmp() {
        var approvalId = UUID.randomUUID();
        var dmpIdentifier = "2022-500027-76-00";
        var approval = new Approval(approvalId, List.of(new NamedIdentifier("DMP", dmpIdentifier)), randomUri(), randomHandle());
        var dmpClient = new FakeDmpClient();
        handler = new FetchApprovalHandler(new FakeApprovalService(List.of(approval)), environment, templateEngine, dmpClient);
        var request = createRequestWithAcceptHeader(approvalId, MediaType.HTML_UTF_8.toString());

        var response = handleRequestAsString(request);

        assertEquals(HTTP_OK, response.getStatusCode());
        assertThat(response.getBody(), containsString("<!DOCTYPE html>"));
    }

    private ClinicalTrial createClinicalTrial(String identifier) {
        var events = List.of(new TrialEvent("TrialStart", "Norway", LocalDate.of(2022, 10, 5)));
        var sponsors = List.of(new Sponsor("Sponsor", "Test Hospital", "8", "Hospital", null));
        var investigator = new Investigator("Investigator", "123", "Prof.", "John", "Doe",
            "Oncology", null, URI.create("https://api.nva.unit.no/cristin/person/12345"));
        var trialSites = List.of(new TrialSite("TrialSite", "456", "Test Department",
            "Test Location", null, null, investigator));

        return new ClinicalTrial(
            URI.create("https://api.example.com/clinical-trial/" + identifier),
            identifier,
            URI.create("https://hdl.handle.net/11250.1/12345"),
            "Test Clinical Trial",
            events,
            sponsors,
            trialSites,
            null
        );
    }

    private GatewayResponse<ApprovalResponse> handleRequest(InputStream request) {
        try {
            handler.handleRequest(request, output, CONTEXT);
            return GatewayResponse.fromOutputStream(output, ApprovalResponse.class);
        } catch (IOException e) {
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

    private InputStream createRequestWithHandleQuery(String handle) {
        return createRequestWithQueryParameters(Map.of(HANDLE_QUERY_PARAMETER, handle));
    }

    private InputStream createRequestWithNamedIdentifierQuery(String name, String value) {
        return createRequestWithQueryParameters(Map.of(NAME_QUERY_PARAMETER, name, VALUE_QUERY_PARAMETER, value));
    }

    private InputStream createRequestWithQueryParameters(Map<String, String> queryParameters) {
        try {
            return new HandlerRequestBuilder<Void>(JsonUtils.dtoObjectMapper)
                .withQueryParameters(queryParameters)
                                .build();
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    private InputStream createRequestWithPathAndQueryParameters(UUID approvalId, String handle) {
        try {
            return new HandlerRequestBuilder<Void>(JsonUtils.dtoObjectMapper)
                .withPathParameters(Map.of(APPROVAL_ID_PATH_PARAMETER, approvalId.toString()))
                .withQueryParameters(Map.of(HANDLE_QUERY_PARAMETER, handle))
                                .build();
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    private InputStream createRequestWithAcceptHeader(UUID approvalId, String acceptHeader) {
        try {
            return new HandlerRequestBuilder<Void>(JsonUtils.dtoObjectMapper)
                       .withPathParameters(Map.of(APPROVAL_ID_PATH_PARAMETER, approvalId.toString()))
                       .withHeaders(Map.of(HttpHeaders.ACCEPT, acceptHeader))
                       .build();
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    private InputStream createRequestWithPathParameterAndNullQueryParams(UUID approvalId) {
        try {
            return new HandlerRequestBuilder<Void>(JsonUtils.dtoObjectMapper)
                       .withPathParameters(Map.of(APPROVAL_ID_PATH_PARAMETER, approvalId.toString()))
                       .withQueryParameters(null)
                       .build();
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    private static TemplateEngine createTemplateEngine() {
        var codeResolver = new ResourceCodeResolver("jte");
        return TemplateEngine.create(codeResolver, ContentType.Html);
    }
}
