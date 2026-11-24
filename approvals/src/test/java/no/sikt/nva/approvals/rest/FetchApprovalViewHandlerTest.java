package no.sikt.nva.approvals.rest;

import static java.net.HttpURLConnection.HTTP_NOT_FOUND;
import static java.net.HttpURLConnection.HTTP_NOT_MODIFIED;
import static java.net.HttpURLConnection.HTTP_OK;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static nva.commons.apigateway.ApiGatewayHandler.ALLOWED_ORIGIN_ENV;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import no.sikt.nva.approvals.domain.ClinicalTrialView;
import no.sikt.nva.approvals.domain.ClinicalTrialView.Investigator;
import no.sikt.nva.approvals.domain.ClinicalTrialView.Organization;
import no.sikt.nva.approvals.domain.ClinicalTrialView.Person;
import no.sikt.nva.approvals.domain.ClinicalTrialView.TrialSite;
import no.sikt.nva.approvals.domain.SourceViewService;
import no.sikt.nva.approvals.domain.StoredSourceView;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.identifiers.OrganizationIdentifier;
import no.unit.nva.identifiers.PersonIdentifier;
import no.unit.nva.identifiers.SortableIdentifier;
import no.unit.nva.stubs.FakeContext;
import no.unit.nva.testutils.HandlerRequestBuilder;
import nva.commons.apigateway.GatewayResponse;
import nva.commons.core.Environment;
import org.apache.hc.core5.http.HttpHeaders;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class FetchApprovalViewHandlerTest {

  private static final FakeContext CONTEXT = new FakeContext();
  private static final String APPROVAL_ID_PATH_PARAMETER = "approvalId";
  private static final String API_HOST = "api.unittest.nva.unit.no";
  private static final String API_HOST_ENV = "API_HOST";
  private static final String COGNITO_AUTHORIZER_URLS_ENV = "COGNITO_AUTHORIZER_URLS";
  private static final Instant SAVED = Instant.parse("2026-11-20T10:00:00Z");
  private static final int VIEW_VERSION = 1;
  private static final OrganizationIdentifier ORGANIZATION =
      new OrganizationIdentifier("1936.0.0.0");
  private static final PersonIdentifier PERSON = new PersonIdentifier("47186");
  private static final String CLINICAL_TRIAL_VIEW_TYPE = "ClinicalTrialView";

  private SourceViewService sourceViewService;
  private FetchApprovalViewHandler handler;
  private ByteArrayOutputStream output;

  @BeforeEach
  void setUp() {
    output = new ByteArrayOutputStream();
    sourceViewService = mock(SourceViewService.class);
    var environment = mock(Environment.class);
    lenient().when(environment.readEnv(ALLOWED_ORIGIN_ENV)).thenReturn("*");
    lenient()
        .when(environment.readEnv(COGNITO_AUTHORIZER_URLS_ENV))
        .thenReturn("http://localhost:3000");
    lenient().when(environment.readEnv(API_HOST_ENV)).thenReturn(API_HOST);
    handler = new FetchApprovalViewHandler(sourceViewService, environment);
  }

  @Test
  void shouldReturnViewIdentifiedByItsOwnUriAndDerivedFromItsSnapshot() throws IOException {
    var storedView = givenStoredView();

    var body = bodyOf(send(request(storedView.approvalIdentifier(), Map.of())));

    var approvalUri = "https://%s/approval/%s".formatted(API_HOST, storedView.approvalIdentifier());
    assertEquals(CLINICAL_TRIAL_VIEW_TYPE, body.get("type").asText());
    assertEquals("%s/view".formatted(approvalUri), body.get("id").asText());
    assertEquals(
        "%s/change/%s".formatted(approvalUri, storedView.snapshotIdentifier()),
        body.get("wasDerivedFrom").asText());
    assertEquals(((ClinicalTrialView) storedView.view()).title(), body.get("title").asText());
  }

  @Test
  void shouldReturnJsonLdWithContextOfTheApprovalApi() throws IOException {
    var storedView = givenStoredView();

    var response =
        send(
            request(
                storedView.approvalIdentifier(),
                Map.of(HttpHeaders.ACCEPT, "application/ld+json")));

    assertEquals(
        "https://%s/approval/context/v1".formatted(API_HOST),
        bodyOf(response).get("@context").asText());
  }

  @Test
  void shouldIdentifyOrganizationAndPersonWithUrisOfThisEnvironment() throws IOException {
    var storedView = givenStoredView();

    var trialSite =
        bodyOf(send(request(storedView.approvalIdentifier(), Map.of()))).at("/trialSites/0");

    assertEquals(
        "https://%s/cristin/organization/%s".formatted(API_HOST, ORGANIZATION.value()),
        trialSite.at("/organization/id").asText());
    assertEquals(
        "https://%s/cristin/person/%s".formatted(API_HOST, PERSON.value()),
        trialSite.at("/investigator/person/id").asText());
  }

  @Test
  void shouldReturnEntityTagOfSnapshotAndViewVersion() throws IOException {
    var storedView = givenStoredView();

    var response = send(request(storedView.approvalIdentifier(), Map.of()));

    assertEquals(
        "\"%s.%d\"".formatted(storedView.snapshotIdentifier(), VIEW_VERSION),
        response.getHeaders().get(HttpHeaders.ETAG));
  }

  @Test
  void shouldReturnNotModifiedWhenEntityTagMatches() throws IOException {
    var storedView = givenStoredView();
    var entityTag = "\"%s.%d\"".formatted(storedView.snapshotIdentifier(), VIEW_VERSION);

    var response =
        send(
            request(storedView.approvalIdentifier(), Map.of(HttpHeaders.IF_NONE_MATCH, entityTag)));

    assertEquals(HTTP_NOT_MODIFIED, response.getStatusCode());
  }

  @Test
  void shouldReturnNotFoundWhenApprovalHasNoView() throws IOException {
    var approvalIdentifier = UUID.randomUUID();
    when(sourceViewService.fetchStoredView(approvalIdentifier)).thenReturn(Optional.empty());

    var response = send(request(approvalIdentifier, Map.of()));

    assertEquals(HTTP_NOT_FOUND, response.getStatusCode());
  }

  private StoredSourceView givenStoredView() {
    var trialSite =
        new TrialSite(
            new Organization(ORGANIZATION),
            randomString(),
            new Investigator(new Person(PERSON), null, null, null, null));
    var view =
        new ClinicalTrialView(
            randomString(), randomString(), null, null, List.of(), List.of(), List.of(trialSite));
    var storedView =
        new StoredSourceView(
            UUID.randomUUID(), SortableIdentifier.next(), VIEW_VERSION, view, SAVED, SAVED);
    when(sourceViewService.fetchStoredView(storedView.approvalIdentifier()))
        .thenReturn(Optional.of(storedView));
    return storedView;
  }

  private GatewayResponse<String> send(InputStream request) throws IOException {
    handler.handleRequest(request, output, CONTEXT);
    return GatewayResponse.fromOutputStream(output, String.class);
  }

  private static JsonNode bodyOf(GatewayResponse<String> response) throws IOException {
    assertEquals(HTTP_OK, response.getStatusCode());
    return JsonUtils.dtoObjectMapper.readTree(response.getBody());
  }

  private static InputStream request(UUID approvalIdentifier, Map<String, String> headers)
      throws IOException {
    return new HandlerRequestBuilder<Void>(JsonUtils.dtoObjectMapper)
        .withPathParameters(Map.of(APPROVAL_ID_PATH_PARAMETER, approvalIdentifier.toString()))
        .withHeaders(headers)
        .build();
  }
}
