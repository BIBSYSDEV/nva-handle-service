package no.sikt.nva.approvals.rest;

import static java.net.HttpURLConnection.HTTP_FORBIDDEN;
import static java.util.stream.Collectors.joining;

import java.util.Collection;
import nva.commons.apigateway.exceptions.ApiGatewayException;

public class DisallowedIdentifierNamesException extends ApiGatewayException {

  private static final String MESSAGE = "Identifier names not allowed for customer: [%s]";
  private static final String NAME_DELIMITER = ", ";

  public DisallowedIdentifierNamesException(Collection<String> disallowedIdentifierNames) {
    super(MESSAGE.formatted(joinSorted(disallowedIdentifierNames)));
  }

  @Override
  protected Integer statusCode() {
    return HTTP_FORBIDDEN;
  }

  private static String joinSorted(Collection<String> disallowedIdentifierNames) {
    return disallowedIdentifierNames.stream().sorted().collect(joining(NAME_DELIMITER));
  }
}
