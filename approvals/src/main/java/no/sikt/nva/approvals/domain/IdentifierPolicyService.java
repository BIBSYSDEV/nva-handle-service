package no.sikt.nva.approvals.domain;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface IdentifierPolicyService {

  IdentifierPolicy getIdentifierPolicy(UUID customerId);

  List<NamedIdentifier> findDisallowedIdentifiers(
      UUID customerId, Collection<NamedIdentifier> namedIdentifiers);
}
