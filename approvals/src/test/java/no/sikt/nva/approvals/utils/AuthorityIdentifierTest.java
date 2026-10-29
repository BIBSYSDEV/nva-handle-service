package no.sikt.nva.approvals.utils;

import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.net.URI;
import nva.commons.core.paths.UriWrapper;
import org.junit.jupiter.api.Test;

class AuthorityIdentifierTest {

  private static final String HOST = randomUri().getHost();
  private static final String CRISTIN = "cristin";
  private static final String PERSON = "person";
  private static final String ORGANIZATION = "organization";

  @Test
  void shouldCreatePersonIdentifierFromCristinPersonUri() {
    var identifier = randomString();

    var personIdentifier = PersonIdentifier.fromUri(cristinUri(PERSON, identifier));

    assertEquals(identifier, personIdentifier.value());
  }

  @Test
  void shouldCreateOrganizationIdentifierFromCristinOrganizationUri() {
    var identifier = randomString();

    var organizationIdentifier =
        OrganizationIdentifier.fromUri(cristinUri(ORGANIZATION, identifier));

    assertEquals(identifier, organizationIdentifier.value());
  }

  @Test
  void shouldThrowExceptionWhenPersonUriHasUnexpectedResource() {
    var uri = cristinUri(randomString(), randomString());

    assertThrows(IllegalArgumentException.class, () -> PersonIdentifier.fromUri(uri));
  }

  @Test
  void shouldThrowExceptionWhenPersonUriIsNotCristinUri() {
    var uri = UriWrapper.fromHost(HOST).addChild(PERSON).addChild(randomString()).getUri();

    assertThrows(IllegalArgumentException.class, () -> PersonIdentifier.fromUri(uri));
  }

  @Test
  void shouldThrowExceptionWhenPersonUriHasNoIdentifier() {
    var uri = UriWrapper.fromHost(HOST).addChild(CRISTIN).addChild(PERSON).getUri();

    assertThrows(IllegalArgumentException.class, () -> PersonIdentifier.fromUri(uri));
  }

  @Test
  void shouldThrowExceptionWhenOrganizationUriHasUnexpectedResource() {
    var uri = cristinUri(PERSON, randomString());

    assertThrows(IllegalArgumentException.class, () -> OrganizationIdentifier.fromUri(uri));
  }

  @Test
  void shouldThrowExceptionWhenOrganizationUriIsNotCristinUri() {
    var uri = UriWrapper.fromHost(HOST).addChild(ORGANIZATION).addChild(randomString()).getUri();

    assertThrows(IllegalArgumentException.class, () -> OrganizationIdentifier.fromUri(uri));
  }

  @Test
  void shouldThrowExceptionWhenOrganizationUriHasNoIdentifier() {
    var uri = UriWrapper.fromHost(HOST).addChild(CRISTIN).addChild(ORGANIZATION).getUri();

    assertThrows(IllegalArgumentException.class, () -> OrganizationIdentifier.fromUri(uri));
  }

  @Test
  void shouldThrowExceptionWhenUriIsNull() {
    assertThrows(IllegalArgumentException.class, () -> PersonIdentifier.fromUri(null));
  }

  @Test
  void shouldThrowExceptionWhenPersonIdentifierValueIsMissing() {
    assertThrows(IllegalArgumentException.class, () -> new PersonIdentifier(null));
    assertThrows(IllegalArgumentException.class, () -> new PersonIdentifier(" "));
  }

  @Test
  void shouldThrowExceptionWhenOrganizationIdentifierValueIsMissing() {
    assertThrows(IllegalArgumentException.class, () -> new OrganizationIdentifier(null));
    assertThrows(IllegalArgumentException.class, () -> new OrganizationIdentifier(" "));
  }

  private URI cristinUri(String resource, String identifier) {
    return UriWrapper.fromHost(HOST)
        .addChild(CRISTIN)
        .addChild(resource)
        .addChild(identifier)
        .getUri();
  }
}
