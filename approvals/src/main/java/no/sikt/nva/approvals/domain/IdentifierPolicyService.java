package no.sikt.nva.approvals.domain;

import java.util.UUID;

@SuppressWarnings("PMD.ImplicitFunctionalInterface")
public interface IdentifierPolicyService {

  IdentifierPolicy getIdentifierPolicy(UUID customerIdentifier);
}
