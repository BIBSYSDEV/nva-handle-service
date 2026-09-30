package no.sikt.nva.approvals.snapshot;

import java.time.Instant;
import no.sikt.nva.approvals.persistence.ApprovalRepository;
import no.sikt.nva.approvals.persistence.DynamoDbApprovalRepository;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;

public class SnapshotServiceImpl implements SnapshotService {

  private final ApprovalRepository approvalRepository;

  public SnapshotServiceImpl(ApprovalRepository approvalRepository) {
    this.approvalRepository = approvalRepository;
  }

  @JacocoGenerated
  public static SnapshotService defaultInstance(Environment environment) {
    return new SnapshotServiceImpl(DynamoDbApprovalRepository.defaultInstance(environment));
  }

  // TODO: retrieve the source via SourceClient (NP-51912) and store its content on the snapshot
  @Override
  public void createSnapshot(SourceChange sourceChange) {
    approvalRepository.save(SourceSnapshot.create(sourceChange, Instant.now()));
  }
}
