package no.sikt.nva.approvals.snapshot;

import java.time.Clock;
import no.sikt.nva.approvals.domain.Content;
import no.sikt.nva.approvals.domain.SourceSnapshot;
import no.sikt.nva.approvals.persistence.ApprovalRepository;
import no.sikt.nva.approvals.persistence.DynamoDbApprovalRepository;
import no.sikt.nva.approvals.source.SourceClient;
import no.sikt.nva.approvals.source.SourceClientException;
import no.sikt.nva.approvals.source.SourceResponse;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;

public class SnapshotServiceImpl implements SnapshotService {

  private static final String SOURCE_NOT_FOUND_MESSAGE = "Source %s of approval %s was not found";

  private final ApprovalRepository approvalRepository;
  private final SourceClient sourceClient;
  private final Clock clock;

  public SnapshotServiceImpl(
      ApprovalRepository approvalRepository, SourceClient sourceClient, Clock clock) {
    this.approvalRepository = approvalRepository;
    this.sourceClient = sourceClient;
    this.clock = clock;
  }

  @JacocoGenerated
  public static SnapshotService defaultInstance(Environment environment) {
    return new SnapshotServiceImpl(
        DynamoDbApprovalRepository.defaultInstance(environment),
        SourceClient.defaultInstance(environment),
        Clock.systemUTC());
  }

  @Override
  public void createSnapshot(SourceChange sourceChange) throws SourceClientException {
    var content = fetchContent(sourceChange);
    approvalRepository.save(SourceSnapshot.create(sourceChange, content, clock.instant()));
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
