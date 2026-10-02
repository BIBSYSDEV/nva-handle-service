package no.sikt.nva.approvals.validation;

import static java.util.Objects.isNull;
import static nva.commons.core.attempt.Try.attempt;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.net.URI;
import no.sikt.nva.approvals.domain.Handle;

public class HandleUriValidator implements ConstraintValidator<HandleUri, URI> {

  @Override
  public boolean isValid(URI uri, ConstraintValidatorContext context) {
    return isNull(uri) || attempt(() -> new Handle(uri)).isSuccess();
  }
}
