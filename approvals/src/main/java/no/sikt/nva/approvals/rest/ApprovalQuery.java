package no.sikt.nva.approvals.rest;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static no.sikt.nva.approvals.validation.RequestConstraints.IDENTIFIER_NAME_PATTERN;
import static no.sikt.nva.approvals.validation.RequestConstraints.IDENTIFIER_NAME_PATTERN_MESSAGE;
import static no.sikt.nva.approvals.validation.RequestConstraints.MAX_IDENTIFIER_NAME_LENGTH;
import static no.sikt.nva.approvals.validation.RequestConstraints.MAX_IDENTIFIER_VALUE_BYTES;
import static no.sikt.nva.approvals.validation.RequestConstraints.TOO_LONG_MESSAGE;
import static nva.commons.core.StringUtils.isNotBlank;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.net.URI;
import java.util.Map;
import no.sikt.nva.approvals.domain.Handle;
import no.sikt.nva.approvals.domain.NamedIdentifier;
import no.sikt.nva.approvals.validation.HandleUri;
import no.sikt.nva.approvals.validation.Utf8Size;

@QueryCombination
public record ApprovalQuery(
    @HandleUri String handle,
    @Size(max = MAX_IDENTIFIER_NAME_LENGTH, message = TOO_LONG_MESSAGE)
        @Pattern(regexp = IDENTIFIER_NAME_PATTERN, message = IDENTIFIER_NAME_PATTERN_MESSAGE)
        String name,
    @Utf8Size(max = MAX_IDENTIFIER_VALUE_BYTES) String value) {

  private static final String HANDLE_PARAMETER = "handle";
  private static final String NAME_PARAMETER = "name";
  private static final String VALUE_PARAMETER = "value";

  public static ApprovalQuery fromQueryParameters(Map<String, String> queryParameters) {
    return new ApprovalQuery(
        parameter(queryParameters, HANDLE_PARAMETER),
        parameter(queryParameters, NAME_PARAMETER),
        parameter(queryParameters, VALUE_PARAMETER));
  }

  public boolean isEmpty() {
    return isNull(handle) && isNull(name) && isNull(value);
  }

  public boolean isHandleLookup() {
    return nonNull(handle);
  }

  public Handle toHandle() {
    return new Handle(URI.create(handle));
  }

  public NamedIdentifier toNamedIdentifier() {
    return new NamedIdentifier(name, value);
  }

  private static String parameter(Map<String, String> queryParameters, String parameterName) {
    if (isNull(queryParameters)) {
      return null;
    }
    var parameterValue = queryParameters.get(parameterName);
    return isNotBlank(parameterValue) ? parameterValue : null;
  }
}
