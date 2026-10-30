package no.sikt.nva.approvals.index;

import static nva.commons.core.attempt.Try.attempt;

import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.attempt.Failure;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch._types.OpenSearchException;
import org.opensearch.client.opensearch.core.IndexRequest;
import org.opensearch.client.opensearch.core.IndexResponse;
import org.opensearch.client.opensearch.indices.CreateIndexRequest;
import org.opensearch.client.opensearch.indices.CreateIndexResponse;
import org.opensearch.client.opensearch.indices.DeleteIndexRequest;
import org.opensearch.client.opensearch.indices.DeleteIndexResponse;

public class SearchClient {

  private static final String INDEXING_FAILED_MESSAGE = "Failed to index document: ";
  private static final String INDEX_CREATION_FAILED_MESSAGE = "Failed to create index: ";
  private static final String INDEX_DELETION_FAILED_MESSAGE = "Failed to delete index: ";
  private static final String INDEX_ALREADY_EXISTS = "resource_already_exists_exception";
  public static final String SEARCH_INDEX = "SEARCH_INDEX";

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

  public void createIndex() {
    attempt(
            () ->
                client.indices().create(new CreateIndexRequest.Builder().index(indexName).build()))
        .orElse(this::ignoreExistingIndex);
  }

  public void deleteIndex() {
    attempt(
            () ->
                client.indices().delete(new DeleteIndexRequest.Builder().index(indexName).build()))
        .orElseThrow(this::deletionException);
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

  private CreateIndexResponse ignoreExistingIndex(Failure<CreateIndexResponse> failure) {
    if (isIndexAlreadyExists(failure.getException())) {
      return null;
    }
    throw new SearchClientException(
        INDEX_CREATION_FAILED_MESSAGE + indexName, failure.getException());
  }

  private SearchClientException deletionException(Failure<DeleteIndexResponse> failure) {
    return new SearchClientException(
        INDEX_DELETION_FAILED_MESSAGE + indexName, failure.getException());
  }

  private static boolean isIndexAlreadyExists(Exception exception) {
    return exception instanceof OpenSearchException openSearchException
        && INDEX_ALREADY_EXISTS.equals(openSearchException.error().type());
  }

  private IndexRequest<IndexDocument> toIndexRequest(IndexDocument document) {
    return new IndexRequest.Builder<IndexDocument>()
        .index(indexName)
        .id(document.identifier().toString())
        .document(document)
        .build();
  }
}
