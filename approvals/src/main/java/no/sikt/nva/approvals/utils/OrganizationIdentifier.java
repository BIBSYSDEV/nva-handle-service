package no.sikt.nva.approvals.utils;

import static no.sikt.nva.approvals.utils.AuthorityIdentifier.getIdentifier;

import java.net.URI;
import nva.commons.core.StringUtils;

public record OrganizationIdentifier(String value) implements AuthorityIdentifier {

  private static final String ORGANIZATION_PATH = "organization";
  private static final String INVALID_ORGANIZATION_IDENTIFIER_EXCEPTION =
      "Provided uri is not a organization identifier: ";
  private static final String MISSING_ORGANIZATION_IDENTIFIER_EXCEPTION =
      "Organization identifier cannot be blank!";

  public OrganizationIdentifier {
    if (StringUtils.isBlank(value)) {
      throw new IllegalArgumentException(MISSING_ORGANIZATION_IDENTIFIER_EXCEPTION);
    }
  }

  public static OrganizationIdentifier fromUri(URI uri) {
    return getIdentifier(uri, ORGANIZATION_PATH)
        .map(OrganizationIdentifier::new)
        .orElseThrow(
            () -> new IllegalArgumentException(INVALID_ORGANIZATION_IDENTIFIER_EXCEPTION + uri));
  }
}
