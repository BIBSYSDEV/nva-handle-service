package no.sikt.nva.approvals.index;

import static nva.commons.core.attempt.Try.attempt;

import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.attempt.Failure;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch.core.IndexRequest;
import org.opensearch.client.opensearch.core.IndexResponse;

public class SearchClient {

  private static final String INDEXING_FAILED_MESSAGE = "Failed to index document: ";
  private static final String SEARCH_INDEX = "SEARCH_INDEX";

  private final OpenSearchClient client;
  private final String indexName;

  SearchClient(OpenSearchClient client, String indexName) {
    this.client = client;
    this.indexName = indexName;
  }

  @JacocoGenerated
  public static SearchClient defaultInstance(Environment environment) {
    return new SearchClient(
        OpenSearchClientFactory.createAuthenticatedClient(environment),
        environment.readEnv(SEARCH_INDEX));
  }

  public void index(IndexDocument document) {
    attempt(() -> client.index(toIndexRequest(document)))
        .orElseThrow(failure -> indexingException(document, failure));
  }

  private static SearchClientException indexingException(
      IndexDocument document, Failure<IndexResponse> failure) {
    return new SearchClientException(
        INDEXING_FAILED_MESSAGE + document.identifier(), failure.getException());
  }

  private IndexRequest<IndexDocument> toIndexRequest(IndexDocument document) {
    return new IndexRequest.Builder<IndexDocument>()
        .index(indexName)
        .id(document.identifier().toString())
        .document(document)
        .build();
  }
}
