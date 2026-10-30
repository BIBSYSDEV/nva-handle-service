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
  private static final IndexDocument SEARCHABLE_DOCUMENT = randomIndexDocument();
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

    assertEquals(document, getIndexedDocument(document.identifier()));
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
  void shouldReturnAllDocumentsWhenQueryIsBlank() {
    var hits = searchFreeText(StringUtils.EMPTY_STRING);

    assertEquals(2, hits.size());
  }

  @Test
  void shouldFindDocumentByApprovalIdentifier() {
    var approvalIdentifier = SEARCHABLE_DOCUMENT.identifier().toString();

    assertEquals(List.of(SEARCHABLE_DOCUMENT), searchFreeText(approvalIdentifier));
  }

  @Test
  void shouldFindDocumentByHandleIdentifier() {
    var handleIdentifier = SEARCHABLE_DOCUMENT.handle();

    assertEquals(List.of(SEARCHABLE_DOCUMENT), searchFreeText(handleIdentifier));
  }

  @Test
  void shouldFindDocumentByHandleUri() {
    var handleUri = "https://hdl.handle.net/" + SEARCHABLE_DOCUMENT.handle();

    assertEquals(List.of(SEARCHABLE_DOCUMENT), searchFreeText(handleUri));
  }

  @Test
  void shouldFindDocumentByTitle() {
    var title = SEARCHABLE_DOCUMENT.title();

    assertEquals(List.of(SEARCHABLE_DOCUMENT), searchFreeText(title));
  }

  @Test
  void shouldFindDocumentByPersonIdentifier() {
    var personIdentifier = person().id();

    assertEquals(List.of(SEARCHABLE_DOCUMENT), searchFreeText(personIdentifier));
  }

  @Test
  void shouldFindDocumentByPersonUri() {
    var personUri = "https://api.example.org/cristin/person/" + person().id();

    assertEquals(List.of(SEARCHABLE_DOCUMENT), searchFreeText(personUri));
  }

  @Test
  void shouldFindDocumentByPersonName() {
    var personName = person().name();

    assertEquals(List.of(SEARCHABLE_DOCUMENT), searchFreeText(personName));
  }

  @Test
  void shouldFindDocumentByOrganizationIdentifier() {
    var organizationIdentifier = organization().id();

    assertEquals(List.of(SEARCHABLE_DOCUMENT), searchFreeText(organizationIdentifier));
  }

  @Test
  void shouldFindDocumentByOrganizationUri() {
    var organizationUri = "https://api.example.org/cristin/organization/" + organization().id();

    assertEquals(List.of(SEARCHABLE_DOCUMENT), searchFreeText(organizationUri));
  }

  @Test
  void shouldFindDocumentByOrganizationName() {
    var organizationName = organization().name();

    assertEquals(List.of(SEARCHABLE_DOCUMENT), searchFreeText(organizationName));
  }

  @Test
  void shouldFindDocumentByNamedIdentifierValue() {
    var namedIdentifierValue = namedIdentifier().value();

    assertEquals(List.of(SEARCHABLE_DOCUMENT), searchFreeText(namedIdentifierValue));
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
