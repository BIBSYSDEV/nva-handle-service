package no.sikt.nva.approvals.domain;

import static java.util.UUID.randomUUID;
import static no.sikt.nva.approvals.utils.TestUtils.randomIdentifiers;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class IdentifierPolicyTest {

  private static final String DMP = "DMP";
  private static final String CTIS = "CTIS";

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
  void shouldReturnOnlyDisallowedIdentifiers() {
    var identifierPolicy = new IdentifierPolicy(randomUUID(), Set.of(DMP));
    var allowed = new NamedIdentifier(DMP, randomString());
    var disallowed = new NamedIdentifier(CTIS, randomString());

    assertEquals(List.of(disallowed), identifierPolicy.rejects(List.of(allowed, disallowed)));
  }

  @Test
  void shouldRejectEverythingWhenPolicyDeniesAll() {
    var namedIdentifiers = randomIdentifiers(3);
    var identifierPolicy = IdentifierPolicy.denyAll(randomUUID());

    assertEquals(namedIdentifiers, identifierPolicy.rejects(namedIdentifiers));
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
}
