package no.sikt.nva.approvals.index;

import static java.util.UUID.randomUUID;
import static no.sikt.nva.approvals.utils.TestUtils.randomHandle;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import no.sikt.nva.approvals.index.IndexDocument.Display;
import org.apache.hc.core5.http.HttpHost;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.opensearch.client.json.jackson.JacksonJsonpMapper;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch._types.OpenSearchException;
import org.opensearch.client.opensearch._types.mapping.Property;
import org.opensearch.client.opensearch.core.GetRequest;
import org.opensearch.client.transport.httpclient5.ApacheHttpClient5Transport;
import org.opensearch.client.transport.httpclient5.ApacheHttpClient5TransportBuilder;
import org.opensearch.testcontainers.OpenSearchContainer;
import org.testcontainers.utility.DockerImageName;

class SearchClientTest {

  private static final DockerImageName OPEN_SEARCH_IMAGE =
      DockerImageName.parse("opensearchproject/opensearch:3.5.0");
  private static final String INDEX_NAME = "approvals";
  private static final String UNREACHABLE_HOST = "http://localhost:1";
  private static final String HANDLE = "handle";
  private static final String DISPLAY = "display";
  private static final IndexDocument SEARCHABLE_DOCUMENT = randomIndexDocument();
  private static final String UNMAPPED_FIELD = "unmappedField";
  private static final OpenSearchContainer<?> CONTAINER =
      new OpenSearchContainer<>(OPEN_SEARCH_IMAGE);

  private static String searchableIndexName;
  private static ApacheHttpClient5Transport transport;
  private static OpenSearchClient openSearchClient;
  private static SearchClient searchClient;

  @BeforeAll
  static void startContainer() throws IOException {
    CONTAINER.start();
    transport = transport(CONTAINER.getHttpHostAddress());
    openSearchClient = new OpenSearchClient(transport);
    searchClient = new SearchClient(openSearchClient, INDEX_NAME);
    searchableIndexName = randomIndexName();
    var client = searchClient(searchableIndexName);
    client.createIndex();
    client.index(SEARCHABLE_DOCUMENT);
    client.index(randomIndexDocument());
    refresh(searchableIndexName);
  }

  @AfterAll
  static void stopContainer() throws IOException {
    transport.close();
    CONTAINER.stop();
  }

  @Test
  void shouldIndexDocument() throws IOException {
    var document = randomIndexDocument();

    searchClient.index(document);

    assertEquals(document, getIndexedDocument(INDEX_NAME, document.identifier()));
  }

  @Test
  void shouldThrowSearchClientExceptionWhenIndexingFails() throws IOException {
    try (var unreachableTransport = transport(UNREACHABLE_HOST)) {
      var client = new SearchClient(new OpenSearchClient(unreachableTransport), INDEX_NAME);

      assertThrows(SearchClientException.class, () -> client.index(randomIndexDocument()));
    }
  }

  @Test
  void shouldCreateIndex() throws IOException {
    var indexName = randomIndexName();

    searchClient(indexName).createIndex();

    assertTrue(indexExists(indexName));
  }

  @Test
  void shouldNotFailWhenCreatingIndexThatAlreadyExists() throws IOException {
    var indexName = randomIndexName();
    var client = searchClient(indexName);
    client.createIndex();

    client.createIndex();

    assertTrue(indexExists(indexName));
  }

  @Test
  void shouldDeleteIndex() throws IOException {
    var indexName = randomIndexName();
    var client = searchClient(indexName);
    client.createIndex();

    client.deleteIndex();

    assertFalse(indexExists(indexName));
  }

  @Test
  void shouldThrowSearchClientExceptionWhenSearchingIndexThatDoesNotExist() {
    var client = searchClient(randomIndexName());

    assertThrows(SearchClientException.class, () -> client.search(new Query(randomString())));
  }

  @Test
  void shouldThrowSearchClientExceptionWhenDeletingIndexThatDoesNotExist() {
    var client = searchClient(randomIndexName());

    assertThrows(SearchClientException.class, client::deleteIndex);
  }

  @Test
  void shouldIndexDocumentIntoIndexWithMappings() throws IOException {
    var indexName = randomIndexName();
    var client = searchClient(indexName);
    client.createIndex();
    var document = randomIndexDocument();

    client.index(document);

    assertEquals(document, getIndexedDocument(indexName, document.identifier()));
  }

  @Test
  void shouldRejectDocumentWithFieldOutsideMappings() {
    var indexName = randomIndexName();
    searchClient(indexName).createIndex();

    assertThrows(
        OpenSearchException.class,
        () -> indexRawDocument(indexName, Map.of(UNMAPPED_FIELD, randomString())));
  }

  @Test
  void shouldFindDocumentByHandleUri() throws IOException {
    assertEquals(
        List.of(SEARCHABLE_DOCUMENT), searchFreeText(SEARCHABLE_DOCUMENT.handle().toString()));
  }

  @Test
  void shouldFindDocumentByHandleIdentifier() throws IOException {
    assertEquals(
        List.of(SEARCHABLE_DOCUMENT),
        searchFreeText(lastPathSegment(SEARCHABLE_DOCUMENT.handle())));
  }

  @Test
  void shouldFindDocumentByApprovalIdentifier() throws IOException {
    assertEquals(
        List.of(SEARCHABLE_DOCUMENT), searchFreeText(SEARCHABLE_DOCUMENT.identifier().toString()));
  }

  @Test
  void shouldFindDocumentByNamedIdentifierValue() throws IOException {
    assertEquals(List.of(SEARCHABLE_DOCUMENT), searchFreeText(namedIdentifier().value()));
  }

  @Test
  void shouldFindDocumentByPersonName() throws IOException {
    assertEquals(List.of(SEARCHABLE_DOCUMENT), searchFreeText(person().name()));
  }

  @Test
  void shouldFindDocumentByPersonIdentifierUri() throws IOException {
    assertEquals(List.of(SEARCHABLE_DOCUMENT), searchFreeText(person().id().toString()));
  }

  @Test
  void shouldFindDocumentByPersonIdentifier() throws IOException {
    assertEquals(List.of(SEARCHABLE_DOCUMENT), searchFreeText(lastPathSegment(person().id())));
  }

  @Test
  void shouldFindDocumentByOrganizationName() throws IOException {
    assertEquals(List.of(SEARCHABLE_DOCUMENT), searchFreeText(organization().name()));
  }

  @Test
  void shouldFindDocumentByOrganizationIdentifierUri() throws IOException {
    assertEquals(List.of(SEARCHABLE_DOCUMENT), searchFreeText(organization().id().toString()));
  }

  @Test
  void shouldFindDocumentByOrganizationIdentifier() throws IOException {
    assertEquals(
        List.of(SEARCHABLE_DOCUMENT), searchFreeText(lastPathSegment(organization().id())));
  }

  @Test
  void shouldFindDocumentByTitle() throws IOException {
    assertEquals(List.of(SEARCHABLE_DOCUMENT), searchFreeText(SEARCHABLE_DOCUMENT.title()));
  }

  private static Person person() {
    return SEARCHABLE_DOCUMENT.persons().iterator().next();
  }

  private static Organization organization() {
    return SEARCHABLE_DOCUMENT.organizations().iterator().next();
  }

  private static Identifier namedIdentifier() {
    return SEARCHABLE_DOCUMENT.identifiers().iterator().next();
  }

  private static String lastPathSegment(URI uri) {
    return UriWrapper.fromUri(uri).getLastPathElement();
  }

  private static List<IndexDocument> searchFreeText(String value) {
    return searchClient(searchableIndexName).search(new Query(value));
  }

  private static void refresh(String indexName) throws IOException {
    openSearchClient.indices().refresh(request -> request.index(indexName));
  }

  @Test
  void shouldNotIndexDisplayField() throws IOException {
    var indexName = randomIndexName();

    searchClient(indexName).createIndex();

    assertFalse(Boolean.TRUE.equals(mappedProperty(indexName, DISPLAY).object().enabled()));
  }

  @Test
  void shouldMapHandleAsKeyword() throws IOException {
    var indexName = randomIndexName();

    searchClient(indexName).createIndex();

    assertTrue(mappedProperty(indexName, HANDLE).isKeyword());
  }

  private static void indexRawDocument(String indexName, Map<String, String> document)
      throws IOException {
    openSearchClient.index(
        request -> request.index(indexName).id(randomUUID().toString()).document(document));
  }

  private static Property mappedProperty(String indexName, String property) throws IOException {
    var response = openSearchClient.indices().getMapping(request -> request.index(indexName));
    return response.result().get(indexName).mappings().properties().get(property);
  }

  private static boolean indexExists(String indexName) throws IOException {
    return openSearchClient.indices().exists(request -> request.index(indexName)).value();
  }

  private static SearchClient searchClient(String indexName) {
    return new SearchClient(openSearchClient, indexName);
  }

  private static String randomIndexName() {
    return "index-" + randomUUID();
  }

  private static IndexDocument getIndexedDocument(String indexName, UUID identifier)
      throws IOException {
    var request = new GetRequest.Builder().index(indexName).id(identifier.toString()).build();
    return openSearchClient.get(request, IndexDocument.class).source();
  }

  private static ApacheHttpClient5Transport transport(String hostAddress) {
    return ApacheHttpClient5TransportBuilder.builder(HttpHost.create(URI.create(hostAddress)))
        .setMapper(new JacksonJsonpMapper())
        .build();
  }

  private static IndexDocument randomIndexDocument() {
    var identifier = randomUUID();
    var handle = randomHandle().identifier();
    var title = randomString();
    return new IndexDocument(
        identifier,
        handle,
        title,
        Set.of(),
        Set.of(),
        Set.of(),
        new Display(identifier, handle, title, Set.of()));
  }
}
