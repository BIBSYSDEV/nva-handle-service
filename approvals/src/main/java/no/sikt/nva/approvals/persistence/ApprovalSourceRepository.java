package no.sikt.nva.approvals.persistence;

import java.net.URI;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;

public interface ApprovalSourceRepository {

  Map<UUID, URI> findSources(Collection<UUID> approvalIdentifiers);

  boolean replaceSource(UUID approvalIdentifier, URI expectedSource, URI newSource);
}
