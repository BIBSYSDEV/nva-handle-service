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
    var customerId = randomUUID();
    when(approvalRepository.findIdentifierPolicy(customerId)).thenReturn(Optional.empty());

    assertEquals(
        IdentifierPolicy.denyAll(customerId),
        identifierPolicyService.getIdentifierPolicy(customerId));
  }

  @Test
  void shouldDisallowAllIdentifierNamesWhenCustomerHasNoPolicy() {
    var customerId = randomUUID();
    var namedIdentifiers = randomIdentifiers(2);
    var expectedNames =
        namedIdentifiers.stream().map(NamedIdentifier::name).collect(toUnmodifiableSet());
    when(approvalRepository.findIdentifierPolicy(customerId)).thenReturn(Optional.empty());

    assertEquals(
        expectedNames,
        identifierPolicyService.findDisallowedIdentifierNames(customerId, namedIdentifiers));
  }

  @Test
  void shouldReturnNoDisallowedIdentifierNamesWhenAllNamesAreAllowed() {
    var customerId = randomUUID();
    var namedIdentifier = new NamedIdentifier(DMP, randomString());
    when(approvalRepository.findIdentifierPolicy(customerId))
        .thenReturn(Optional.of(new IdentifierPolicy(customerId, Set.of(DMP))));

    assertTrue(
        identifierPolicyService
            .findDisallowedIdentifierNames(customerId, List.of(namedIdentifier))
            .isEmpty());
  }
}
