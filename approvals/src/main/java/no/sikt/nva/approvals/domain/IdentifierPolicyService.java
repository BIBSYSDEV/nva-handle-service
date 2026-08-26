package no.sikt.nva.approvals.domain;

import java.util.Collection;
import java.util.Set;
import java.util.UUID;

public interface IdentifierPolicyService {

  IdentifierPolicy getIdentifierPolicy(UUID customerId);

  Set<String> findDisallowedIdentifierNames(
      UUID customerId, Collection<NamedIdentifier> namedIdentifiers);
}
