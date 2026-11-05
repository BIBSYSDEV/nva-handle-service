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
import no.sikt.nva.approvals.utils.JsonLdTestUtils.Triple;
import nva.commons.core.paths.UriWrapper;
import org.junit.jupiter.api.Test;

class SearchContextTriplesTest {

  private static final String API_HOST = "localhost";
  private static final String XSD_INTEGER = "http://www.w3.org/2001/XMLSchema#integer";
  private static final int TOTAL_ITEMS = 42;
  private static final String SEARCH_TEMPLATE = "https://localhost/approval{?query,size,cursor}";

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
    var mapping = objectOf(triples, template, hydra("mapping"));

    assertThat(
        triples,
        hasItems(
            Triple.literal(template, hydra("template"), SEARCH_TEMPLATE),
            Triple.of(mapping, hydra("property"), hydra("freetextQuery"))));
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
    return new IriTemplate(
        SEARCH_TEMPLATE,
        List.of(new IriTemplateMapping("query", "hydra:freetextQuery", "Free text")));
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
