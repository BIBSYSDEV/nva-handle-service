package no.sikt.nva.approvals.domain;

import static java.util.stream.Collectors.toUnmodifiableSet;
import static no.sikt.nva.approvals.utils.TestUtils.randomIdentifiers;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.junit.jupiter.api.Test;

class IdentifierPolicyTest {

  private static final String DMP = "DMP";
  private static final String CTIS = "CTIS";
  private static final String BLANK_NAME = "  ";
  private static final String PADDED_DMP = "  DMP  ";

  @Test
  void shouldAllowIdentifierWithAllowedName() {
    var identifierPolicy = new IdentifierPolicy(Set.of(DMP));

    assertTrue(identifierPolicy.allows(new NamedIdentifier(DMP, randomString())));
  }

  @Test
  void shouldAllowIdentifierRegardlessOfCase() {
    var identifierPolicy = new IdentifierPolicy(Set.of(DMP));

    assertTrue(identifierPolicy.allows(new NamedIdentifier("dmp", randomString())));
  }

  @Test
  void shouldAllowIdentifierWhenAllowedNameIsPaddedWithWhitespace() {
    var identifierPolicy = new IdentifierPolicy(Set.of(PADDED_DMP));

    assertTrue(identifierPolicy.allows(new NamedIdentifier(DMP, randomString())));
  }

  @Test
  void shouldAllowIdentifierWhenRequestedNameIsPaddedWithWhitespace() {
    var identifierPolicy = new IdentifierPolicy(Set.of(DMP));

    assertTrue(identifierPolicy.allows(new NamedIdentifier(PADDED_DMP, randomString())));
  }

  @Test
  void shouldReturnOnlyDisallowedNames() {
    var identifierPolicy = new IdentifierPolicy(Set.of(DMP));
    var allowed = new NamedIdentifier(DMP, randomString());
    var disallowed = new NamedIdentifier(CTIS, randomString());

    assertEquals(Set.of(CTIS), identifierPolicy.disallowedNames(List.of(allowed, disallowed)));
  }

  @Test
  void shouldReportDisallowedNameOnceWhenSharedBySeveralIdentifiers() {
    var identifierPolicy = new IdentifierPolicy(Set.of(DMP));
    var namedIdentifiers =
        List.of(
            new NamedIdentifier(CTIS, randomString()), new NamedIdentifier(CTIS, randomString()));

    assertEquals(Set.of(CTIS), identifierPolicy.disallowedNames(namedIdentifiers));
  }

  @Test
  void shouldReportDisallowedNameOnceWhenSharedByIdentifiersDifferingOnlyByCase() {
    var identifierPolicy = new IdentifierPolicy(Set.of(DMP));
    var namedIdentifiers =
        List.of(
            new NamedIdentifier(CTIS, randomString()),
            new NamedIdentifier(CTIS.toLowerCase(Locale.ROOT), randomString()));

    assertEquals(Set.of(CTIS), identifierPolicy.disallowedNames(namedIdentifiers));
  }

  @Test
  void shouldReportSameDisallowedNameRegardlessOfIdentifierOrder() {
    var identifierPolicy = new IdentifierPolicy(Set.of(DMP));
    var lowerCaseFirst =
        List.of(
            new NamedIdentifier(CTIS.toLowerCase(Locale.ROOT), randomString()),
            new NamedIdentifier(CTIS, randomString()));
    var upperCaseFirst =
        List.of(
            new NamedIdentifier(CTIS, randomString()),
            new NamedIdentifier(CTIS.toLowerCase(Locale.ROOT), randomString()));

    assertEquals(
        identifierPolicy.disallowedNames(upperCaseFirst),
        identifierPolicy.disallowedNames(lowerCaseFirst));
  }

  @Test
  void shouldTreatAllowedIdentifierNamesDifferingOnlyByCaseAsOne() {
    var allowedIdentifierNames = Set.of(DMP, DMP.toLowerCase(Locale.ROOT));
    var identifierPolicy = new IdentifierPolicy(allowedIdentifierNames);

    assertEquals(1, identifierPolicy.allowedIdentifierNames().size());
  }

  @Test
  void shouldRejectEverythingWhenPolicyDeniesAll() {
    var namedIdentifiers = randomIdentifiers(3);
    var identifierPolicy = IdentifierPolicy.DENY_ALL;
    var expectedNames =
        namedIdentifiers.stream().map(NamedIdentifier::name).collect(toUnmodifiableSet());

    assertEquals(expectedNames, identifierPolicy.disallowedNames(namedIdentifiers));
  }

  @Test
  void shouldThrowExceptionWhenAllowedIdentifierNamesIsNull() {
    assertThrows(NullPointerException.class, () -> new IdentifierPolicy(null));
  }

  @Test
  void shouldThrowExceptionWhenAllowedIdentifierNameIsBlank() {
    var allowedIdentifierNames = Set.of(DMP, BLANK_NAME);

    assertThrows(
        IllegalArgumentException.class, () -> new IdentifierPolicy(allowedIdentifierNames));
  }

  @Test
  void shouldThrowExceptionWhenAllowedIdentifierNameIsNull() {
    var allowedIdentifierNames = new HashSet<String>();
    allowedIdentifierNames.add(DMP);
    allowedIdentifierNames.add(null);

    assertThrows(
        IllegalArgumentException.class, () -> new IdentifierPolicy(allowedIdentifierNames));
  }
}
