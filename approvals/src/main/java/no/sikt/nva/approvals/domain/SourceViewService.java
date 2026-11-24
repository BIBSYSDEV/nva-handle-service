package no.sikt.nva.approvals.domain;

import static java.util.Objects.nonNull;

import java.util.Optional;
import java.util.UUID;
import no.sikt.nva.approvals.persistence.DynamoDbSourceViewRepository;
import no.sikt.nva.approvals.persistence.SourceViewRepository;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;

public class SourceViewService {

  private final SourceViewRepository sourceViewRepository;

  public SourceViewService(SourceViewRepository sourceViewRepository) {
    this.sourceViewRepository = sourceViewRepository;
  }

  @JacocoGenerated
  public static SourceViewService defaultInstance(Environment environment) {
    return new SourceViewService(DynamoDbSourceViewRepository.defaultInstance(environment));
  }

  public Optional<StoredSourceView> fetchStoredView(UUID approvalIdentifier) {
    return sourceViewRepository.find(approvalIdentifier).filter(stored -> nonNull(stored.view()));
  }
}
