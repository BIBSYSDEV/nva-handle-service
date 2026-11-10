package no.sikt.nva.approvals.index;

import static java.util.function.Predicate.not;
import static java.util.stream.Collectors.toSet;
import static nva.commons.core.attempt.Try.attempt;

import java.io.StringReader;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.StringUtils;
import nva.commons.core.attempt.Failure;
import nva.commons.core.ioutils.IoUtils;
import org.opensearch.client.json.jackson.JacksonJsonpMapper;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch._types.ErrorCause;
import org.opensearch.client.opensearch._types.FieldValue;
import org.opensearch.client.opensearch._types.OpenSearchException;
import org.opensearch.client.opensearch._types.SortOrder;
import org.opensearch.client.opensearch._types.mapping.TypeMapping;
import org.opensearch.client.opensearch._types.query_dsl.Operator;
import org.opensearch.client.opensearch.core.BulkRequest;
import org.opensearch.client.opensearch.core.BulkResponse;
import org.opensearch.client.opensearch.core.SearchRequest;
import org.opensearch.client.opensearch.core.SearchResponse;
import org.opensearch.client.opensearch.core.bulk.BulkOperation;
import org.opensearch.client.opensearch.core.bulk.BulkResponseItem;
import org.opensearch.client.opensearch.indices.CreateIndexRequest;
import org.opensearch.client.opensearch.indices.CreateIndexResponse;
import org.opensearch.client.opensearch.indices.DeleteIndexRequest;
import org.opensearch.client.opensearch.indices.DeleteIndexResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SearchClient {

  private static final String INDEXING_FAILED_MESSAGE = "Failed to index documents in: ";
  private static final String INDEX_CREATION_FAILED_MESSAGE = "Failed to create index: ";
  private static final String INDEX_DELETION_FAILED_MESSAGE = "Failed to delete index: ";
  private static final String INDEX_ALREADY_EXISTS = "resource_already_exists_exception";
  private static final String SEARCH_FAILED_MESSAGE = "Failed to search index: ";
  private static final String FREE_TEXT = "freeText";
  private static final String IDENTIFIER = "identifier";
  public static final String SEARCH_INDEX = "SEARCH_INDEX";
  private static final String MAPPINGS_FILE = "index/approval-mappings.json";
  private static final TypeMapping MAPPINGS = readMappings();
  private static final String DOCUMENT_REJECTED_MESSAGE =
      "IndexDocument with id {} was rejected: {}";
  private static final Logger LOGGER = LoggerFactory.getLogger(SearchClient.class);

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
                client
                    .indices()
                    .create(
                        new CreateIndexRequest.Builder()
                            .index(indexName)
                            .mappings(MAPPINGS)
                            .build()))
        .orElse(this::ignoreExistingIndex);
  }

  public void deleteIndex() {
    attempt(
            () ->
                client.indices().delete(new DeleteIndexRequest.Builder().index(indexName).build()))
        .orElseThrow(this::deletionException);
  }

  public SearchResponse<IndexDocument> search(Query query) {
    return attempt(() -> client.search(toSearchRequest(query), IndexDocument.class))
        .orElseThrow(this::searchException);
  }

  public Set<UUID> index(Collection<IndexDocument> documents) {
    return attempt(() -> client.bulk(createBulkRequest(documents)))
        .map(SearchClient::indexedIdentifiers)
        .orElseThrow(this::indexingException);
  }

  private static Set<UUID> indexedIdentifiers(BulkResponse response) {
    response.items().stream().filter(SearchClient::isRejected).forEach(SearchClient::logRejection);
    return response.items().stream()
        .filter(not(SearchClient::isRejected))
        .map(BulkResponseItem::id)
        .filter(Objects::nonNull)
        .map(UUID::fromString)
        .collect(toSet());
  }

  private static boolean isRejected(BulkResponseItem item) {
    return Objects.nonNull(item.error());
  }

  private static void logRejection(BulkResponseItem item) {
    LOGGER.error(DOCUMENT_REJECTED_MESSAGE, item.id(), getErrorCause(item));
  }

  private static String getErrorCause(BulkResponseItem item) {
    return Optional.ofNullable(item.error()).map(ErrorCause::toJsonString).orElse(null);
  }

  private SearchClientException indexingException(Failure<Set<UUID>> failure) {
    return new SearchClientException(INDEXING_FAILED_MESSAGE + indexName, failure.getException());
  }

  private static TypeMapping readMappings() {
    var mapper = new JacksonJsonpMapper();
    var mappings = IoUtils.stringFromResources(Path.of(MAPPINGS_FILE));
    try (var parser = mapper.jsonProvider().createParser(new StringReader(mappings))) {
      return TypeMapping._DESERIALIZER.deserialize(parser, mapper);
    }
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

  private SearchRequest toSearchRequest(Query query) {
    var request =
        new SearchRequest.Builder()
            .index(indexName)
            .size(query.size())
            .sort(sort -> sort.field(field -> field.field(IDENTIFIER).order(SortOrder.Asc)))
            .query(
                builder ->
                    StringUtils.isBlank(query.query())
                        ? builder.matchAll(matchAll -> matchAll)
                        : builder.match(
                            match ->
                                match
                                    .field(FREE_TEXT)
                                    .query(FieldValue.of(query.searchTerm()))
                                    .operator(Operator.And)));
    return Optional.ofNullable(query.cursor())
        .map(cursor -> request.searchAfter(FieldValue.of(cursor)))
        .orElse(request)
        .build();
  }

  private SearchClientException searchException(Failure<SearchResponse<IndexDocument>> failure) {
    return new SearchClientException(SEARCH_FAILED_MESSAGE + indexName, failure.getException());
  }

  private BulkRequest createBulkRequest(Collection<IndexDocument> documents) {
    return new BulkRequest.Builder()
        .index(indexName)
        .operations(documents.stream().map(SearchClient::toBulkOperation).toList())
        .build();
  }

  private static BulkOperation toBulkOperation(IndexDocument document) {
    return new BulkOperation.Builder()
        .index(operation -> operation.id(document.identifier().toString()).document(document))
        .build();
  }
}
