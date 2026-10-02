package no.sikt.nva.approvals.validation;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static no.sikt.nva.approvals.validation.RequestConstraints.NOT_A_HANDLE_MESSAGE;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

@Documented
@Constraint(validatedBy = HandleUriValidator.class)
@Target({FIELD, PARAMETER})
@Retention(RUNTIME)
public @interface HandleUri {

  String message() default NOT_A_HANDLE_MESSAGE;

  Class<?>[] groups() default {};

  Class<? extends Payload>[] payload() default {};
}
