package no.sikt.nva.approvals.domain;

import static java.time.temporal.ChronoUnit.MICROS;

import java.net.URI;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

public record ApprovalRevision(
    String changeId,
    Approval approval,
    Instant createdDate,
    ApprovalActivity activity,
    URI context,
    URI ontology) {

  private static final DateTimeFormatter CHANGE_ID_TIMESTAMP_FORMAT =
      DateTimeFormatter.ofPattern("uuuu-MM-dd'T'HH:mm:ss.SSSSSS'Z'").withZone(ZoneOffset.UTC);
  private static final String CHANGE_ID_FORMAT = "%s_%s";

  public static ApprovalRevision create(
      Approval approval,
      ApprovalActivity activity,
      URI context,
      URI ontology,
      Instant createdDate) {
    var timestamp = createdDate.truncatedTo(MICROS);
    return new ApprovalRevision(
        createChangeId(timestamp), approval, timestamp, activity, context, ontology);
  }

  private static String createChangeId(Instant timestamp) {
    return CHANGE_ID_FORMAT.formatted(
        CHANGE_ID_TIMESTAMP_FORMAT.format(timestamp), UUID.randomUUID());
  }
}
