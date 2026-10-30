package no.sikt.nva.approvals.index;

import static java.util.UUID.randomUUID;
import static no.sikt.nva.approvals.utils.TestUtils.randomHandle;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.net.URI;
import java.util.Set;
import java.util.UUID;
import no.sikt.nva.approvals.index.IndexDocument.Display;
import org.apache.hc.core5.http.HttpHost;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.opensearch.client.json.jackson.JacksonJsonpMapper;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch.core.GetRequest;
import org.opensearch.client.transport.httpclient5.ApacheHttpClient5TransportBuilder;
import org.opensearch.testcontainers.OpenSearchContainer;
import org.testcontainers.utility.DockerImageName;

class SearchClientTest {

  private static final DockerImageName OPEN_SEARCH_IMAGE =
      DockerImageName.parse("opensearchproject/opensearch:3.5.0");
  private static final String INDEX_NAME = "approvals";
  private static final String UNREACHABLE_HOST = "http://localhost:1";
  private static final OpenSearchContainer<?> CONTAINER =
      new OpenSearchContainer<>(OPEN_SEARCH_IMAGE);

  private static OpenSearchClient openSearchClient;
  private static SearchClient searchClient;

  @BeforeAll
  static void startContainer() {
    CONTAINER.start();
    openSearchClient = openSearchClient(CONTAINER.getHttpHostAddress());
    searchClient = new SearchClient(openSearchClient, INDEX_NAME);
  }

  @AfterAll
  static void stopContainer() {
    CONTAINER.stop();
  }

  @Test
  void shouldIndexDocument() throws IOException {
    var document = randomIndexDocument();

    searchClient.index(document);

    assertEquals(document, getIndexedDocument(document.identifier()));
  }

  @Test
  void shouldThrowSearchClientExceptionWhenIndexingFails() {
    var client = new SearchClient(openSearchClient(UNREACHABLE_HOST), INDEX_NAME);

    assertThrows(SearchClientException.class, () -> client.index(randomIndexDocument()));
  }

  private static IndexDocument getIndexedDocument(UUID identifier) throws IOException {
    var request = new GetRequest.Builder().index(INDEX_NAME).id(identifier.toString()).build();
    return openSearchClient.get(request, IndexDocument.class).source();
  }

  private static OpenSearchClient openSearchClient(String hostAddress) {
    var transport =
        ApacheHttpClient5TransportBuilder.builder(HttpHost.create(URI.create(hostAddress)))
            .setMapper(new JacksonJsonpMapper())
            .build();
    return new OpenSearchClient(transport);
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
