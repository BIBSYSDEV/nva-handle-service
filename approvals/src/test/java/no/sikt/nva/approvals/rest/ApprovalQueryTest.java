package no.sikt.nva.approvals.rest;

import static no.sikt.nva.approvals.utils.TestUtils.randomHandle;
import static no.sikt.nva.approvals.validation.RequestConstraints.MAX_IDENTIFIER_NAME_LENGTH;
import static no.sikt.nva.approvals.validation.RequestConstraints.MAX_URI_LENGTH;
import static no.sikt.nva.approvals.validation.RequestValidator.validateQueryParameters;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import no.sikt.nva.approvals.domain.NamedIdentifier;
import nva.commons.apigateway.exceptions.BadRequestException;
import nva.commons.apigateway.exceptions.ValidationError;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ApprovalQueryTest {

  private static final String HANDLE_PARAMETER = "handle";
  private static final String NAME_PARAMETER = "name";
  private static final String VALUE_PARAMETER = "value";
  private static final String QUERY_POINTER = "handle";
  private static final String MANDATORY_MESSAGE = "Is mandatory";
  private static final String NAME_TOO_LONG_MESSAGE = "Must be at most 100 characters long";
  private static final String NOT_A_HANDLE_MESSAGE = "Must be the handle of an approval";
  private static final String NAME_PATTERN_MESSAGE =
      "May only contain letters, digits, hyphen and underscore";
  private static final String MISSING_QUERY_PARAMETERS_MESSAGE =
      "Missing query parameters. Use 'handle' or 'name' and 'value'";
  private static final String CONFLICTING_QUERY_PARAMETERS_MESSAGE =
      "Use either 'handle' or 'name' and 'value', not both";
  private static final String VALID_HANDLE = "https://hdl.handle.net/11250.1/12345";
  private static final String BLANK = " ";

  @Test
  void shouldAcceptHandleLookup() {
    var query = new ApprovalQuery(VALID_HANDLE, null, null);

    assertDoesNotThrow(() -> validateQueryParameters(query));
    assertTrue(query.isHandleLookup());
  }

  @Test
  void shouldAcceptNamedIdentifierLookup() {
    var query = new ApprovalQuery(null, "REK", randomString());

    assertDoesNotThrow(() -> validateQueryParameters(query));
    assertFalse(query.isHandleLookup());
    assertEquals(new NamedIdentifier("REK", query.value()), query.toNamedIdentifier());
  }

  @Test
  void shouldReportMissingQueryParameters() {
    var query = new ApprovalQuery(null, null, null);

    assertEquals(
        List.of(new ValidationError(MISSING_QUERY_PARAMETERS_MESSAGE, QUERY_POINTER)),
        validationErrors(query));
  }

  @Test
  void shouldReportHandleCombinedWithNamedIdentifier() {
    var query = new ApprovalQuery(VALID_HANDLE, "REK", randomString());

    assertEquals(
        List.of(new ValidationError(CONFLICTING_QUERY_PARAMETERS_MESSAGE, QUERY_POINTER)),
        validationErrors(query));
  }

  @Test
  void shouldPointToValueWhenOnlyNameIsProvided() {
    var query = new ApprovalQuery(null, "REK", null);

    assertEquals(
        List.of(new ValidationError(MANDATORY_MESSAGE, VALUE_PARAMETER)), validationErrors(query));
  }

  @Test
  void shouldReportInvalidNameTogetherWithMissingValue() {
    var query = new ApprovalQuery(null, "a".repeat(MAX_IDENTIFIER_NAME_LENGTH + 1), null);

    assertEquals(
        List.of(
            new ValidationError(NAME_TOO_LONG_MESSAGE, NAME_PARAMETER),
            new ValidationError(MANDATORY_MESSAGE, VALUE_PARAMETER)),
        validationErrors(query));
  }

  @Test
  void shouldPointToNameWhenOnlyValueIsProvided() {
    var query = new ApprovalQuery(null, null, randomString());

    assertEquals(
        List.of(new ValidationError(MANDATORY_MESSAGE, NAME_PARAMETER)), validationErrors(query));
  }

  @ParameterizedTest
  @ValueSource(strings = {"not-a-valid-handle", "https://example.com/11250.1/12345", "%%"})
  void shouldPointToHandleWhenInvalid(String handle) {
    var query = new ApprovalQuery(handle, null, null);

    assertEquals(
        List.of(new ValidationError(NOT_A_HANDLE_MESSAGE, HANDLE_PARAMETER)),
        validationErrors(query));
  }

  @Test
  void shouldPointToHandleWhenTooLong() {
    var query = new ApprovalQuery(VALID_HANDLE + "a".repeat(MAX_URI_LENGTH), null, null);

    assertEquals(
        List.of(new ValidationError(NOT_A_HANDLE_MESSAGE, HANDLE_PARAMETER)),
        validationErrors(query));
  }

  @Test
  void shouldPointToNameWithUnsupportedCharacters() {
    var query = new ApprovalQuery(null, "REK 2", randomString());

    assertEquals(
        List.of(new ValidationError(NAME_PATTERN_MESSAGE, NAME_PARAMETER)),
        validationErrors(query));
  }

  @Test
  void shouldTreatBlankParametersAsMissing() {
    var queryParameters = new HashMap<String, String>();
    queryParameters.put(HANDLE_PARAMETER, BLANK);
    queryParameters.put(NAME_PARAMETER, "REK");
    queryParameters.put(VALUE_PARAMETER, BLANK);

    var query = ApprovalQuery.fromQueryParameters(queryParameters);

    assertEquals(new ApprovalQuery(null, "REK", null), query);
  }

  @Test
  void shouldHandleMissingQueryParameterMap() {
    assertTrue(ApprovalQuery.fromQueryParameters(null).isEmpty());
  }

  @Test
  void shouldNotBeEmptyWhenAnyQueryParameterIsGiven() {
    assertFalse(
        ApprovalQuery.fromQueryParameters(Map.of(VALUE_PARAMETER, randomString())).isEmpty());
  }

  @Test
  void shouldBeEmptyWhenQueryParametersAreBlank() {
    assertTrue(ApprovalQuery.fromQueryParameters(Map.of(NAME_PARAMETER, BLANK)).isEmpty());
  }

  @Test
  void shouldCreateHandle() {
    var handle = randomHandle();

    assertEquals(handle, new ApprovalQuery(handle.toString(), null, null).toHandle());
  }

  private static List<ValidationError> validationErrors(ApprovalQuery query) {
    return assertThrows(BadRequestException.class, () -> validateQueryParameters(query))
        .getErrors();
  }
}
