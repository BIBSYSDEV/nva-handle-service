package no.sikt.nva.approvals.rest;

import static no.sikt.nva.approvals.validation.RequestConstraints.MAX_SEARCH_OFFSET;
import static no.sikt.nva.approvals.validation.RequestConstraints.MAX_SEARCH_SIZE;
import static no.sikt.nva.approvals.validation.RequestValidator.validateQueryParameters;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.Map;
import no.sikt.nva.approvals.index.Query;
import nva.commons.apigateway.exceptions.BadRequestException;
import nva.commons.apigateway.exceptions.ValidationError;
import org.junit.jupiter.api.Test;

class SearchQueryParametersTest {

  private static final String NOT_A_NUMBER_MESSAGE = "Must be a whole number";
  private static final String UNKNOWN_QUERY_PARAMETER_MESSAGE =
      "Unknown query parameter. Use 'query', 'offset' or 'size'";
  private static final String TOO_SMALL_MESSAGE = "Must be at least 0";

  @Test
  void shouldRejectUnknownQueryParameterWithoutRepeatingItsName() {
    var queryParameters = Map.of("alpha", randomString(), "beta", randomString());

    var exception =
        assertThrows(
            BadRequestException.class,
            () -> SearchQueryParameters.fromQueryParameters(queryParameters));

    assertEquals(UNKNOWN_QUERY_PARAMETER_MESSAGE, exception.getMessage());
    assertEquals(List.of(), exception.getErrors());
  }

  @Test
  void shouldRejectOffsetThatIsNotANumber() {
    assertEquals(
        List.of(new ValidationError(NOT_A_NUMBER_MESSAGE, "offset")),
        errorsFromQueryParameters(Map.of("offset", "ten")));
  }

  @Test
  void shouldRejectSizeThatIsNotANumber() {
    assertEquals(
        List.of(new ValidationError(NOT_A_NUMBER_MESSAGE, "size")),
        errorsFromQueryParameters(Map.of("size", "5.5")));
  }

  @Test
  void shouldReportNegativeOffset() {
    var parameters = new SearchQueryParameters(null, -1, null);

    assertEquals(
        List.of(new ValidationError(TOO_SMALL_MESSAGE, "offset")), validationErrors(parameters));
  }

  @Test
  void shouldReportOffsetAboveMaximum() {
    var parameters = new SearchQueryParameters(null, MAX_SEARCH_OFFSET + 1, null);

    assertEquals(
        List.of(new ValidationError("Must be at most " + MAX_SEARCH_OFFSET, "offset")),
        validationErrors(parameters));
  }

  @Test
  void shouldReportSizeAboveMaximum() {
    var parameters = new SearchQueryParameters(null, null, MAX_SEARCH_SIZE + 1);

    assertEquals(
        List.of(new ValidationError("Must be at most " + MAX_SEARCH_SIZE, "size")),
        validationErrors(parameters));
  }

  @Test
  void shouldUseDefaultPagingWhenParametersAreAbsent() throws BadRequestException {
    var parameters = SearchQueryParameters.fromQueryParameters(Map.of());

    assertEquals(new Query(null), parameters.toQuery());
  }

  @Test
  void shouldConvertToQueryWithRequestedPaging() throws BadRequestException {
    var query = randomString();

    var parameters =
        SearchQueryParameters.fromQueryParameters(
            Map.of("query", query, "offset", "20", "size", "5"));

    assertEquals(new Query(query, 20, 5), parameters.toQuery());
  }

  private static List<ValidationError> errorsFromQueryParameters(
      Map<String, String> queryParameters) {
    return assertThrows(
            BadRequestException.class,
            () -> SearchQueryParameters.fromQueryParameters(queryParameters))
        .getErrors();
  }

  private static List<ValidationError> validationErrors(SearchQueryParameters parameters) {
    return assertThrows(BadRequestException.class, () -> validateQueryParameters(parameters))
        .getErrors();
  }
}
