package no.sikt.nva.approvals.events;

import static java.nio.charset.StandardCharsets.UTF_8;
import static nva.commons.core.attempt.Try.attempt;

import java.net.URI;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import no.sikt.nva.approvals.domain.Approval;
import no.sikt.nva.approvals.domain.ApprovalNotFoundException;
import no.sikt.nva.approvals.persistence.ApprovalRepository;
import no.sikt.nva.approvals.persistence.DynamoDbApprovalRepository;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;

public class ApprovalEventService {

  private static final String HASH_ALGORITHM = "SHA-256";
  private static final String EVENT_KEY_SEPARATOR = "|";
  private static final String SOURCE_MISMATCH_MESSAGE =
      "Event source %s does not match the source of approval %s";
  private static final String UNREGISTERED_SOURCE_MESSAGE =
      "Event source %s does not belong to a registered source system";
  private final ApprovalRepository approvalRepository;
  private final SourceSystemRegister sourceSystemRegister;
  private final PullQueue pullQueue;

  public ApprovalEventService(
      ApprovalRepository approvalRepository,
      SourceSystemRegister sourceSystemRegister,
      PullQueue pullQueue) {
    this.approvalRepository = approvalRepository;
    this.sourceSystemRegister = sourceSystemRegister;
    this.pullQueue = pullQueue;
  }

  @JacocoGenerated
  public static ApprovalEventService defaultInstance(Environment environment) {
    return new ApprovalEventService(
        DynamoDbApprovalRepository.defaultInstance(environment),
        SourceSystemRegister.defaultInstance(environment),
        SqsPullQueue.defaultInstance(environment));
  }

  /**
   * Records a change notification and enqueues one pull of the approval source. A notification with
   * a {@code source} and {@code id} that has been received before is accepted without a second
   * pull.
   */
  public void receive(CloudEvent cloudEvent, String clientId, URI customerId)
      throws ApprovalNotFoundException {
    var handle = cloudEvent.handle();
    var approval =
        approvalRepository
            .findByHandle(handle)
            .orElseThrow(() -> new ApprovalNotFoundException(handle));
    ensureSourceMatchesApproval(cloudEvent, approval);
    ensureSourceIsRegistered(cloudEvent);

    var approvalEvent =
        new ApprovalEvent(
            eventKey(cloudEvent),
            approval.identifier(),
            cloudEvent,
            Instant.now(),
            clientId,
            customerId);
    if (approvalRepository.saveEventIfAbsent(approvalEvent)) {
      pullQueue.enqueue(new PullRequest(approval.identifier(), approvalEvent.eventKey()));
    }
  }

  /**
   * The key of a notification is derived from its {@code source} and {@code id}, so the id of the
   * third party is never used as a key. The separator cannot occur in a URI or an event id, which
   * keeps two different pairs from producing the same key.
   */
  private static String eventKey(CloudEvent cloudEvent) {
    var keyMaterial = cloudEvent.source() + EVENT_KEY_SEPARATOR + cloudEvent.id();
    var digest =
        attempt(() -> MessageDigest.getInstance(HASH_ALGORITHM))
            .orElseThrow()
            .digest(keyMaterial.getBytes(UTF_8));
    return HexFormat.of().formatHex(digest);
  }

  private static void ensureSourceMatchesApproval(CloudEvent cloudEvent, Approval approval) {
    if (!approval.source().equals(cloudEvent.source())) {
      throw new IllegalArgumentException(
          SOURCE_MISMATCH_MESSAGE.formatted(cloudEvent.source(), approval.identifier()));
    }
  }

  private void ensureSourceIsRegistered(CloudEvent cloudEvent) {
    if (sourceSystemRegister.resolve(cloudEvent.source()).isEmpty()) {
      throw new IllegalArgumentException(
          UNREGISTERED_SOURCE_MESSAGE.formatted(cloudEvent.source()));
    }
  }
}
