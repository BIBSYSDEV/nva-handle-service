package no.sikt.nva.approvals.rest;

import static java.util.Objects.nonNull;
import static no.sikt.nva.approvals.validation.RequestConstraints.CONFLICTING_QUERY_PARAMETERS_MESSAGE;
import static no.sikt.nva.approvals.validation.RequestConstraints.MANDATORY_MESSAGE;
import static no.sikt.nva.approvals.validation.RequestConstraints.MISSING_QUERY_PARAMETERS_MESSAGE;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class QueryCombinationValidator
    implements ConstraintValidator<QueryCombination, ApprovalQuery> {

  private static final String HANDLE_PARAMETER = "handle";
  private static final String NAME_PARAMETER = "name";
  private static final String VALUE_PARAMETER = "value";

  @Override
  public boolean isValid(ApprovalQuery query, ConstraintValidatorContext context) {
    context.disableDefaultConstraintViolation();
    if (hasHandle(query) && hasNamedIdentifierPart(query)) {
      return reportOnQuery(CONFLICTING_QUERY_PARAMETERS_MESSAGE, context);
    }
    if (!hasHandle(query) && !hasNamedIdentifierPart(query)) {
      return reportOnQuery(MISSING_QUERY_PARAMETERS_MESSAGE, context);
    }
    if (hasHandle(query)) {
      return true;
    }
    var hasName = isPresentOrReported(query.name(), NAME_PARAMETER, context);
    var hasValue = isPresentOrReported(query.value(), VALUE_PARAMETER, context);
    return hasName && hasValue;
  }

  private static boolean hasHandle(ApprovalQuery query) {
    return nonNull(query.handle());
  }

  private static boolean hasNamedIdentifierPart(ApprovalQuery query) {
    return nonNull(query.name()) || nonNull(query.value());
  }

  private static boolean reportOnQuery(String message, ConstraintValidatorContext context) {
    context
        .buildConstraintViolationWithTemplate(message)
        .addPropertyNode(HANDLE_PARAMETER)
        .addConstraintViolation();
    return false;
  }

  private static boolean isPresentOrReported(
      String parameterValue, String parameterName, ConstraintValidatorContext context) {
    if (nonNull(parameterValue)) {
      return true;
    }
    context
        .buildConstraintViolationWithTemplate(MANDATORY_MESSAGE)
        .addPropertyNode(parameterName)
        .addConstraintViolation();
    return false;
  }
}
