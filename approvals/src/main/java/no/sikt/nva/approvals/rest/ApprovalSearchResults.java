package no.sikt.nva.approvals.rest;

import static no.sikt.nva.approvals.utils.UriConstants.searchId;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import no.sikt.nva.approvals.index.IndexDocument;
import no.sikt.nva.approvals.index.Query;
import no.unit.nva.commons.pagination.PaginatedSearchResult;
import nva.commons.apigateway.exceptions.UnprocessableContentException;
import nva.commons.core.StringUtils;
import org.opensearch.client.opensearch.core.SearchResponse;
import org.opensearch.client.opensearch.core.search.Hit;

public final class ApprovalSearchResults {

  private static final String QUERY_PARAMETER = "query";

  private ApprovalSearchResults() {}

  public static PaginatedSearchResult<ApprovalHit> from(
      SearchResponse<IndexDocument> response, Query query, String apiHost)
      throws UnprocessableContentException {
    return PaginatedSearchResult.create(
        searchId(apiHost),
        query.offset(),
        query.size(),
        totalHits(response),
        toHits(response, apiHost),
        searchParameters(query));
  }

  private static int totalHits(SearchResponse<IndexDocument> response) {
    return (int) response.hits().total().value();
  }

  private static List<ApprovalHit> toHits(SearchResponse<IndexDocument> response, String apiHost) {
    return response.hits().hits().stream()
        .map(Hit::source)
        .map(document -> ApprovalHit.fromDisplay(document.display(), apiHost))
        .toList();
  }

  private static Map<String, String> searchParameters(Query query) {
    return Optional.ofNullable(query.query())
        .filter(StringUtils::isNotBlank)
        .map(value -> Map.of(QUERY_PARAMETER, value))
        .orElseGet(Map::of);
  }
}
