package no.sikt.nva.approvals.snapshot;

import java.time.Instant;
import java.util.Optional;
import no.sikt.nva.approvals.domain.ApprovalModifiedException;
import no.sikt.nva.approvals.domain.Content;
import no.sikt.nva.approvals.domain.SourceSnapshot;
import no.sikt.nva.approvals.persistence.ChangeRepository;
import no.sikt.nva.approvals.persistence.DynamoDbChangeRepository;
import no.sikt.nva.approvals.persistence.DynamoDbLatestSnapshotRepository;
import no.sikt.nva.approvals.persistence.LatestSnapshotRepository;
import no.sikt.nva.approvals.persistence.SnapshotState;
import no.sikt.nva.approvals.source.SourceClient;
import no.sikt.nva.approvals.source.SourceClientException;
import no.sikt.nva.approvals.source.SourceResponse;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;

public class SnapshotServiceImpl implements SnapshotService {

  private static final int FIRST_ATTEMPT = 1;
  private static final int MAX_ATTEMPTS = 3;

  private final LatestSnapshotRepository latestSnapshotRepository;
  private final ChangeRepository changeRepository;
  private final SourceClient sourceClient;

  public SnapshotServiceImpl(
      LatestSnapshotRepository latestSnapshotRepository,
      ChangeRepository changeRepository,
      SourceClient sourceClient) {
    this.latestSnapshotRepository = latestSnapshotRepository;
    this.changeRepository = changeRepository;
    this.sourceClient = sourceClient;
  }

  @JacocoGenerated
  public static SnapshotService defaultInstance(Environment environment) {
    return new SnapshotServiceImpl(
        DynamoDbLatestSnapshotRepository.defaultInstance(environment),
        DynamoDbChangeRepository.defaultInstance(environment),
        SourceClient.defaultInstance(environment));
  }

  @Override
  public void createSnapshot(CreateSnapshotRequest createSnapshotRequest)
      throws SourceClientException {
    for (var attempt = FIRST_ATTEMPT; attempt <= MAX_ATTEMPTS; attempt++) {
      var state = findStateOfCurrentSource(createSnapshotRequest);
      if (state.isEmpty()) {
        return;
      }
      var content = fetchContent(createSnapshotRequest);
      try {
        saveIfChanged(createSnapshotRequest, content, state.get());
        return;
      } catch (ApprovalModifiedException exception) {
        if (attempt == MAX_ATTEMPTS) {
          throw exception;
        }
      }
    }
  }

  private void saveIfChanged(
      CreateSnapshotRequest createSnapshotRequest, Content content, SnapshotState state) {
    if (hasChangedSinceLatestSnapshot(createSnapshotRequest, content)) {
      latestSnapshotRepository.saveAsLatestSnapshot(
          SourceSnapshot.create(createSnapshotRequest, content, Instant.now()), state);
    }
  }

  private Optional<SnapshotState> findStateOfCurrentSource(
      CreateSnapshotRequest createSnapshotRequest) {
    return latestSnapshotRepository
        .findSnapshotStateConsistently(createSnapshotRequest.approvalIdentifier())
        .filter(state -> createSnapshotRequest.source().equals(state.source()));
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
