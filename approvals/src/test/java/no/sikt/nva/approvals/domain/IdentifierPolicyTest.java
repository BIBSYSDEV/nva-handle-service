package no.sikt.nva.approvals.domain;

import static java.util.UUID.randomUUID;
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
  void shouldPermitIdentifierWithAllowedName() {
    var identifierPolicy = new IdentifierPolicy(randomUUID(), Set.of(DMP));

    assertTrue(identifierPolicy.permits(new NamedIdentifier(DMP, randomString())));
  }

  @Test
  void shouldPermitIdentifierRegardlessOfCase() {
    var identifierPolicy = new IdentifierPolicy(randomUUID(), Set.of(DMP));

    assertTrue(identifierPolicy.permits(new NamedIdentifier("dmp", randomString())));
  }

  @Test
  void shouldPermitIdentifierWhenAllowedNameIsPaddedWithWhitespace() {
    var identifierPolicy = new IdentifierPolicy(randomUUID(), Set.of(PADDED_DMP));

    assertTrue(identifierPolicy.permits(new NamedIdentifier(DMP, randomString())));
  }

  @Test
  void shouldPermitIdentifierWhenRequestedNameIsPaddedWithWhitespace() {
    var identifierPolicy = new IdentifierPolicy(randomUUID(), Set.of(DMP));

    assertTrue(identifierPolicy.permits(new NamedIdentifier(PADDED_DMP, randomString())));
  }

  @Test
  void shouldReturnOnlyDisallowedNames() {
    var identifierPolicy = new IdentifierPolicy(randomUUID(), Set.of(DMP));
    var allowed = new NamedIdentifier(DMP, randomString());
    var disallowed = new NamedIdentifier(CTIS, randomString());

    assertEquals(Set.of(CTIS), identifierPolicy.disallowedNames(List.of(allowed, disallowed)));
  }

  @Test
  void shouldReportDisallowedNameOnceWhenSharedBySeveralIdentifiers() {
    var identifierPolicy = new IdentifierPolicy(randomUUID(), Set.of(DMP));
    var namedIdentifiers =
        List.of(
            new NamedIdentifier(CTIS, randomString()), new NamedIdentifier(CTIS, randomString()));

    assertEquals(Set.of(CTIS), identifierPolicy.disallowedNames(namedIdentifiers));
  }

  @Test
  void shouldReportDisallowedNameOnceWhenSharedByIdentifiersDifferingOnlyByCase() {
    var identifierPolicy = new IdentifierPolicy(randomUUID(), Set.of(DMP));
    var namedIdentifiers =
        List.of(
            new NamedIdentifier(CTIS, randomString()),
            new NamedIdentifier(CTIS.toLowerCase(Locale.ROOT), randomString()));

    assertEquals(1, identifierPolicy.disallowedNames(namedIdentifiers).size());
  }

  @Test
  void shouldTreatAllowedIdentifierNamesDifferingOnlyByCaseAsOne() {
    var allowedIdentifierNames = Set.of(DMP, DMP.toLowerCase(Locale.ROOT));
    var identifierPolicy = new IdentifierPolicy(randomUUID(), allowedIdentifierNames);

    assertEquals(1, identifierPolicy.allowedIdentifierNames().size());
  }

  @Test
  void shouldRejectEverythingWhenPolicyDeniesAll() {
    var namedIdentifiers = randomIdentifiers(3);
    var identifierPolicy = IdentifierPolicy.denyAll(randomUUID());
    var expectedNames =
        namedIdentifiers.stream().map(NamedIdentifier::name).collect(toUnmodifiableSet());

    assertEquals(expectedNames, identifierPolicy.disallowedNames(namedIdentifiers));
  }

  @Test
  void shouldThrowExceptionWhenCustomerIdIsNull() {
    var allowedIdentifierNames = Set.of(DMP);

    assertThrows(
        NullPointerException.class, () -> new IdentifierPolicy(null, allowedIdentifierNames));
  }

  @Test
  void shouldThrowExceptionWhenAllowedIdentifierNamesIsNull() {
    var customerId = randomUUID();

    assertThrows(NullPointerException.class, () -> new IdentifierPolicy(customerId, null));
  }

  @Test
  void shouldThrowExceptionWhenAllowedIdentifierNameIsBlank() {
    var customerId = randomUUID();
    var allowedIdentifierNames = Set.of(DMP, BLANK_NAME);

    assertThrows(
        IllegalArgumentException.class,
        () -> new IdentifierPolicy(customerId, allowedIdentifierNames));
  }

  @Test
  void shouldThrowExceptionWhenAllowedIdentifierNameIsNull() {
    var customerId = randomUUID();
    var allowedIdentifierNames = new HashSet<String>();
    allowedIdentifierNames.add(DMP);
    allowedIdentifierNames.add(null);

    assertThrows(
        IllegalArgumentException.class,
        () -> new IdentifierPolicy(customerId, allowedIdentifierNames));
  }
}
