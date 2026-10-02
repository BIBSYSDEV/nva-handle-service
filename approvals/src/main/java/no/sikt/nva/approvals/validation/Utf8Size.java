package no.sikt.nva.approvals.validation;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static no.sikt.nva.approvals.validation.RequestConstraints.TOO_MANY_BYTES_MESSAGE;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

@Documented
@Constraint(validatedBy = Utf8SizeValidator.class)
@Target({FIELD, PARAMETER})
@Retention(RUNTIME)
public @interface Utf8Size {

  String message() default TOO_MANY_BYTES_MESSAGE;

  Class<?>[] groups() default {};

  Class<? extends Payload>[] payload() default {};

  int max();
}
