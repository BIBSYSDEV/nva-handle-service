package no.sikt.nva.approvals.rest;

import static no.sikt.nva.approvals.index.SourceViewGenerator.CURRENT_VIEW_VERSION;
import static no.sikt.nva.approvals.utils.JsonLdTestUtils.APPROVAL_ONTOLOGY_NAMESPACE;
import static no.sikt.nva.approvals.utils.JsonLdTestUtils.expandWithServedContext;
import static no.sikt.nva.approvals.utils.TestUtils.randomSourceSnapshotWithContent;
import static nva.commons.core.ioutils.IoUtils.stringFromResources;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.apicatalog.jsonld.JsonLdError;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.json.JsonArray;
import jakarta.json.JsonObject;
import jakarta.json.JsonString;
import jakarta.json.JsonValue;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Stream;
import no.sikt.nva.approvals.domain.Content;
import no.sikt.nva.approvals.domain.SourceViewService;
import no.sikt.nva.approvals.domain.StoredSourceView;
import no.sikt.nva.approvals.index.SourceViewGenerator;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.stubs.FakeContext;
import no.unit.nva.testutils.HandlerRequestBuilder;
import nva.commons.apigateway.GatewayResponse;
import nva.commons.core.Environment;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class ViewContextExpansionTest {

  private static final String SCHEMA = "https://schema.org/";
  private static final String PROV_WAS_DERIVED_FROM = "http://www.w3.org/ns/prov#wasDerivedFrom";
  private static final String PROV_AGENT = "http://www.w3.org/ns/prov#agent";
  private static final String ID_KEYWORD = "@id";
  private static final String TYPE_KEYWORD = "@type";
  private static final String CONTEXT_KEYWORD = "@context";
  private static final String VALUE_KEYWORD = "@value";
  private static final String ONTOLOGY_FILE = "ontology/approval-ontology-1.0.0.ttl";
  private static final boolean NOT_UNDER_EVENT = false;
  private static final String JSON_LD_MEDIA_TYPE = "application/ld+json";
  private static final String CLINICAL_TRIAL_FILE = "clinical-trial.json";
  private static final String APPROVAL_ID_PATH_PARAMETER = "approvalId";
  private static final Instant SAVED = Instant.parse("2026-11-20T10:00:00Z");
  private static String viewDocument;

  @BeforeAll
  static void fetchViewOfClinicalTrialSource() throws Exception {
    var content =
        Content.create(JSON_LD_MEDIA_TYPE, stringFromResources(Path.of(CLINICAL_TRIAL_FILE)));
    var snapshot = randomSourceSnapshotWithContent(content);
    var storedView =
        new StoredSourceView(
            snapshot.approvalIdentifier(),
            snapshot.identifier(),
            CURRENT_VIEW_VERSION,
            new SourceViewGenerator().generate(snapshot),
            SAVED,
            SAVED);
    viewDocument = withoutContext(fetchView(storedView));
  }

  @Test
  void shouldExpandViewFieldsWithoutApprovalMeanings() throws IOException, JsonLdError {
    var view = expandWithServedContext(viewDocument);

    assertThat(
        view.keySet(),
        containsInAnyOrder(
            ID_KEYWORD,
            TYPE_KEYWORD,
            PROV_WAS_DERIVED_FROM,
            schema("identifier"),
            schema("name"),
            schema("dateModified"),
            ontology("regionStatus"),
            ontology("event"),
            schema("sponsor"),
            ontology("trialSite")));
  }

  @Test
  void shouldExpandSponsorNameAsNameAndNotAsIdentifierNamespace() throws IOException, JsonLdError {
    var sponsor = firstNode(expandWithServedContext(viewDocument), schema("sponsor"));

    assertThat(sponsor.keySet(), containsInAnyOrder(TYPE_KEYWORD, schema("name")));
  }

  @Test
  void shouldExpandInvestigatorTitleAsHonorificPrefix() throws IOException, JsonLdError {
    var trialSite = firstNode(expandWithServedContext(viewDocument), ontology("trialSite"));
    var investigator = firstNode(trialSite, ontology("investigator"));

    assertThat(
        investigator.keySet(),
        containsInAnyOrder(
            TYPE_KEYWORD,
            PROV_AGENT,
            schema("honorificPrefix"),
            schema("givenName"),
            schema("familyName"),
            ontology("department")));
  }

  @Test
  void shouldLinkTrialSiteAndInvestigatorToTheirAgents() throws IOException, JsonLdError {
    var trialSite = firstNode(expandWithServedContext(viewDocument), ontology("trialSite"));
    var organization = firstNode(trialSite, PROV_AGENT);
    var person = firstNode(firstNode(trialSite, ontology("investigator")), PROV_AGENT);

    assertEquals(schema("Organization"), organization.getJsonArray(TYPE_KEYWORD).getString(0));
    assertEquals(schema("Person"), person.getJsonArray(TYPE_KEYWORD).getString(0));
  }

  @Test
  void shouldExpandStatusAsRegionAndStatus() throws IOException, JsonLdError {
    var status = firstNode(expandWithServedContext(viewDocument), ontology("regionStatus"));

    assertThat(
        status.keySet(), containsInAnyOrder(TYPE_KEYWORD, ontology("region"), ontology("status")));
  }

  @Test
  void shouldDeclareEveryApprovalTermOfTheViewInTheOntology() throws IOException, JsonLdError {
    var ontology = stringFromResources(Path.of(ONTOLOGY_FILE));

    var undeclaredTerms =
        approvalTerms(expandWithServedContext(viewDocument), NOT_UNDER_EVENT)
            .map(ViewContextExpansionTest::localName)
            .distinct()
            .filter(term -> !ontology.contains(":%s a ".formatted(term)))
            .toList();

    assertThat(undeclaredTerms, is(empty()));
  }

  private static Stream<String> approvalTerms(JsonValue value, boolean underEvent) {
    return switch (value) {
      case JsonObject node -> approvalTermsOfNode(node, underEvent);
      case JsonArray values -> values.stream().flatMap(item -> approvalTerms(item, underEvent));
      default -> Stream.empty();
    };
  }

  private static Stream<String> approvalTermsOfNode(JsonObject node, boolean underEvent) {
    if (node.containsKey(VALUE_KEYWORD)) {
      return Stream.empty();
    }
    var properties = node.keySet().stream().filter(ViewContextExpansionTest::isApprovalTerm);
    var types = underEvent ? Stream.<String>empty() : approvalTypesOf(node);
    var nested =
        node.entrySet().stream()
            .flatMap(entry -> approvalTerms(entry.getValue(), isEvent(entry.getKey())));
    return Stream.of(properties, types, nested).flatMap(Function.identity());
  }

  private static Stream<String> approvalTypesOf(JsonObject node) {
    return node.containsKey(TYPE_KEYWORD)
        ? node.getJsonArray(TYPE_KEYWORD).getValuesAs(JsonString.class).stream()
            .map(JsonString::getString)
            .filter(ViewContextExpansionTest::isApprovalTerm)
        : Stream.empty();
  }

  private static boolean isEvent(String property) {
    return ontology("event").equals(property);
  }

  private static boolean isApprovalTerm(String iri) {
    return iri.startsWith(APPROVAL_ONTOLOGY_NAMESPACE);
  }

  private static String localName(String iri) {
    return iri.substring(APPROVAL_ONTOLOGY_NAMESPACE.length());
  }

  private static String fetchView(StoredSourceView storedView) throws IOException {
    var sourceViewService = mock(SourceViewService.class);
    when(sourceViewService.fetchStoredView(storedView.approvalIdentifier()))
        .thenReturn(Optional.of(storedView));
    var output = new ByteArrayOutputStream();
    new FetchApprovalViewHandler(sourceViewService, new Environment())
        .handleRequest(request(storedView.approvalIdentifier()), output, new FakeContext());
    return GatewayResponse.fromOutputStream(output, String.class).getBody();
  }

  private static InputStream request(UUID approvalIdentifier) throws IOException {
    return new HandlerRequestBuilder<Void>(JsonUtils.dtoObjectMapper)
        .withPathParameters(Map.of(APPROVAL_ID_PATH_PARAMETER, approvalIdentifier.toString()))
        .build();
  }

  private static String withoutContext(String body) throws IOException {
    var view = (ObjectNode) JsonUtils.dtoObjectMapper.readTree(body);
    view.remove(CONTEXT_KEYWORD);
    return view.toString();
  }

  private static JsonObject firstNode(JsonObject node, String property) {
    return node.getJsonArray(property).getJsonObject(0);
  }

  private static String schema(String term) {
    return "%s%s".formatted(SCHEMA, term);
  }

  private static String ontology(String term) {
    return "%s%s".formatted(APPROVAL_ONTOLOGY_NAMESPACE, term);
  }
}
