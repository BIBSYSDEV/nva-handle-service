package no.sikt.nva.approvals.snapshot;

@FunctionalInterface
public interface SnapshotService {

  void createSnapshot(SourceChange sourceChange);
}
