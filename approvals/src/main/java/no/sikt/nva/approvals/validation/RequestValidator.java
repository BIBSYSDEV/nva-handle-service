package no.sikt.nva.approvals.validation;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ElementKind;
import jakarta.validation.Path;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import nva.commons.apigateway.exceptions.BadRequestException;
import nva.commons.apigateway.exceptions.ValidationError;

public final class RequestValidator {

  private static final Validator VALIDATOR =
      Validation.buildDefaultValidatorFactory().getValidator();
  private static final String POINTER_SEPARATOR = "/";
  private static final String ROOT_POINTER = "";
  private static final String VIOLATION_FORMAT = "%s: %s";
  private static final String VIOLATION_DELIMITER = "; ";
  private static final String BODY_MISSING_MESSAGE = "Request body is missing";

  private RequestValidator() {}

  public static void validateBody(Object body) throws BadRequestException {
    if (isNull(body)) {
      throw new BadRequestException(BODY_MISSING_MESSAGE);
    }
    validate(body, RequestValidator::toJsonPointer);
  }

  public static void validateQueryParameters(Object queryParameters) throws BadRequestException {
    validate(queryParameters, RequestValidator::toParameterName);
  }

  public static BadRequestException badRequest(String message, String pointer) {
    return new BadRequestException(message, new ValidationError(message, pointer));
  }

  private static void validate(Object request, Function<Path, String> pointerResolver)
      throws BadRequestException {
    var validationErrors =
        VALIDATOR.validate(request).stream()
            .map(violation -> toValidationError(violation, pointerResolver))
            .sorted(
                Comparator.comparing(ValidationError::pointer)
                    .thenComparing(ValidationError::detail))
            .toList();
    if (!validationErrors.isEmpty()) {
      throw new BadRequestException(describe(validationErrors), validationErrors);
    }
  }

  private static ValidationError toValidationError(
      ConstraintViolation<?> violation, Function<Path, String> pointerResolver) {
    return new ValidationError(
        violation.getMessage(), pointerResolver.apply(violation.getPropertyPath()));
  }

  private static String toJsonPointer(Path path) {
    return nodes(path).stream()
        .map(RequestValidator::pointerSegments)
        .collect(Collectors.joining(ROOT_POINTER));
  }

  private static String pointerSegments(Path.Node node) {
    var segments = new StringBuilder();
    if (nonNull(node.getIndex())) {
      segments.append(POINTER_SEPARATOR).append(node.getIndex());
    }
    if (hasName(node)) {
      segments.append(POINTER_SEPARATOR).append(node.getName());
    }
    return segments.toString();
  }

  private static String toParameterName(Path path) {
    return nodes(path).stream()
        .filter(RequestValidator::hasName)
        .map(Path.Node::getName)
        .collect(Collectors.joining(POINTER_SEPARATOR));
  }

  private static boolean hasName(Path.Node node) {
    return nonNull(node.getName())
        && node.getKind() != ElementKind.CONTAINER_ELEMENT
        && node.getKind() != ElementKind.BEAN;
  }

  private static List<Path.Node> nodes(Path path) {
    return StreamSupport.stream(path.spliterator(), false).toList();
  }

  private static String describe(Collection<ValidationError> validationErrors) {
    return validationErrors.stream()
        .map(error -> VIOLATION_FORMAT.formatted(error.pointer(), error.detail()))
        .collect(Collectors.joining(VIOLATION_DELIMITER));
  }
}
