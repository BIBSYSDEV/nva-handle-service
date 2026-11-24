package no.sikt.nva.approvals.domain;

import static java.util.Objects.nonNull;

import java.util.Optional;
import java.util.UUID;
import no.sikt.nva.approvals.persistence.ChangeRepository;
import no.sikt.nva.approvals.persistence.DynamoDbChangeRepository;
import no.sikt.nva.approvals.persistence.DynamoDbSourceViewRepository;
import no.sikt.nva.approvals.persistence.SourceViewRepository;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;

public class SourceViewService {

  private final SourceViewRepository sourceViewRepository;
  private final ChangeRepository changeRepository;

  public SourceViewService(
      SourceViewRepository sourceViewRepository, ChangeRepository changeRepository) {
    this.sourceViewRepository = sourceViewRepository;
    this.changeRepository = changeRepository;
  }

  @JacocoGenerated
  public static SourceViewService defaultInstance(Environment environment) {
    return new SourceViewService(
        DynamoDbSourceViewRepository.defaultInstance(environment),
        DynamoDbChangeRepository.defaultInstance(environment));
  }

  public Optional<StoredSourceView> fetchStoredView(UUID approvalIdentifier) {
    return sourceViewRepository.find(approvalIdentifier).filter(stored -> nonNull(stored.view()));
  }

  public ApprovalSourceView fetchSourceView(UUID approvalIdentifier) {
    var readableView = fetchStoredView(approvalIdentifier);
    var sourceModified =
        changeRepository.findLatestSnapshot(approvalIdentifier).map(SourceSnapshot::createdDate);
    return new ApprovalSourceView(
        readableView.map(StoredSourceView::view).orElse(null),
        readableView.map(StoredSourceView::modifiedDate).orElse(null),
        sourceModified.orElse(null));
  }
}
