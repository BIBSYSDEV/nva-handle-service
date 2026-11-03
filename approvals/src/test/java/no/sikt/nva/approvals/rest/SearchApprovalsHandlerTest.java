package no.sikt.nva.approvals.rest;

import static java.net.HttpURLConnection.HTTP_BAD_REQUEST;
import static java.net.HttpURLConnection.HTTP_OK;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import no.sikt.nva.approvals.index.IndexDocument;
import no.sikt.nva.approvals.index.Query;
import no.sikt.nva.approvals.index.SearchClient;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.commons.pagination.PaginatedSearchResult;
import no.unit.nva.stubs.FakeContext;
import no.unit.nva.testutils.HandlerRequestBuilder;
import nva.commons.apigateway.GatewayResponse;
import nva.commons.core.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opensearch.client.opensearch.core.SearchResponse;
import org.opensearch.client.opensearch.core.search.TotalHitsRelation;
import org.zalando.problem.Problem;

class SearchApprovalsHandlerTest {

  private static final Environment ENVIRONMENT = new Environment();
  private static final FakeContext CONTEXT = new FakeContext();
  private static final String QUERY_PARAMETER = "query";
  private static final String OFFSET_PARAMETER = "offset";
  private static final String SIZE_PARAMETER = "size";
  private static final String UNKNOWN_QUERY_PARAMETER_MESSAGE =
      "Unknown query parameter. Use 'query', 'offset' or 'size'";

  private SearchClient searchClient;
  private SearchApprovalsHandler handler;
  private ByteArrayOutputStream output;

  @BeforeEach
  void setUp() {
    searchClient = mock(SearchClient.class);
    when(searchClient.search(any(Query.class))).thenReturn(emptyResponse());
    handler = new SearchApprovalsHandler(searchClient, ENVIRONMENT);
    output = new ByteArrayOutputStream();
  }

  @Test
  void shouldSearchWithQueryFromRequestAndDefaultPaging() throws IOException {
    var query = randomString();

    send(request(Map.of(QUERY_PARAMETER, query)));

    verify(searchClient).search(new Query(query));
  }

  @Test
  void shouldSearchWithRequestedOffsetAndSize() throws IOException {
    var query = randomString();

    send(request(Map.of(QUERY_PARAMETER, query, OFFSET_PARAMETER, "20", SIZE_PARAMETER, "5")));

    verify(searchClient).search(new Query(query, 20, 5));
  }

  @Test
  void shouldSearchWithoutQueryWhenQueryParameterIsMissing() throws IOException {
    send(request(Map.of()));

    verify(searchClient).search(new Query(null));
  }

  @Test
  void shouldReturnOk() throws IOException {
    var response = send(request(Map.of(QUERY_PARAMETER, randomString())));

    assertThat(response.getStatusCode(), equalTo(HTTP_OK));
  }

  @Test
  void shouldReturnBadRequestWhenQueryParameterIsUnknown() throws IOException {
    handler.handleRequest(
        request(Map.of(randomString(), randomString(), randomString(), randomString())),
        output,
        CONTEXT);

    var response = GatewayResponse.fromOutputStream(output, Problem.class);
    assertThat(response.getStatusCode(), equalTo(HTTP_BAD_REQUEST));
    assertThat(
        response.getBodyObject(Problem.class).getDetail(),
        equalTo(UNKNOWN_QUERY_PARAMETER_MESSAGE));
  }

  private static SearchResponse<IndexDocument> emptyResponse() {
    return new SearchResponse.Builder<IndexDocument>()
        .took(1)
        .timedOut(false)
        .shards(shards -> shards.total(1).successful(1).failed(0))
        .hits(
            hits ->
                hits.total(total -> total.value(0).relation(TotalHitsRelation.Eq)).hits(List.of()))
        .build();
  }

  private GatewayResponse<PaginatedSearchResult<ApprovalHit>> send(InputStream request)
      throws IOException {
    handler.handleRequest(request, output, CONTEXT);
    return GatewayResponse.of(output);
  }

  private static InputStream request(Map<String, String> queryParameters) throws IOException {
    return new HandlerRequestBuilder<Void>(JsonUtils.dtoObjectMapper)
        .withQueryParameters(queryParameters)
        .build();
  }
}
