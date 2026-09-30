package no.sikt.nva.approvals.rest;

import static no.sikt.nva.approvals.utils.TestUtils.randomIdentifiers;
import static no.sikt.nva.approvals.validation.RequestConstraints.MAX_IDENTIFIERS;
import static no.sikt.nva.approvals.validation.RequestConstraints.MAX_IDENTIFIER_NAME_LENGTH;
import static no.sikt.nva.approvals.validation.RequestConstraints.MAX_IDENTIFIER_VALUE_LENGTH;
import static no.sikt.nva.approvals.validation.RequestConstraints.MAX_URI_LENGTH;
import static no.sikt.nva.approvals.validation.RequestValidator.validateBody;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import no.sikt.nva.approvals.domain.NamedIdentifier;
import nva.commons.apigateway.exceptions.BadRequestException;
import nva.commons.apigateway.exceptions.ValidationError;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class CreateApprovalRequestTest {

  private static final String CHARACTER = "a";
  private static final String MANDATORY_MESSAGE = "Is mandatory";
  private static final String IDENTIFIERS_SIZE_MESSAGE =
      "Between 1 and 20 identifiers are required";
  private static final String NAME_TOO_LONG_MESSAGE = "Must be at most 100 characters long";
  private static final String VALUE_TOO_LONG_MESSAGE = "Must be at most 1000 characters long";
  private static final String URI_TOO_LONG_MESSAGE = "Must be at most 1024 characters long";
  private static final String NAME_PATTERN_MESSAGE =
      "May only contain letters, digits, hyphen and underscore";
  private static final String DUPLICATE_MESSAGE =
      "Identifier with the same name and value is provided more than once";
  private static final String IDENTIFIERS_POINTER = "/identifiers";
  private static final String SOURCE_POINTER = "/source";
  private static final String BASE_URI = "https://example.com/";

  @Test
  void shouldAcceptMaximumNumberOfIdentifiers() {
    var request = new CreateApprovalRequest(randomIdentifiers(MAX_IDENTIFIERS), randomUri());

    assertDoesNotThrow(() -> validateBody(request));
  }

  @Test
  void shouldRejectMoreThanMaximumNumberOfIdentifiers() {
    var request = new CreateApprovalRequest(randomIdentifiers(MAX_IDENTIFIERS + 1), randomUri());

    assertEquals(
        List.of(new ValidationError(IDENTIFIERS_SIZE_MESSAGE, IDENTIFIERS_POINTER)),
        validationErrors(request));
  }

  @Test
  void shouldRejectEmptyIdentifiers() {
    var request = new CreateApprovalRequest(List.of(), randomUri());

    assertEquals(
        List.of(new ValidationError(IDENTIFIERS_SIZE_MESSAGE, IDENTIFIERS_POINTER)),
        validationErrors(request));
  }

  @Test
  void shouldRejectMissingIdentifiers() {
    var request = new CreateApprovalRequest(null, randomUri());

    assertEquals(
        List.of(new ValidationError(MANDATORY_MESSAGE, IDENTIFIERS_POINTER)),
        validationErrors(request));
  }

  @Test
  void shouldRejectMissingSource() {
    var request = new CreateApprovalRequest(randomIdentifiers(1), null);

    assertEquals(
        List.of(new ValidationError(MANDATORY_MESSAGE, SOURCE_POINTER)), validationErrors(request));
  }

  @Test
  void shouldAcceptIdentifierNameAndValueOfMaximumLength() {
    var identifier =
        new NamedIdentifier(text(MAX_IDENTIFIER_NAME_LENGTH), text(MAX_IDENTIFIER_VALUE_LENGTH));
    var request = new CreateApprovalRequest(List.of(identifier), randomUri());

    assertDoesNotThrow(() -> validateBody(request));
  }

  @Test
  void shouldPointToIdentifierNameThatIsTooLong() {
    var identifiers =
        identifiersWith(new NamedIdentifier(text(MAX_IDENTIFIER_NAME_LENGTH + 1), randomString()));
    var request = new CreateApprovalRequest(identifiers, randomUri());

    assertEquals(
        List.of(new ValidationError(NAME_TOO_LONG_MESSAGE, "/identifiers/2/name")),
        validationErrors(request));
  }

  @Test
  void shouldPointToIdentifierValueThatIsTooLong() {
    var identifiers =
        identifiersWith(new NamedIdentifier(randomString(), text(MAX_IDENTIFIER_VALUE_LENGTH + 1)));
    var request = new CreateApprovalRequest(identifiers, randomUri());

    assertEquals(
        List.of(new ValidationError(VALUE_TOO_LONG_MESSAGE, "/identifiers/2/value")),
        validationErrors(request));
  }

  @Test
  void shouldAcceptSourceOfMaximumLength() {
    var request = new CreateApprovalRequest(randomIdentifiers(1), uriOfLength(MAX_URI_LENGTH));

    assertDoesNotThrow(() -> validateBody(request));
  }

  @Test
  void shouldRejectSourceThatIsTooLong() {
    var request = new CreateApprovalRequest(randomIdentifiers(1), uriOfLength(MAX_URI_LENGTH + 1));

    assertEquals(
        List.of(new ValidationError(URI_TOO_LONG_MESSAGE, SOURCE_POINTER)),
        validationErrors(request));
  }

  @Test
  void shouldPointToMissingIdentifier() {
    var identifiers = new ArrayList<>(randomIdentifiers(1));
    identifiers.add(null);
    var request = new CreateApprovalRequest(identifiers, randomUri());

    assertEquals(
        List.of(new ValidationError(MANDATORY_MESSAGE, "/identifiers/1")),
        validationErrors(request));
  }

  @Test
  void shouldPointToMissingIdentifierNameAndValue() {
    var request = new CreateApprovalRequest(List.of(new NamedIdentifier(null, null)), randomUri());

    assertEquals(
        List.of(
            new ValidationError(MANDATORY_MESSAGE, "/identifiers/0/name"),
            new ValidationError(MANDATORY_MESSAGE, "/identifiers/0/value")),
        validationErrors(request));
  }

  @ParameterizedTest
  @ValueSource(strings = {"a#b", "REK 2", "rek.2", "rek/2", "rek:2", "æøå", ""})
  void shouldRejectIdentifierNameWithUnsupportedCharacters(String name) {
    var request =
        new CreateApprovalRequest(List.of(new NamedIdentifier(name, randomString())), randomUri());

    assertEquals(
        List.of(new ValidationError(NAME_PATTERN_MESSAGE, "/identifiers/0/name")),
        validationErrors(request));
  }

  @ParameterizedTest
  @ValueSource(strings = {"DMP", "dmp", "  DMP  ", "apitest-uib", "rek_2", "REK2"})
  void shouldAcceptSupportedIdentifierNames(String name) {
    var request =
        new CreateApprovalRequest(List.of(new NamedIdentifier(name, randomString())), randomUri());

    assertDoesNotThrow(() -> validateBody(request));
  }

  @ParameterizedTest
  @ValueSource(strings = {"DMP", "dmp", "  DMP  "})
  void shouldPointToDuplicateWhenNamesDifferOnlyByCaseOrSurroundingWhitespace(String otherName) {
    var value = randomString();
    var identifiers =
        List.of(
            new NamedIdentifier("DMP", value),
            new NamedIdentifier(randomString(), randomString()),
            new NamedIdentifier(otherName, value));
    var request = new CreateApprovalRequest(identifiers, randomUri());

    assertEquals(
        List.of(new ValidationError(DUPLICATE_MESSAGE, "/identifiers/2")),
        validationErrors(request));
  }

  @Test
  void shouldAcceptIdentifiersSharingNameWithDifferentValues() {
    var identifiers =
        List.of(
            new NamedIdentifier("DMP", randomString()), new NamedIdentifier("DMP", randomString()));
    var request = new CreateApprovalRequest(identifiers, randomUri());

    assertDoesNotThrow(() -> validateBody(request));
  }

  @Test
  void shouldReportEveryInvalidField() {
    var identifiers =
        List.of(
            new NamedIdentifier(text(MAX_IDENTIFIER_NAME_LENGTH + 1), randomString()),
            new NamedIdentifier(randomString(), text(MAX_IDENTIFIER_VALUE_LENGTH + 1)));
    var request = new CreateApprovalRequest(identifiers, null);

    assertEquals(
        List.of(
            new ValidationError(NAME_TOO_LONG_MESSAGE, "/identifiers/0/name"),
            new ValidationError(VALUE_TOO_LONG_MESSAGE, "/identifiers/1/value"),
            new ValidationError(MANDATORY_MESSAGE, SOURCE_POINTER)),
        validationErrors(request));
  }

  private static List<ValidationError> validationErrors(CreateApprovalRequest request) {
    return assertThrows(BadRequestException.class, () -> validateBody(request)).getErrors();
  }

  private static List<NamedIdentifier> identifiersWith(NamedIdentifier invalidIdentifier) {
    var identifiers = new ArrayList<>(randomIdentifiers(2));
    identifiers.add(invalidIdentifier);
    return identifiers;
  }

  private static String text(int length) {
    return CHARACTER.repeat(length);
  }

  private static URI uriOfLength(int length) {
    return URI.create(BASE_URI + text(length - BASE_URI.length()));
  }
}
