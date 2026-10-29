package no.sikt.nva.approvals.index;

import java.util.UUID;
import no.sikt.nva.approvals.domain.SourceSnapshot;
import no.sikt.nva.approvals.persistence.ChangeRepository;

public class IndexingService {

  private static final String MISSING_SNAPSHOT_MESSAGE = "Approval is missing snapshot";
  private final ChangeRepository changeRepository;

  public IndexingService(ChangeRepository changeRepository) {
    this.changeRepository = changeRepository;
  }

  public void index(UUID approvalIdentifier) throws IndexingServiceException {
    var snapshot = fetchLatestSnapshot(approvalIdentifier);
    throw new UnsupportedOperationException(snapshot.toString());
  }

  private SourceSnapshot fetchLatestSnapshot(UUID approvalIdentifier)
      throws IndexingServiceException {
    return changeRepository
        .findLatestSnapshot(approvalIdentifier)
        .orElseThrow(() -> new IndexingServiceException(MISSING_SNAPSHOT_MESSAGE));
  }
}
