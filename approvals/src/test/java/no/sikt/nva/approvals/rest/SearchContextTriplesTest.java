package no.sikt.nva.approvals.rest;

import static java.util.UUID.randomUUID;
import static no.sikt.nva.approvals.utils.JsonLdTestUtils.APPROVAL_ONTOLOGY_NAMESPACE;
import static no.sikt.nva.approvals.utils.JsonLdTestUtils.HYDRA_NAMESPACE;
import static no.sikt.nva.approvals.utils.JsonLdTestUtils.RDF_TYPE;
import static no.sikt.nva.approvals.utils.JsonLdTestUtils.objectOf;
import static no.sikt.nva.approvals.utils.JsonLdTestUtils.toTriplesWithApprovalContext;
import static no.sikt.nva.approvals.utils.UriConstants.approvalId;
import static no.sikt.nva.approvals.utils.UriConstants.context;
import static no.sikt.nva.approvals.utils.UriConstants.searchId;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;

import com.apicatalog.jsonld.JsonLdError;
import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import no.sikt.nva.approvals.domain.NamedIdentifier;
import no.sikt.nva.approvals.index.IndexDocument;
import no.sikt.nva.approvals.index.Query;
import no.sikt.nva.approvals.utils.JsonLdTestUtils.Triple;
import nva.commons.core.paths.UriWrapper;
import org.junit.jupiter.api.Test;
import org.opensearch.client.opensearch.core.SearchResponse;
import org.opensearch.client.opensearch.core.search.TotalHitsRelation;

class SearchContextTriplesTest {

  private static final String API_HOST = "localhost";
  private static final String XSD_INTEGER = "http://www.w3.org/2001/XMLSchema#integer";
  private static final int TOTAL_ITEMS = 42;
  private static final String SEARCH_TEMPLATE =
      "https://localhost/approval{?query,person,size,cursor}";

  @Test
  void shouldReturnTriplesOfSearchResultsTypeAndItsHits() throws IOException, JsonLdError {
    var hit = approvalHit();
    var results = searchResults(List.of(hit), null);

    var triples = toTriplesWithApprovalContext(results);

    assertThat(
        triples,
        hasItems(
            Triple.of(searchIri(), RDF_TYPE, term("ApprovalSearchResults")),
            Triple.of(searchIri(), hydra("member"), hit.id().toString())));
  }

  @Test
  void shouldReturnTripleOfHitTypedAsApproval() throws IOException, JsonLdError {
    var hit = approvalHit();

    var triples = toTriplesWithApprovalContext(searchResults(List.of(hit), null));

    assertThat(triples, hasItem(Triple.of(hit.id().toString(), RDF_TYPE, term("Approval"))));
  }

  @Test
  void shouldReturnTripleOfTotalItemsAsInteger() throws IOException, JsonLdError {
    var results = searchResults(List.of(), null);

    var triples = toTriplesWithApprovalContext(results);

    assertThat(
        triples,
        hasItem(
            Triple.typed(
                searchIri(), hydra("totalItems"), String.valueOf(TOTAL_ITEMS), XSD_INTEGER)));
  }

  @Test
  void shouldReturnTriplesOfSearchTemplateAndItsFreeTextMapping() throws IOException, JsonLdError {
    var triples = toTriplesWithApprovalContext(searchResults(List.of(), null));

    var template = objectOf(triples, searchIri(), hydra("search"));

    assertThat(
        triples,
        hasItems(
            Triple.literal(template, hydra("template"), SEARCH_TEMPLATE),
            Triple.of(
                mappingFor(triples, template, "query"),
                hydra("property"),
                hydra("freetextQuery"))));
  }

  @Test
  void shouldReturnTriplesOfSizeAndCursorMappings() throws IOException, JsonLdError {
    var triples = toTriplesWithApprovalContext(searchResults(List.of(), null));

    var template = objectOf(triples, searchIri(), hydra("search"));

    assertThat(
        triples,
        hasItems(
            Triple.of(mappingFor(triples, template, "size"), hydra("property"), hydra("limit")),
            Triple.of(
                mappingFor(triples, template, "cursor"),
                hydra("property"),
                hydra("pageReference"))));
  }

  @Test
  void shouldReturnTripleOfHitTitle() throws IOException, JsonLdError {
    var hit = approvalHit();

    var triples = toTriplesWithApprovalContext(searchResults(List.of(hit), null));

    assertThat(triples, hasItem(Triple.literal(hit.id().toString(), term("title"), hit.title())));
  }

  @Test
  void shouldReturnTripleOfNextPageOfSearchResults() throws IOException, JsonLdError {
    var next = UriWrapper.fromUri(searchId(API_HOST)).addQueryParameter("cursor", "x").getUri();

    var triples = toTriplesWithApprovalContext(searchResults(List.of(), next));

    assertThat(triples, hasItem(Triple.of(searchIri(), hydra("next"), next.toString())));
  }

  private static ApprovalSearchResponse searchResults(List<ApprovalHit> hits, URI next) {
    return new ApprovalSearchResponse(
        context(API_HOST), searchId(API_HOST), TOTAL_ITEMS, hits, next, searchTemplate());
  }

  private static IriTemplate searchTemplate() {
    return ApprovalSearchResponse.from(emptyResponse(), new Query(null), API_HOST).search();
  }

  private static SearchResponse<IndexDocument> emptyResponse() {
    return new SearchResponse.Builder<IndexDocument>()
        .took(1)
        .timedOut(false)
        .shards(shards -> shards.total(1).successful(1).failed(0))
        .hits(
            hits ->
                hits.total(total -> total.value(0).relation(TotalHitsRelation.Eq)).hits(List.of()))
        .build();
  }

  private static String mappingFor(List<Triple> triples, String template, String variable) {
    return triples.stream()
        .filter(triple -> triple.subject().equals(template))
        .filter(triple -> triple.predicate().equals(hydra("mapping")))
        .map(Triple::object)
        .filter(node -> triples.contains(Triple.literal(node, hydra("variable"), variable)))
        .findFirst()
        .orElseThrow();
  }

  private static String hydra(String name) {
    return HYDRA_NAMESPACE + name;
  }

  private static ApprovalHit approvalHit() {
    return new ApprovalHit(
        approvalId(API_HOST, randomUUID()),
        handleUri(),
        randomString(),
        List.of(new NamedIdentifier(randomString(), randomString())));
  }

  private static URI handleUri() {
    return UriWrapper.fromHost("hdl.handle.net").addChild(UUID.randomUUID().toString()).getUri();
  }

  private static String searchIri() {
    return searchId(API_HOST).toString();
  }

  private static String term(String name) {
    return APPROVAL_ONTOLOGY_NAMESPACE + name;
  }
}
