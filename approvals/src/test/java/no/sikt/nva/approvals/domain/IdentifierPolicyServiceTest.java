package no.sikt.nva.approvals.domain;

import static java.util.UUID.randomUUID;
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
  void shouldDisallowAllIdentifiersWhenCustomerHasNoPolicy() {
    var customerId = randomUUID();
    var namedIdentifiers = randomIdentifiers(2);
    when(approvalRepository.findIdentifierPolicy(customerId)).thenReturn(Optional.empty());

    assertEquals(
        namedIdentifiers,
        identifierPolicyService.findDisallowedIdentifiers(customerId, namedIdentifiers));
  }

  @Test
  void shouldReturnNoDisallowedIdentifiersWhenAllNamesAreAllowed() {
    var customerId = randomUUID();
    var namedIdentifier = new NamedIdentifier("DMP", randomString());
    when(approvalRepository.findIdentifierPolicy(customerId))
        .thenReturn(Optional.of(new IdentifierPolicy(customerId, Set.of("DMP"))));

    assertTrue(
        identifierPolicyService
            .findDisallowedIdentifiers(customerId, List.of(namedIdentifier))
            .isEmpty());
  }
}
