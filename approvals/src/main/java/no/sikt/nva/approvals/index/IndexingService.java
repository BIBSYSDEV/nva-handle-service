package no.sikt.nva.approvals.index;

import java.util.UUID;
import no.sikt.nva.approvals.domain.SourceSnapshot;
import no.sikt.nva.approvals.persistence.ChangeRepository;

public class IndexingService {

  private static final String MISSING_SNAPSHOT_MESSAGE = "Approval is missing snapshot";
  private final ChangeRepository changeRepository;
  private final IndexDocumentGenerator generator;

  public IndexingService(ChangeRepository changeRepository, IndexDocumentGenerator generator) {
    this.changeRepository = changeRepository;
    this.generator = generator;
  }

  public void index(UUID approvalIdentifier) throws IndexingServiceException {
    var snapshot = fetchLatestSnapshot(approvalIdentifier);
    var indexDocument = generateIndexDocument(snapshot);
    throw new UnsupportedOperationException(indexDocument.toString());
  }

  private IndexDocument generateIndexDocument(SourceSnapshot snapshot)
      throws IndexingServiceException {
    try {
      return generator.generate(snapshot);
    } catch (IndexDocumentGeneratorException exception) {
      throw new IndexingServiceException(exception.getMessage());
    }
  }

  private SourceSnapshot fetchLatestSnapshot(UUID approvalIdentifier)
      throws IndexingServiceException {
    return changeRepository
        .findLatestSnapshot(approvalIdentifier)
        .orElseThrow(() -> new IndexingServiceException(MISSING_SNAPSHOT_MESSAGE));
  }
}
