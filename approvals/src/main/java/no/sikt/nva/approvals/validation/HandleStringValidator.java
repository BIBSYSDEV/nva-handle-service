package no.sikt.nva.approvals.validation;

import static java.util.Objects.isNull;
import static nva.commons.core.attempt.Try.attempt;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.net.URI;
import no.sikt.nva.approvals.domain.Handle;

public class HandleStringValidator implements ConstraintValidator<HandleUri, String> {

  @Override
  public boolean isValid(String value, ConstraintValidatorContext context) {
    return isNull(value) || attempt(() -> new Handle(URI.create(value))).isSuccess();
  }
}
