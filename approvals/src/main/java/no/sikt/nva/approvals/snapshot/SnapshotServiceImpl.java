package no.sikt.nva.approvals.snapshot;

import java.time.Instant;
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
  public void createSnapshot(CreateSnapshotRequest createSnapshotRequest)
      throws SourceClientException {
    var content = fetchContent(createSnapshotRequest);
    if (hasChangedSinceLatestSnapshot(createSnapshotRequest, content)) {
      approvalRepository.save(SourceSnapshot.create(createSnapshotRequest, content, Instant.now()));
    }
  }

  private boolean hasChangedSinceLatestSnapshot(
      CreateSnapshotRequest createSnapshotRequest, Content content) {
    return changeRepository
        .findLatestSnapshot(createSnapshotRequest.approvalIdentifier())
        .map(latestSnapshot -> !latestSnapshot.content().hash().equals(content.hash()))
        .orElse(true);
  }

  private Content fetchContent(CreateSnapshotRequest createSnapshotRequest)
      throws SourceClientException {
    return sourceClient
        .fetchSource(
            createSnapshotRequest.source(), createSnapshotRequest.agent().customerIdentifier())
        .map(SnapshotServiceImpl::toContent)
        .orElseThrow(
            () ->
                new SourceClientException(
                    "Source %s of approval %s was not found"
                        .formatted(
                            createSnapshotRequest.source(),
                            createSnapshotRequest.approvalIdentifier())));
  }

  private static Content toContent(SourceResponse sourceResponse) {
    return Content.create(sourceResponse.contentType(), sourceResponse.body());
  }
}
