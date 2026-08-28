package no.sikt.nva.approvals.domain;

import static java.util.UUID.randomUUID;
import static java.util.stream.Collectors.toUnmodifiableSet;
import static no.sikt.nva.approvals.utils.TestUtils.randomIdentifiers;
import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import no.sikt.nva.approvals.persistence.ApprovalRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class IdentifierPolicyServiceTest {

  private static final String DMP = "DMP";
  private ApprovalRepository approvalRepository;
  private IdentifierPolicyService identifierPolicyService;

  @BeforeEach
  void setup() {
    this.approvalRepository = mock(ApprovalRepository.class);
    this.identifierPolicyService = new IdentifierPolicyServiceImpl(approvalRepository);
  }

  @Test
  void shouldReturnDenyAllPolicyWhenCustomerHasNoPolicy() {
    var customerIdentifier = randomUUID();
    when(approvalRepository.findIdentifierPolicy(customerIdentifier)).thenReturn(Optional.empty());

    assertEquals(
        IdentifierPolicy.DENY_ALL, identifierPolicyService.getIdentifierPolicy(customerIdentifier));
  }

  @Test
  void shouldDisallowAllIdentifierNamesWhenCustomerHasNoPolicy() {
    var customerIdentifier = randomUUID();
    var namedIdentifiers = randomIdentifiers(2);
    var expectedNames =
        namedIdentifiers.stream().map(NamedIdentifier::name).collect(toUnmodifiableSet());
    when(approvalRepository.findIdentifierPolicy(customerIdentifier)).thenReturn(Optional.empty());

    assertEquals(
        expectedNames,
        identifierPolicyService.findDisallowedIdentifierNames(
            customerIdentifier, namedIdentifiers));
  }

  @Test
  void shouldReturnNoDisallowedIdentifierNamesWhenAllNamesAreAllowed() {
    var customerIdentifier = randomUUID();
    var namedIdentifier = new NamedIdentifier(DMP, randomString());
    when(approvalRepository.findIdentifierPolicy(customerIdentifier))
        .thenReturn(Optional.of(new IdentifierPolicy(Set.of(DMP))));

    assertTrue(
        identifierPolicyService
            .findDisallowedIdentifierNames(customerIdentifier, List.of(namedIdentifier))
            .isEmpty());
  }
}
