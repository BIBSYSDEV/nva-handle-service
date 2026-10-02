package no.sikt.nva.approvals.validation;

import static java.util.Objects.isNull;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.net.URI;

public class UriSizeValidator implements ConstraintValidator<UriSize, URI> {

  private int maxLength;

  @Override
  public void initialize(UriSize constraint) {
    this.maxLength = constraint.max();
  }

  @Override
  public boolean isValid(URI uri, ConstraintValidatorContext context) {
    return isNull(uri) || uri.toString().length() <= maxLength;
  }
}
