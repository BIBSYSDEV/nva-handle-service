package no.sikt.nva.approvals.domain;

import java.util.Collection;
import java.util.Set;
import java.util.UUID;
import no.sikt.nva.approvals.persistence.ApprovalRepository;
import no.sikt.nva.approvals.persistence.DynamoDbApprovalRepository;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;

public class IdentifierPolicyServiceImpl implements IdentifierPolicyService {

  private final ApprovalRepository approvalRepository;

  public IdentifierPolicyServiceImpl(ApprovalRepository approvalRepository) {
    this.approvalRepository = approvalRepository;
  }

  @JacocoGenerated
  public static IdentifierPolicyService defaultInstance(Environment environment) {
    return new IdentifierPolicyServiceImpl(DynamoDbApprovalRepository.defaultInstance(environment));
  }

  @Override
  public IdentifierPolicy getIdentifierPolicy(UUID customerId) {
    return approvalRepository
        .findIdentifierPolicy(customerId)
        .orElseGet(() -> IdentifierPolicy.denyAll(customerId));
  }

  @Override
  public Set<String> findDisallowedIdentifierNames(
      UUID customerId, Collection<NamedIdentifier> namedIdentifiers) {
    return getIdentifierPolicy(customerId).disallowedNames(namedIdentifiers);
  }
}
