package no.sikt.nva.approvals.domain;

import static no.sikt.nva.approvals.validation.RequestConstraints.IDENTIFIER_NAME_PATTERN;
import static no.sikt.nva.approvals.validation.RequestConstraints.IDENTIFIER_NAME_PATTERN_MESSAGE;
import static no.sikt.nva.approvals.validation.RequestConstraints.MANDATORY_MESSAGE;
import static no.sikt.nva.approvals.validation.RequestConstraints.MAX_IDENTIFIER_NAME_LENGTH;
import static no.sikt.nva.approvals.validation.RequestConstraints.MAX_IDENTIFIER_VALUE_LENGTH;
import static no.sikt.nva.approvals.validation.RequestConstraints.TOO_LONG_MESSAGE;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import com.fasterxml.jackson.annotation.JsonTypeName;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.Locale;

@JsonTypeInfo(use = Id.NAME, property = "type")
@JsonTypeName("Identifier")
public record NamedIdentifier(
    @NotNull(message = MANDATORY_MESSAGE)
        @Size(max = MAX_IDENTIFIER_NAME_LENGTH, message = TOO_LONG_MESSAGE)
        @Pattern(regexp = IDENTIFIER_NAME_PATTERN, message = IDENTIFIER_NAME_PATTERN_MESSAGE)
        String name,
    @NotNull(message = MANDATORY_MESSAGE)
        @Size(max = MAX_IDENTIFIER_VALUE_LENGTH, message = TOO_LONG_MESSAGE)
        String value) {

  public static String normalizeName(String name) {
    return name.trim().toLowerCase(Locale.ROOT);
  }

  public String normalizedName() {
    return normalizeName(name);
  }
}
