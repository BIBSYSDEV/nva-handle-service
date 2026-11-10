package no.sikt.nva.approvals.index;

import static java.util.function.Predicate.not;
import static java.util.stream.Collectors.toSet;
import static nva.commons.core.attempt.Try.attempt;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import no.sikt.nva.approvals.domain.Approval;
import no.sikt.nva.approvals.domain.SourceSnapshot;
import no.sikt.nva.approvals.persistence.ApprovalRepository;
import no.sikt.nva.approvals.persistence.ChangeRepository;
import no.sikt.nva.approvals.persistence.DynamoDbApprovalRepository;
import no.sikt.nva.approvals.persistence.DynamoDbChangeRepository;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IndexingService {

  private static final String MISSING_SNAPSHOT_MESSAGE = "Approval is missing snapshot ";
  private static final String APPROVAL_NOT_FOUND_MESSAGE = "Approval not found ";
  private static final String INDEXING_FAILED_MESSAGE =
      "Could not create an index document of approval {}";
  private static final Logger LOGGER = LoggerFactory.getLogger(IndexingService.class);

  private final ChangeRepository changeRepository;
  private final ApprovalRepository approvalRepository;
  private final IndexDocumentGenerator generator;
  private final SearchClient searchClient;

  public IndexingService(
      ChangeRepository changeRepository,
      ApprovalRepository approvalRepository,
      IndexDocumentGenerator generator,
      SearchClient searchClient) {
    this.changeRepository = changeRepository;
    this.approvalRepository = approvalRepository;
    this.generator = generator;
    this.searchClient = searchClient;
  }

  @JacocoGenerated
  public static IndexingService defaultInstance(Environment environment) {
    return new IndexingService(
        DynamoDbChangeRepository.defaultInstance(environment),
        DynamoDbApprovalRepository.defaultInstance(environment),
        new IndexDocumentGenerator(),
        SearchClient.defaultInstance(environment));
  }

  public IndexingResult index(Collection<UUID> approvalIdentifiers) {
    var indexed = indexDocuments(createIndexDocuments(approvalIdentifiers));
    return new IndexingResult(indexed, failed(approvalIdentifiers, indexed));
  }

  private List<IndexDocument> createIndexDocuments(Collection<UUID> approvalIdentifiers) {
    return approvalIdentifiers.stream()
        .map(this::generateIndexDocument)
        .flatMap(Optional::stream)
        .toList();
  }

  private static Set<UUID> failed(Collection<UUID> approvalIdentifiers, Set<UUID> indexed) {
    return approvalIdentifiers.stream().filter(not(indexed::contains)).collect(toSet());
  }

  private Set<UUID> indexDocuments(Collection<IndexDocument> documents) {
    return documents.isEmpty() ? Set.of() : searchClient.index(documents);
  }

  private Optional<IndexDocument> generateIndexDocument(UUID approvalIdentifier) {
    return attempt(() -> createIndexDocument(approvalIdentifier))
        .orElse(failure -> skipApproval(approvalIdentifier, failure.getException()));
  }

  private Optional<IndexDocument> createIndexDocument(UUID approvalIdentifier)
      throws IndexingServiceException, IndexDocumentGeneratorException {
    return Optional.of(
        generator.generate(
            fetchLatestSnapshot(approvalIdentifier), fetchApproval(approvalIdentifier)));
  }

  private static Optional<IndexDocument> skipApproval(
      UUID approvalIdentifier, Exception exception) {
    LOGGER.error(INDEXING_FAILED_MESSAGE, approvalIdentifier, exception);
    return Optional.empty();
  }

  private SourceSnapshot fetchLatestSnapshot(UUID approvalIdentifier)
      throws IndexingServiceException {
    return changeRepository
        .findLatestSnapshot(approvalIdentifier)
        .orElseThrow(
            () -> new IndexingServiceException(MISSING_SNAPSHOT_MESSAGE + approvalIdentifier));
  }

  private Approval fetchApproval(UUID approvalIdentifier) throws IndexingServiceException {
    return approvalRepository
        .findByApprovalIdentifier(approvalIdentifier)
        .orElseThrow(
            () -> new IndexingServiceException(APPROVAL_NOT_FOUND_MESSAGE + approvalIdentifier));
  }
}
