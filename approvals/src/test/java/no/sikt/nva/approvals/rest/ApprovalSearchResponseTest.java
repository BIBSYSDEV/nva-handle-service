package no.sikt.nva.approvals.rest;

import static java.util.UUID.randomUUID;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.endsWith;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import no.sikt.nva.approvals.index.IndexDocument;
import no.sikt.nva.approvals.index.IndexDocument.Display;
import no.sikt.nva.approvals.index.IndexDocument.Identifier;
import no.sikt.nva.approvals.index.Query;
import org.junit.jupiter.api.Test;
import org.opensearch.client.opensearch.core.SearchResponse;
import org.opensearch.client.opensearch.core.search.Hit;
import org.opensearch.client.opensearch.core.search.TotalHitsRelation;

class ApprovalSearchResponseTest {

  private static final String API_HOST = randomUri().getHost();
  private static final String QUERY_PARAMETER = "query";
  private static final String SIZE_PARAMETER = "size";
  private static final String CURSOR_PARAMETER = "cursor";
  private static final int SINGLE_HIT = 1;

  @Test
  void shouldConvertDocumentsToApprovalHits() {
    var document = indexDocument();

    var results = ApprovalSearchResponse.from(response(List.of(document), 1), query(), API_HOST);

    assertThat(results.hits(), contains(ApprovalHit.fromDisplay(document.display(), API_HOST)));
  }

  @Test
  void shouldReturnSearchTemplateWithSupportedParameters() {
    var results = ApprovalSearchResponse.from(response(List.of(), 0), query(), API_HOST);

    assertThat(results.search().template(), endsWith("{?query,size,cursor}"));
  }

  @Test
  void shouldReportTotalNumberOfHits() {
    var results = ApprovalSearchResponse.from(response(List.of(), 42), query(), API_HOST);

    assertEquals(42, results.totalHits());
  }

  @Test
  void shouldBuildIdFromQueryAndSize() {
    var query = new Query(randomString(), null, 5);

    var results = ApprovalSearchResponse.from(response(List.of(), 0), query, API_HOST);

    assertEquals(
        Map.of(QUERY_PARAMETER, query.query(), SIZE_PARAMETER, "5"),
        queryParametersOf(results.id()));
  }

  @Test
  void shouldOmitQueryFromIdWhenQueryIsBlank() {
    var results = ApprovalSearchResponse.from(response(List.of(), 0), new Query(null), API_HOST);

    assertEquals(Map.of(SIZE_PARAMETER, "10"), queryParametersOf(results.id()));
  }

  @Test
  void shouldReturnNextPageUriWithCursorOfLastHitWhenPageIsFull() {
    var document = indexDocument();
    var query = new Query(randomString(), null, SINGLE_HIT);

    var results = ApprovalSearchResponse.from(response(List.of(document), 50), query, API_HOST);

    assertEquals(
        document.identifier().toString(), queryParametersOf(results.next()).get(CURSOR_PARAMETER));
  }

  @Test
  void shouldNotReturnNextWhenRequestedSizeIsZero() {
    var query = new Query(randomString(), null, 0);

    var results = ApprovalSearchResponse.from(response(List.of(), 42), query, API_HOST);

    assertNull(results.next());
  }

  @Test
  void shouldNotReturnNextWhenPageIsNotFull() {
    var results =
        ApprovalSearchResponse.from(response(List.of(indexDocument()), 1), query(), API_HOST);

    assertNull(results.next());
  }

  private static Query query() {
    return new Query(randomString());
  }

  private static Map<String, String> queryParametersOf(URI uri) {
    return Arrays.stream(uri.getQuery().split("&"))
        .map(parameter -> parameter.split("=", 2))
        .collect(Collectors.toMap(parts -> parts[0], parts -> parts[1]));
  }

  private static SearchResponse<IndexDocument> response(
      List<IndexDocument> documents, int totalHits) {
    return new SearchResponse.Builder<IndexDocument>()
        .took(1)
        .timedOut(false)
        .shards(shards -> shards.total(1).successful(1).failed(0))
        .hits(
            hits ->
                hits.total(total -> total.value(totalHits).relation(TotalHitsRelation.Eq))
                    .hits(documents.stream().map(ApprovalSearchResponseTest::hit).toList()))
        .build();
  }

  private static Hit<IndexDocument> hit(IndexDocument document) {
    return new Hit.Builder<IndexDocument>()
        .index("approvals")
        .id(document.identifier().toString())
        .source(document)
        .build();
  }

  private static IndexDocument indexDocument() {
    var identifier = randomUUID();
    var handle = randomString() + "/" + randomString();
    var title = randomString();
    var identifiers = Set.of(new Identifier(randomString(), randomString()));
    return new IndexDocument(
        identifier,
        handle,
        title,
        Set.of(),
        Set.of(),
        identifiers,
        new Display(identifier, handle, title, identifiers),
        Set.of());
  }
}
