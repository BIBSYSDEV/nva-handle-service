package no.sikt.nva.approvals.rest;

import static java.util.Objects.isNull;
import static no.sikt.nva.approvals.validation.RequestConstraints.MAX_SEARCH_OFFSET;
import static no.sikt.nva.approvals.validation.RequestConstraints.MAX_SEARCH_SIZE;
import static no.sikt.nva.approvals.validation.RequestConstraints.NOT_A_NUMBER_MESSAGE;
import static no.sikt.nva.approvals.validation.RequestConstraints.TOO_LARGE_MESSAGE;
import static no.sikt.nva.approvals.validation.RequestConstraints.TOO_SMALL_MESSAGE;
import static no.sikt.nva.approvals.validation.RequestConstraints.UNKNOWN_QUERY_PARAMETER_MESSAGE;
import static no.sikt.nva.approvals.validation.RequestValidator.badRequest;
import static nva.commons.core.StringUtils.isNotBlank;
import static nva.commons.core.attempt.Try.attempt;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import no.sikt.nva.approvals.index.Query;
import nva.commons.apigateway.exceptions.BadRequestException;

public record SearchQueryParameters(
    String query,
    @Min(value = 0, message = TOO_SMALL_MESSAGE)
        @Max(value = MAX_SEARCH_OFFSET, message = TOO_LARGE_MESSAGE)
        Integer offset,
    @Min(value = 0, message = TOO_SMALL_MESSAGE)
        @Max(value = MAX_SEARCH_SIZE, message = TOO_LARGE_MESSAGE)
        Integer size) {

  private static final String QUERY_PARAMETER = "query";
  private static final String OFFSET_PARAMETER = "offset";
  private static final String SIZE_PARAMETER = "size";
  private static final Set<String> SUPPORTED_PARAMETERS =
      Set.of(QUERY_PARAMETER, OFFSET_PARAMETER, SIZE_PARAMETER);

  public static SearchQueryParameters fromQueryParameters(Map<String, String> queryParameters)
      throws BadRequestException {
    var parameters = isNull(queryParameters) ? Map.<String, String>of() : queryParameters;
    ensureSupportedParameters(parameters);
    return new SearchQueryParameters(
        parameter(parameters, QUERY_PARAMETER),
        numericParameter(parameters, OFFSET_PARAMETER),
        numericParameter(parameters, SIZE_PARAMETER));
  }

  public Query toQuery() {
    return new Query(
        query,
        Optional.ofNullable(offset).orElse(Query.DEFAULT_OFFSET),
        Optional.ofNullable(size).orElse(Query.DEFAULT_SIZE));
  }

  private static void ensureSupportedParameters(Map<String, String> queryParameters)
      throws BadRequestException {
    var unsupported =
        queryParameters.keySet().stream()
            .filter(Predicate.not(SUPPORTED_PARAMETERS::contains))
            .sorted()
            .toList();
    if (!unsupported.isEmpty()) {
      throw badRequest(UNKNOWN_QUERY_PARAMETER_MESSAGE, unsupported);
    }
  }

  private static Integer numericParameter(Map<String, String> queryParameters, String parameterName)
      throws BadRequestException {
    var value = parameter(queryParameters, parameterName);
    return isNull(value)
        ? null
        : attempt(() -> Integer.parseInt(value))
            .orElseThrow(_ -> badRequest(NOT_A_NUMBER_MESSAGE, parameterName));
  }

  private static String parameter(Map<String, String> queryParameters, String parameterName) {
    var value = queryParameters.get(parameterName);
    return isNotBlank(value) ? value : null;
  }
}
