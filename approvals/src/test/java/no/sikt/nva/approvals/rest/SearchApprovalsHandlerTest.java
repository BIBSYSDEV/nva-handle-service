package no.sikt.nva.approvals.rest;

import static java.net.HttpURLConnection.HTTP_BAD_GATEWAY;
import static java.net.HttpURLConnection.HTTP_BAD_REQUEST;
import static java.net.HttpURLConnection.HTTP_NOT_ACCEPTABLE;
import static java.net.HttpURLConnection.HTTP_OK;
import static java.util.UUID.randomUUID;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static nva.commons.apigateway.MediaTypes.APPLICATION_JSON_LD;
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
import no.sikt.nva.approvals.index.PersonFilter;
import no.sikt.nva.approvals.index.Query;
import no.sikt.nva.approvals.index.SearchClient;
import no.sikt.nva.approvals.index.SearchClientException;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.stubs.FakeContext;
import no.unit.nva.testutils.HandlerRequestBuilder;
import nva.commons.apigateway.GatewayResponse;
import nva.commons.core.Environment;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.opensearch.client.opensearch.core.SearchResponse;
import org.opensearch.client.opensearch.core.search.TotalHitsRelation;

class SearchApprovalsHandlerTest {

  private static final Environment ENVIRONMENT = new Environment();
  private static final FakeContext CONTEXT = new FakeContext();
  private static final String QUERY_PARAMETER = "query";
  private static final String CURSOR_PARAMETER = "cursor";
  private static final String SIZE_PARAMETER = "size";
  private static final String PERSON_PARAMETER = "person";
  private static final String PERSON_ID = "12345";

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
  void shouldSearchWithRequestedCursorAndSize() throws IOException {
    var query = randomString();
    var cursor = randomUUID().toString();

    send(request(Map.of(QUERY_PARAMETER, query, CURSOR_PARAMETER, cursor, SIZE_PARAMETER, "5")));

    verify(searchClient).search(new Query(query, List.of(), cursor, 5));
  }

  @Test
  void shouldSearchWithoutQueryWhenQueryParameterIsMissing() throws IOException {
    send(request(Map.of()));

    verify(searchClient).search(new Query(null));
  }

  @Test
  void shouldSearchWithPersonIdentifierFromRequest() throws IOException {
    send(request(Map.of(PERSON_PARAMETER, PERSON_ID)));

    verify(searchClient).search(new Query(null, List.of(new PersonFilter(PERSON_ID)), null, 10));
  }

  @Test
  void shouldSearchWithIdentifierExtractedFromPersonUri() throws IOException {
    var personUri = "https://api.example.org/cristin/person/" + PERSON_ID;

    send(request(Map.of(PERSON_PARAMETER, personUri)));

    verify(searchClient).search(new Query(null, List.of(new PersonFilter(PERSON_ID)), null, 10));
  }

  @Test
  void shouldReturnBadRequestWhenPersonIsNotAPersonUri() throws IOException {
    var response = send(request(Map.of(PERSON_PARAMETER, "https://api.example.org/people/12345")));

    assertThat(response.getStatusCode(), equalTo(HTTP_BAD_REQUEST));
  }

  @Test
  void shouldReturnBadRequestWhenPersonPathIsNotAPerson() throws IOException {
    var response = send(request(Map.of(PERSON_PARAMETER, "/people/12345")));

    assertThat(response.getStatusCode(), equalTo(HTTP_BAD_REQUEST));
  }

  @Test
  void shouldReturnBadRequestWhenSizeIsNotANumber() throws IOException {
    var response = send(request(Map.of(SIZE_PARAMETER, "abc")));

    assertThat(response.getStatusCode(), equalTo(HTTP_BAD_REQUEST));
  }

  @Test
  void shouldReturnOkWhenSizeIsZero() throws IOException {
    var response = send(request(Map.of(SIZE_PARAMETER, "0")));

    assertThat(response.getStatusCode(), equalTo(HTTP_OK));
  }

  @Test
  void shouldReturnBadRequestWhenSizeIsNegative() throws IOException {
    var response = send(request(Map.of(SIZE_PARAMETER, "-1")));

    assertThat(response.getStatusCode(), equalTo(HTTP_BAD_REQUEST));
  }

  @Test
  void shouldReturnBadRequestWhenSizeExceedsMaximum() throws IOException {
    var response = send(request(Map.of(SIZE_PARAMETER, "101")));

    assertThat(response.getStatusCode(), equalTo(HTTP_BAD_REQUEST));
  }

  @Test
  void shouldReturnOkWhenAcceptHeaderIsJsonLd() throws IOException {
    var response = send(requestWithAccept(APPLICATION_JSON_LD.toString()));

    assertThat(response.getStatusCode(), equalTo(HTTP_OK));
  }

  @Test
  void shouldReturnNotAcceptableWhenAcceptHeaderIsUnsupported() throws IOException {
    var response = send(requestWithAccept("application/xml"));

    assertThat(response.getStatusCode(), equalTo(HTTP_NOT_ACCEPTABLE));
  }

  @Test
  void shouldReturnBadRequestWhenQueryParameterIsNotSupported() throws IOException {
    var response = send(request(Map.of("handle", randomString())));

    assertThat(response.getStatusCode(), equalTo(HTTP_BAD_REQUEST));
  }

  @Test
  void shouldReturnBadRequestWhenCursorIsNotAnApprovalIdentifier() throws IOException {
    var response = send(request(Map.of(CURSOR_PARAMETER, randomString())));

    assertThat(response.getStatusCode(), equalTo(HTTP_BAD_REQUEST));
  }

  @Test
  void shouldReturnBadGatewayWhenSearchFails() throws IOException {
    when(searchClient.search(any(Query.class)))
        .thenThrow(new SearchClientException(randomString(), new IllegalStateException()));

    var response = send(request(Map.of(QUERY_PARAMETER, randomString())));

    assertThat(response.getStatusCode(), equalTo(HTTP_BAD_GATEWAY));
  }

  @Test
  void shouldReturnOk() throws IOException {
    var response = send(request(Map.of(QUERY_PARAMETER, randomString())));

    assertThat(response.getStatusCode(), equalTo(HTTP_OK));
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

  private GatewayResponse<ApprovalSearchResponse> send(InputStream request) throws IOException {
    handler.handleRequest(request, output, CONTEXT);
    return GatewayResponse.of(output);
  }

  private static InputStream requestWithAccept(String acceptHeader) throws IOException {
    return new HandlerRequestBuilder<Void>(JsonUtils.dtoObjectMapper)
        .withQueryParameters(Map.of(QUERY_PARAMETER, randomString()))
        .withHeaders(Map.of("Accept", acceptHeader))
        .build();
  }

  private static InputStream request(Map<String, String> queryParameters) throws IOException {
    return new HandlerRequestBuilder<Void>(JsonUtils.dtoObjectMapper)
        .withQueryParameters(queryParameters)
        .build();
  }
}
