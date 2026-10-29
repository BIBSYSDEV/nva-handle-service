package no.sikt.nva.approvals.index;

import java.util.UUID;
import no.sikt.nva.approvals.domain.Approval;
import no.sikt.nva.approvals.domain.SourceSnapshot;
import no.sikt.nva.approvals.persistence.ApprovalRepository;
import no.sikt.nva.approvals.persistence.ChangeRepository;

public class IndexingService {

  private static final String MISSING_SNAPSHOT_MESSAGE = "Approval is missing snapshot";
  private static final String APPROVAL_NOT_FOUND_MESSAGE = "Approval not found";
  private final ChangeRepository changeRepository;
  private final ApprovalRepository approvalRepository;
  private final IndexDocumentGenerator generator;

  public IndexingService(
      ChangeRepository changeRepository,
      ApprovalRepository approvalRepository,
      IndexDocumentGenerator generator) {
    this.changeRepository = changeRepository;
    this.approvalRepository = approvalRepository;
    this.generator = generator;
  }

  public void index(UUID approvalIdentifier) throws IndexingServiceException {
    var snapshot = fetchLatestSnapshot(approvalIdentifier);
    var approval = fetchApproval(approvalIdentifier);
    var indexDocument = generateIndexDocument(snapshot, approval);
    throw new UnsupportedOperationException(indexDocument.toString());
  }

  private IndexDocument generateIndexDocument(SourceSnapshot snapshot, Approval approval)
      throws IndexingServiceException {
    try {
      return generator.generate(snapshot, approval);
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

  private Approval fetchApproval(UUID approvalIdentifier) throws IndexingServiceException {
    return approvalRepository
        .findByApprovalIdentifier(approvalIdentifier)
        .orElseThrow(() -> new IndexingServiceException(APPROVAL_NOT_FOUND_MESSAGE));
  }
}
