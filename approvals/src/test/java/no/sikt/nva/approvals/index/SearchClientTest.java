package no.sikt.nva.approvals.index;

import static java.util.UUID.randomUUID;
import static no.sikt.nva.approvals.utils.TestUtils.randomApproval;
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
import java.util.stream.Collectors;
import java.util.stream.Stream;
import no.sikt.nva.approvals.domain.NamedIdentifier;
import no.sikt.nva.approvals.index.FramedClinicalTrial.Investigator;
import no.sikt.nva.approvals.index.FramedClinicalTrial.TrialSite;
import no.sikt.nva.approvals.index.IndexDocument.Identifier;
import no.sikt.nva.approvals.index.IndexDocument.Organization;
import no.sikt.nva.approvals.index.IndexDocument.Person;
import nva.commons.core.StringUtils;
import nva.commons.core.paths.UriWrapper;
import org.apache.hc.core5.http.HttpHost;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.opensearch.client.json.jackson.JacksonJsonpMapper;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch._types.OpenSearchException;
import org.opensearch.client.opensearch.core.GetRequest;
import org.opensearch.client.opensearch.core.search.Hit;
import org.opensearch.client.transport.httpclient5.ApacheHttpClient5Transport;
import org.opensearch.client.transport.httpclient5.ApacheHttpClient5TransportBuilder;
import org.opensearch.testcontainers.OpenSearchContainer;
import org.testcontainers.utility.DockerImageName;

class SearchClientTest {

  private static final DockerImageName OPEN_SEARCH_IMAGE =
      DockerImageName.parse("opensearchproject/opensearch:3.5.0");
  private static final String INDEX_NAME = "approvals";
  private static final String UNREACHABLE_HOST = "http://localhost:1";
  private static final String UNMAPPED_FIELD = "unmappedField";
  private static final char TYPO = 'x';
  private static final char ALTERNATIVE_TYPO = 'y';
  private static final String IDENTIFIER_FIELD = "identifier";
  private static final IndexDocument SEARCHABLE_DOCUMENT = randomIndexDocument();
  private static final IndexDocument SECOND_DOCUMENT = randomIndexDocument();
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
    client.index(List.of(SEARCHABLE_DOCUMENT, SECOND_DOCUMENT));
    refresh(searchableIndexName);
  }

  @AfterAll
  static void stopContainer() throws IOException {
    transport.close();
    CONTAINER.stop();
  }

  @Test
  void shouldIndexEveryDocumentInBatch() throws IOException {
    var first = randomIndexDocument();
    var second = randomIndexDocument();

    searchClient.index(List.of(first, second));

    assertEquals(
        List.of(first, second),
        List.of(getIndexedDocument(first.identifier()), getIndexedDocument(second.identifier())));
  }

  @Test
  void shouldNotReturnIdentifierOfNonIndexedDocument() throws IOException {
    var indexName = randomIndexName();
    createIndexRejectingIdentifier(indexName);
    var document = randomIndexDocument();

    assertEquals(Set.of(), searchClient(indexName).index(List.of(document)));
  }

  @Test
  void shouldReturnIdentifiersOfIndexedDocuments() {
    var document = randomIndexDocument();

    assertEquals(Set.of(document.identifier()), searchClient.index(List.of(document)));
  }

  @Test
  void shouldNameIndexInEveryBulkAction() {
    var first = randomIndexDocument();
    var second = randomIndexDocument();

    var request = searchClient.createBulkRequest(List.of(first, second));

    assertEquals(
        List.of(INDEX_NAME, INDEX_NAME),
        request.operations().stream().map(operation -> operation.index().index()).toList());
  }

  @Test
  void shouldThrowSearchClientExceptionWhenIndexingFails() throws IOException {
    try (var unreachableTransport = transport(UNREACHABLE_HOST)) {
      var client = new SearchClient(new OpenSearchClient(unreachableTransport), INDEX_NAME);

      assertThrows(SearchClientException.class, () -> client.index(List.of(randomIndexDocument())));
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
  void shouldRejectDocumentWithFieldOutsideMappings() {
    var indexName = randomIndexName();
    searchClient(indexName).createIndex();

    assertThrows(
        OpenSearchException.class,
        () -> indexRawDocument(indexName, Map.of(UNMAPPED_FIELD, randomString())));
  }

  @Test
  void shouldReturnAllDocumentsWhenQueryIsBlank() {
    var hits = searchFreeText(StringUtils.EMPTY_STRING);

    assertEquals(2, hits.size());
  }

  @Test
  void shouldFindDocumentByTitle() {
    var title = SEARCHABLE_DOCUMENT.title();

    assertEquals(List.of(SEARCHABLE_DOCUMENT), searchFreeText(title));
  }

  @Test
  void shouldFindDocumentByMisspelledPersonName() {
    var personName = person().name();

    assertEquals(List.of(SEARCHABLE_DOCUMENT), searchFreeText(withTypo(personName)));
  }

  @Test
  void shouldNotFindDocumentByHandleThroughFreeText() {
    assertEquals(List.of(), searchFreeText(SEARCHABLE_DOCUMENT.handle()));
  }

  @Test
  void shouldNotFindDocumentByNamedIdentifierValueThroughFreeText() {
    assertEquals(List.of(), searchFreeText(namedIdentifier().value()));
  }

  @Test
  void shouldFindDocumentByPersonName() {
    var personName = person().name();

    assertEquals(List.of(SEARCHABLE_DOCUMENT), searchFreeText(personName));
  }

  @Test
  void shouldFindDocumentByOrganizationName() {
    var organizationName = organization().name();

    assertEquals(List.of(SEARCHABLE_DOCUMENT), searchFreeText(organizationName));
  }

  @Test
  void shouldReturnEachDocumentOnceWhenPagingWithCursor() {
    var firstPage = searchPage(null);

    var secondPage = searchPage(firstPage.getFirst().source().identifier().toString());

    var documents =
        Stream.of(firstPage.getFirst().source(), secondPage.getFirst().source())
            .collect(Collectors.toSet());

    assertEquals(Set.of(SEARCHABLE_DOCUMENT, SECOND_DOCUMENT), documents);
  }

  @Test
  void shouldFindDocumentByPersonFilter() {
    var personIdentifier = person().id();

    assertEquals(List.of(SEARCHABLE_DOCUMENT), searchByPerson(personIdentifier));
  }

  @Test
  void shouldNotFindDocumentWhenNoPersonMatchesFilter() {
    assertEquals(List.of(), searchByPerson(randomString()));
  }

  @Test
  void shouldFindDocumentByOrganizationFilter() {
    var organizationIdentifier = organization().id();

    assertEquals(
        List.of(SEARCHABLE_DOCUMENT),
        searchByFilter(new OrganizationFilter(organizationIdentifier)));
  }

  @Test
  void shouldNotFindDocumentWhenNoOrganizationMatchesFilter() {
    assertEquals(List.of(), searchByFilter(new OrganizationFilter(randomString())));
  }

  @Test
  void shouldFindDocumentByNamespacedIdentifierFilter() {
    var identifier = namedIdentifier();

    assertEquals(
        List.of(SEARCHABLE_DOCUMENT),
        searchByFilter(new IdentifierFilter(identifier.name(), identifier.value())));
  }

  @Test
  void shouldNotMatchNamespaceAndValueFromDifferentIdentifiers() throws IOException {
    var first = new Identifier(randomString(), randomString());
    var second = new Identifier(randomString(), randomString());
    var indexName = indexWithIdentifiers(first, second);

    var hits = searchIn(indexName, new IdentifierFilter(first.name(), second.value()));

    assertEquals(List.of(), hits);
  }

  @Test
  void shouldFindDocumentByHandleFilter() {
    assertEquals(
        List.of(SEARCHABLE_DOCUMENT),
        searchByFilter(new HandleFilter(SEARCHABLE_DOCUMENT.handle())));
  }

  @Test
  void shouldNotFindDocumentWhenHandleFilterMatchesNoApproval() {
    assertEquals(List.of(), searchByFilter(new HandleFilter(randomHandle().identifier())));
  }

  private static String indexWithIdentifiers(Identifier first, Identifier second)
      throws IOException {
    var indexName = randomIndexName();
    var client = searchClient(indexName);
    client.createIndex();
    client.index(
        List.of(
            IndexDocument.create(
                randomTrial(),
                randomApproval(
                    List.of(
                        new NamedIdentifier(first.name(), first.value()),
                        new NamedIdentifier(second.name(), second.value())),
                    randomUUID()))));
    refresh(indexName);
    return indexName;
  }

  private static List<IndexDocument> searchIn(String indexName, SearchFilter filter) {
    return searchClient(indexName)
        .search(new Query(null, List.of(filter), null, Query.DEFAULT_SIZE))
        .hits()
        .hits()
        .stream()
        .map(Hit::source)
        .toList();
  }

  private static List<IndexDocument> searchByPerson(String person) {
    return searchByFilter(new PersonFilter(person));
  }

  private static List<IndexDocument> searchByFilter(SearchFilter filter) {
    return searchClient(searchableIndexName)
        .search(new Query(null, List.of(filter), null, Query.DEFAULT_SIZE))
        .hits()
        .hits()
        .stream()
        .map(Hit::source)
        .toList();
  }

  private static Person person() {
    return SEARCHABLE_DOCUMENT.persons().iterator().next();
  }

  private static Organization organization() {
    return SEARCHABLE_DOCUMENT.organizations().iterator().next();
  }

  private static Identifier namedIdentifier() {
    return SEARCHABLE_DOCUMENT.display().identifiers().iterator().next();
  }

  private static List<Hit<IndexDocument>> searchPage(String cursor) {
    return searchClient(searchableIndexName)
        .search(new Query(StringUtils.EMPTY_STRING, List.of(), cursor, 1))
        .hits()
        .hits();
  }

  private static String withTypo(String value) {
    var replacement = value.charAt(1) == TYPO ? ALTERNATIVE_TYPO : TYPO;
    return value.charAt(0) + String.valueOf(replacement) + value.substring(2);
  }

  private static List<IndexDocument> searchFreeText(String value) {
    return searchClient(searchableIndexName).search(new Query(value)).hits().hits().stream()
        .map(Hit::source)
        .toList();
  }

  private static void refresh(String indexName) throws IOException {
    openSearchClient.indices().refresh(request -> request.index(indexName));
  }

  private static void indexRawDocument(String indexName, Map<String, String> document)
      throws IOException {
    openSearchClient.index(
        request -> request.index(indexName).id(randomUUID().toString()).document(document));
  }

  private static void createIndexRejectingIdentifier(String indexName) throws IOException {
    openSearchClient
        .indices()
        .create(
            request ->
                request
                    .index(indexName)
                    .mappings(
                        mappings ->
                            mappings.properties(
                                IDENTIFIER_FIELD,
                                property -> property.integer(integer -> integer))));
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

  private static IndexDocument getIndexedDocument(UUID identifier) throws IOException {
    var request =
        new GetRequest.Builder()
            .index(SearchClientTest.INDEX_NAME)
            .id(identifier.toString())
            .build();
    return openSearchClient.get(request, IndexDocument.class).source();
  }

  private static ApacheHttpClient5Transport transport(String hostAddress) {
    return ApacheHttpClient5TransportBuilder.builder(HttpHost.create(URI.create(hostAddress)))
        .setMapper(new JacksonJsonpMapper())
        .build();
  }

  private static IndexDocument randomIndexDocument() {
    return IndexDocument.create(randomTrial(), randomApproval(randomHandle()));
  }

  private static FramedClinicalTrial randomTrial() {
    return new FramedClinicalTrial(
        randomString(),
        Set.of(new Investigator(cristinUri("person"), randomString(), randomString())),
        Set.of(new TrialSite(cristinUri("organization"), randomString())));
  }

  private static URI cristinUri(String resource) {
    return UriWrapper.fromHost(randomString())
        .addChild("cristin")
        .addChild(resource)
        .addChild(randomString())
        .getUri();
  }
}
