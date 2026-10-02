package no.sikt.nva.approvals.validation;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Objects.isNull;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class Utf8SizeValidator implements ConstraintValidator<Utf8Size, String> {

  private int maxBytes;

  @Override
  public void initialize(Utf8Size constraint) {
    this.maxBytes = constraint.max();
  }

  @Override
  public boolean isValid(String text, ConstraintValidatorContext context) {
    return isNull(text) || text.getBytes(UTF_8).length <= maxBytes;
  }
}
