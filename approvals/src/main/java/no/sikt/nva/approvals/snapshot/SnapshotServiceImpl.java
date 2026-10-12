package no.sikt.nva.approvals.snapshot;

import no.sikt.nva.approvals.domain.Content;
import no.sikt.nva.approvals.domain.SourceSnapshot;
import no.sikt.nva.approvals.persistence.ApprovalRepository;
import no.sikt.nva.approvals.persistence.ChangeRepository;
import no.sikt.nva.approvals.persistence.DynamoDbApprovalRepository;
import no.sikt.nva.approvals.persistence.DynamoDbChangeRepository;
import no.sikt.nva.approvals.source.SourceClient;
import no.sikt.nva.approvals.source.SourceClientException;
import no.sikt.nva.approvals.source.SourceResponse;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;

public class SnapshotServiceImpl implements SnapshotService {

  private static final String SOURCE_NOT_FOUND_MESSAGE = "Source %s of approval %s was not found";

  private final ApprovalRepository approvalRepository;
  private final ChangeRepository changeRepository;
  private final SourceClient sourceClient;

  public SnapshotServiceImpl(
      ApprovalRepository approvalRepository,
      ChangeRepository changeRepository,
      SourceClient sourceClient) {
    this.approvalRepository = approvalRepository;
    this.changeRepository = changeRepository;
    this.sourceClient = sourceClient;
  }

  @JacocoGenerated
  public static SnapshotService defaultInstance(Environment environment) {
    return new SnapshotServiceImpl(
        DynamoDbApprovalRepository.defaultInstance(environment),
        DynamoDbChangeRepository.defaultInstance(environment),
        SourceClient.defaultInstance(environment));
  }

  @Override
  public void createSnapshot(SourceChange sourceChange) throws SourceClientException {
    var content = fetchContent(sourceChange);
    if (hasChangedSinceLatestSnapshot(sourceChange, content)) {
      approvalRepository.save(SourceSnapshot.create(sourceChange, content));
    }
  }

  private boolean hasChangedSinceLatestSnapshot(SourceChange sourceChange, Content content) {
    return changeRepository
        .findLatestSnapshot(sourceChange.approvalIdentifier())
        .map(latestSnapshot -> !latestSnapshot.content().hash().equals(content.hash()))
        .orElse(true);
  }

  private Content fetchContent(SourceChange sourceChange) throws SourceClientException {
    return sourceClient
        .fetchSource(sourceChange.source(), sourceChange.customerIdentifier())
        .map(SnapshotServiceImpl::toContent)
        .orElseThrow(
            () ->
                new SourceClientException(
                    SOURCE_NOT_FOUND_MESSAGE.formatted(
                        sourceChange.source(), sourceChange.approvalIdentifier())));
  }

  private static Content toContent(SourceResponse sourceResponse) {
    return Content.create(sourceResponse.contentType(), sourceResponse.body());
  }
}
