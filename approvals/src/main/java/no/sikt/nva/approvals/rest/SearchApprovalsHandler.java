package no.sikt.nva.approvals.rest;

import static java.net.HttpURLConnection.HTTP_OK;
import static no.sikt.nva.approvals.utils.RequestUtils.getApiHost;
import static no.sikt.nva.approvals.validation.RequestValidator.validateQueryParameters;

import com.amazonaws.services.lambda.runtime.Context;
import no.sikt.nva.approvals.index.SearchClient;
import no.unit.nva.commons.pagination.PaginatedSearchResult;
import nva.commons.apigateway.ApiGatewayHandler;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;

public class SearchApprovalsHandler
    extends ApiGatewayHandler<Void, PaginatedSearchResult<ApprovalHit>> {

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
  protected void validateRequest(Void input, RequestInfo requestInfo, Context context)
      throws ApiGatewayException {
    validateQueryParameters(
        SearchQueryParameters.fromQueryParameters(requestInfo.getQueryParameters()));
  }

  @Override
  protected PaginatedSearchResult<ApprovalHit> processInput(
      Void input, RequestInfo requestInfo, Context context) throws ApiGatewayException {
    var query =
        SearchQueryParameters.fromQueryParameters(requestInfo.getQueryParameters()).toQuery();
    return ApprovalSearchResults.from(searchClient.search(query), query, apiHost);
  }

  @Override
  protected Integer getSuccessStatusCode(Void input, PaginatedSearchResult<ApprovalHit> output) {
    return HTTP_OK;
  }
}
