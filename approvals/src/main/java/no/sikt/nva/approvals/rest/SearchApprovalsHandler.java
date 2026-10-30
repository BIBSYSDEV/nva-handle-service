package no.sikt.nva.approvals.rest;

import static java.net.HttpURLConnection.HTTP_OK;
import static no.sikt.nva.approvals.utils.RequestUtils.getApiHost;

import com.amazonaws.services.lambda.runtime.Context;
import no.sikt.nva.approvals.index.Query;
import no.sikt.nva.approvals.index.SearchClient;
import no.unit.nva.commons.pagination.PaginatedSearchResult;
import nva.commons.apigateway.ApiGatewayHandler;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;

public class SearchApprovalsHandler
    extends ApiGatewayHandler<Void, PaginatedSearchResult<ApprovalHit>> {

  private static final String QUERY_PARAMETER = "query";
  private static final String OFFSET_PARAMETER = "offset";
  private static final String SIZE_PARAMETER = "size";

  private final SearchClient searchClient;
  private final String apiHost;

  @JacocoGenerated
  public SearchApprovalsHandler() {
    var environment = new Environment();
    this(SearchClient.defaultInstance(environment), environment);
  }

  public SearchApprovalsHandler(SearchClient searchClient, Environment environment) {
    super(Void.class, environment);
    this.searchClient = searchClient;
    this.apiHost = getApiHost(environment);
  }

  @Override
  protected void validateRequest(Void input, RequestInfo requestInfo, Context context) {}

  @Override
  protected PaginatedSearchResult<ApprovalHit> processInput(
      Void input, RequestInfo requestInfo, Context context) throws ApiGatewayException {
    var query = toQuery(requestInfo);
    return ApprovalSearchResults.from(searchClient.search(query), query, apiHost);
  }

  private static Query toQuery(RequestInfo requestInfo) {
    return new Query(
        requestInfo.getQueryParameterOpt(QUERY_PARAMETER).orElse(null),
        intParameter(requestInfo, OFFSET_PARAMETER, Query.DEFAULT_OFFSET),
        intParameter(requestInfo, SIZE_PARAMETER, Query.DEFAULT_SIZE));
  }

  private static int intParameter(RequestInfo requestInfo, String name, int defaultValue) {
    return requestInfo.getQueryParameterOpt(name).map(Integer::parseInt).orElse(defaultValue);
  }

  @Override
  protected Integer getSuccessStatusCode(Void input, PaginatedSearchResult<ApprovalHit> output) {
    return HTTP_OK;
  }
}
