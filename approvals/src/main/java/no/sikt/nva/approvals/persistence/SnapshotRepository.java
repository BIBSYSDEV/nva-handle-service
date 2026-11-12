package no.sikt.nva.approvals.persistence;

import java.util.Optional;
import java.util.UUID;
import no.sikt.nva.approvals.domain.SourceSnapshot;

public interface SnapshotRepository {

  Optional<SnapshotState> findSnapshotStateConsistently(UUID approvalIdentifier);

  void save(SourceSnapshot snapshot, SnapshotState expectedState);
}
