package no.sikt.nva.approvals.rest;

import static no.sikt.nva.approvals.utils.UriConstants.CONTEXT_PROPERTY;
import static no.sikt.nva.approvals.utils.UriConstants.CURSOR_QUERY_PARAMETER;
import static no.sikt.nva.approvals.utils.UriConstants.SEARCH_QUERY_PARAMETER;
import static no.sikt.nva.approvals.utils.UriConstants.SIZE_QUERY_PARAMETER;
import static no.sikt.nva.approvals.utils.UriConstants.searchId;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import no.sikt.nva.approvals.index.IndexDocument;
import no.sikt.nva.approvals.index.Query;
import no.sikt.nva.approvals.utils.UriConstants;
import nva.commons.core.StringUtils;
import nva.commons.core.paths.UriWrapper;
import org.opensearch.client.opensearch.core.SearchResponse;
import org.opensearch.client.opensearch.core.search.Hit;

@JsonTypeInfo(use = Id.NAME, property = "type")
@JsonTypeName("ApprovalSearchResults")
public record ApprovalSearchResponse(
    @JsonProperty(CONTEXT_PROPERTY) URI context,
    URI id,
    int totalHits,
    List<ApprovalHit> hits,
    URI next) {

  public static ApprovalSearchResponse from(
      SearchResponse<IndexDocument> response, Query query, String apiHost) {
    return new ApprovalSearchResponse(
        UriConstants.context(apiHost),
        pageId(query, apiHost),
        totalHits(response),
        toHits(response, apiHost),
        nextPage(response, query, apiHost));
  }

  private static URI pageId(Query query, String apiHost) {
    return UriWrapper.fromUri(searchId(apiHost))
        .addQueryParameters(searchParameters(query))
        .getUri();
  }

  private static Map<String, String> searchParameters(Query query) {
    return Stream.of(
            parameter(SEARCH_QUERY_PARAMETER, query.query()),
            parameter(CURSOR_QUERY_PARAMETER, query.cursor()),
            parameter(SIZE_QUERY_PARAMETER, String.valueOf(query.size())))
        .flatMap(Optional::stream)
        .collect(Collectors.toMap(Entry::getKey, Entry::getValue));
  }

  private static Optional<Entry<String, String>> parameter(String name, String value) {
    return Optional.ofNullable(value)
        .filter(StringUtils::isNotBlank)
        .map(present -> Map.entry(name, present));
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

  private static URI nextPage(SearchResponse<IndexDocument> response, Query query, String apiHost) {
    return Optional.of(response.hits().hits())
        .filter(hits -> !hits.isEmpty() && hits.size() == query.size())
        .map(List::getLast)
        .map(Hit::source)
        .map(IndexDocument::identifier)
        .map(UUID::toString)
        .map(cursor -> pageId(nextQuery(query, cursor), apiHost))
        .orElse(null);
  }

  private static Query nextQuery(Query query, String cursor) {
    return new Query(query.query(), cursor, query.size());
  }
}
