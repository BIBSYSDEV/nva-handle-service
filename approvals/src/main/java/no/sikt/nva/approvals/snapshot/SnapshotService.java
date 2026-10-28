package no.sikt.nva.approvals.snapshot;

import no.sikt.nva.approvals.source.SourceClientException;

@FunctionalInterface
public interface SnapshotService {

  void createSnapshot(CreateSnapshotRequest createSnapshotRequest) throws SourceClientException;
}
