package no.sikt.nva.approvals.rest;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import static no.sikt.nva.approvals.validation.RequestConstraints.MISSING_QUERY_PARAMETERS_MESSAGE;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

@Documented
@Constraint(validatedBy = QueryCombinationValidator.class)
@Target(TYPE)
@Retention(RUNTIME)
public @interface QueryCombination {

  String message() default MISSING_QUERY_PARAMETERS_MESSAGE;

  Class<?>[] groups() default {};

  Class<? extends Payload>[] payload() default {};
}
