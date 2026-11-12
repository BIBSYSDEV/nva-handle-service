package no.sikt.nva.approvals.rest;

import static java.net.HttpURLConnection.HTTP_OK;
import static no.sikt.nva.approvals.utils.RequestUtils.getApiHost;
import static no.sikt.nva.approvals.utils.RequestUtils.toApiGatewayException;
import static no.sikt.nva.approvals.utils.UriConstants.CURSOR_QUERY_PARAMETER;
import static no.sikt.nva.approvals.utils.UriConstants.IDENTIFIER_QUERY_PARAMETER;
import static no.sikt.nva.approvals.utils.UriConstants.ORGANIZATION_QUERY_PARAMETER;
import static no.sikt.nva.approvals.utils.UriConstants.PERSON_QUERY_PARAMETER;
import static no.sikt.nva.approvals.utils.UriConstants.SEARCH_QUERY_PARAMETER;
import static no.sikt.nva.approvals.utils.UriConstants.SIZE_QUERY_PARAMETER;
import static no.sikt.nva.approvals.validation.RequestConstraints.INVALID_CURSOR_MESSAGE;
import static no.sikt.nva.approvals.validation.RequestConstraints.INVALID_IDENTIFIER_MESSAGE;
import static no.sikt.nva.approvals.validation.RequestConstraints.INVALID_ORGANIZATION_MESSAGE;
import static no.sikt.nva.approvals.validation.RequestConstraints.INVALID_PERSON_MESSAGE;
import static no.sikt.nva.approvals.validation.RequestConstraints.INVALID_SIZE_MESSAGE;
import static no.sikt.nva.approvals.validation.RequestConstraints.MAX_SEARCH_SIZE;
import static no.sikt.nva.approvals.validation.RequestConstraints.UNSUPPORTED_PARAMETER_MESSAGE;
import static no.sikt.nva.approvals.validation.RequestValidator.badRequest;
import static nva.commons.apigateway.MediaTypes.APPLICATION_JSON_LD;
import static nva.commons.core.attempt.Try.attempt;

import com.amazonaws.services.lambda.runtime.Context;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;
import no.sikt.nva.approvals.index.IdentifierFilter;
import no.sikt.nva.approvals.index.OrganizationFilter;
import no.sikt.nva.approvals.index.PersonFilter;
import no.sikt.nva.approvals.index.Query;
import no.sikt.nva.approvals.index.SearchClient;
import no.sikt.nva.approvals.index.SearchFilter;
import no.sikt.nva.approvals.utils.OrganizationIdentifier;
import no.sikt.nva.approvals.utils.PersonIdentifier;
import nva.commons.apigateway.ApiGatewayHandler;
import nva.commons.apigateway.MediaType;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.BadRequestException;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;

public class SearchApprovalsHandler extends ApiGatewayHandler<Void, ApprovalSearchResponse> {

  private static final int MINIMUM_SIZE = 0;
  private static final String PATH_SEPARATOR = "/";
  private static final Set<String> SUPPORTED_PARAMETERS = SearchParameter.parameterNames();

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
  protected List<MediaType> listSupportedMediaTypes() {
    return List.of(APPLICATION_JSON_LD, MediaType.JSON_UTF_8);
  }

  @Override
  protected void validateRequest(Void input, RequestInfo requestInfo, Context context)
      throws ApiGatewayException {
    getDefaultResponseContentTypeHeaderValue(requestInfo);
    ensureParametersAreSupported(requestInfo);
  }

  private static void ensureParametersAreSupported(RequestInfo requestInfo)
      throws BadRequestException {
    var submitted =
        Optional.ofNullable(requestInfo.getQueryParameters()).orElseGet(Map::of).keySet();
    if (!SUPPORTED_PARAMETERS.containsAll(submitted)) {
      throw new BadRequestException(
          UNSUPPORTED_PARAMETER_MESSAGE.formatted(SearchParameter.parameterNameList()));
    }
  }

  @Override
  protected ApprovalSearchResponse processInput(
      Void input, RequestInfo requestInfo, Context context) throws ApiGatewayException {
    var query = toQuery(requestInfo);
    return attempt(() -> searchClient.search(query))
        .map(response -> ApprovalSearchResponse.from(response, query, apiHost))
        .orElseThrow(failure -> toApiGatewayException(failure.getException()));
  }

  private static Query toQuery(RequestInfo requestInfo) throws BadRequestException {
    return new Query(
        requestInfo.getQueryParameterOpt(SEARCH_QUERY_PARAMETER).orElse(null),
        filters(requestInfo),
        cursor(requestInfo),
        size(requestInfo));
  }

  private static List<SearchFilter> filters(RequestInfo requestInfo) throws BadRequestException {
    return Stream.of(
            personFilter(requestInfo),
            organizationFilter(requestInfo),
            identifierFilter(requestInfo))
        .flatMap(Optional::stream)
        .toList();
  }

  private static Optional<SearchFilter> identifierFilter(RequestInfo requestInfo)
      throws BadRequestException {
    var requested = requestInfo.getQueryParameterOpt(IDENTIFIER_QUERY_PARAMETER);
    return requested.isPresent() ? Optional.of(namedIdentifier(requested.get())) : Optional.empty();
  }

  private static IdentifierFilter namedIdentifier(String value) throws BadRequestException {
    return attempt(() -> IdentifierFilter.fromParameter(value))
        .orElseThrow(_ -> badRequest(INVALID_IDENTIFIER_MESSAGE, IDENTIFIER_QUERY_PARAMETER));
  }

  private static Optional<SearchFilter> personFilter(RequestInfo requestInfo)
      throws BadRequestException {
    var requested = requestInfo.getQueryParameterOpt(PERSON_QUERY_PARAMETER);
    return requested.isPresent()
        ? Optional.of(new PersonFilter(personIdentifier(requested.get())))
        : Optional.empty();
  }

  private static Optional<SearchFilter> organizationFilter(RequestInfo requestInfo)
      throws BadRequestException {
    var requested = requestInfo.getQueryParameterOpt(ORGANIZATION_QUERY_PARAMETER);
    return requested.isPresent()
        ? Optional.of(new OrganizationFilter(organizationIdentifier(requested.get())))
        : Optional.empty();
  }

  private static String personIdentifier(String value) throws BadRequestException {
    return attempt(() -> toPersonIdentifier(value).value())
        .orElseThrow(_ -> badRequest(INVALID_PERSON_MESSAGE, PERSON_QUERY_PARAMETER));
  }

  private static PersonIdentifier toPersonIdentifier(String value) {
    return value.contains(PATH_SEPARATOR)
        ? PersonIdentifier.fromUri(URI.create(value))
        : new PersonIdentifier(value);
  }

  private static String organizationIdentifier(String value) throws BadRequestException {
    return attempt(() -> toOrganizationIdentifier(value).value())
        .orElseThrow(_ -> badRequest(INVALID_ORGANIZATION_MESSAGE, ORGANIZATION_QUERY_PARAMETER));
  }

  private static OrganizationIdentifier toOrganizationIdentifier(String value) {
    return value.contains(PATH_SEPARATOR)
        ? OrganizationIdentifier.fromUri(URI.create(value))
        : new OrganizationIdentifier(value);
  }

  private static String cursor(RequestInfo requestInfo) throws BadRequestException {
    var requested = requestInfo.getQueryParameterOpt(CURSOR_QUERY_PARAMETER);
    return requested.isPresent() ? approvalIdentifier(requested.get()) : null;
  }

  private static String approvalIdentifier(String value) throws BadRequestException {
    return attempt(() -> UUID.fromString(value).toString())
        .orElseThrow(_ -> badRequest(INVALID_CURSOR_MESSAGE, CURSOR_QUERY_PARAMETER));
  }

  private static int size(RequestInfo requestInfo) throws BadRequestException {
    var requested = requestInfo.getQueryParameterOpt(SIZE_QUERY_PARAMETER);
    return requested.isPresent() ? boundedSize(requested.get()) : Query.DEFAULT_SIZE;
  }

  private static int boundedSize(String value) throws BadRequestException {
    var size =
        attempt(() -> Integer.parseInt(value))
            .orElseThrow(failure -> badRequest(INVALID_SIZE_MESSAGE, SIZE_QUERY_PARAMETER));
    if (size < MINIMUM_SIZE || size > MAX_SEARCH_SIZE) {
      throw badRequest(INVALID_SIZE_MESSAGE, SIZE_QUERY_PARAMETER);
    }
    return size;
  }

  @Override
  protected Integer getSuccessStatusCode(Void input, ApprovalSearchResponse output) {
    return HTTP_OK;
  }
}
