package no.sikt.nva.approvals.domain;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.Locale;
import java.util.Objects;

@JsonTypeInfo(use = Id.NAME, property = "type")
@JsonTypeName("Identifier")
public record NamedIdentifier(String name, String value) {

  public NamedIdentifier {
    Objects.requireNonNull(name, "name must not be null");
    Objects.requireNonNull(value, "value must not be null");
  }

  public static String normalizeName(String name) {
    return name.trim().toLowerCase(Locale.ROOT);
  }

  public String normalizedName() {
    return normalizeName(name);
  }
}
