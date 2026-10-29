package no.sikt.nva.approvals.utils;

import static no.sikt.nva.approvals.utils.AuthorityIdentifier.cristinIdentifier;

import java.net.URI;
import nva.commons.core.StringUtils;

public record PersonIdentifier(String value) implements AuthorityIdentifier {

  private static final String PERSON_PATH = "person";
  private static final String INVALID_PERSON_IDENTIFIER_EXCEPTION =
      "Provided uri is not a Cristin person identifier: ";
  private static final String MISSING_PERSON_IDENTIFIER_EXCEPTION =
      "Person identifier cannot be blank!";

  public PersonIdentifier {
    if (StringUtils.isBlank(value)) {
      throw new IllegalArgumentException(MISSING_PERSON_IDENTIFIER_EXCEPTION);
    }
  }

  public static PersonIdentifier fromUri(URI uri) {
    return cristinIdentifier(uri, PERSON_PATH)
        .map(PersonIdentifier::new)
        .orElseThrow(() -> new IllegalArgumentException(INVALID_PERSON_IDENTIFIER_EXCEPTION + uri));
  }
}
