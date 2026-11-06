package no.sikt.nva.approvals.persistence;

import java.util.Optional;
import java.util.UUID;
import no.sikt.nva.approvals.domain.SourceSnapshot;

public interface LatestSnapshotRepository {

  Optional<SnapshotLock> findSnapshotLock(UUID approvalIdentifier);

  void saveAsLatestSnapshot(SourceSnapshot snapshot, SnapshotLock lock);
}
