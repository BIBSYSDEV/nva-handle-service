package no.sikt.nva.approvals.domain;

import static java.time.temporal.ChronoUnit.MICROS;

import java.net.URI;
import java.time.Instant;
import java.util.UUID;
import no.unit.nva.identifiers.SortableIdentifier;

public record ApprovalRevision(
    SortableIdentifier identifier,
    Approval approval,
    Instant createdDate,
    ApprovalActivity activity,
    URI context,
    URI ontology)
    implements Change {

  public static ApprovalRevision create(
      Approval approval,
      ApprovalActivity activity,
      URI context,
      URI ontology,
      Instant createdDate) {
    var timestamp = createdDate.truncatedTo(MICROS);
    return new ApprovalRevision(
        SortableIdentifier.create(timestamp, UUID.randomUUID()),
        approval,
        timestamp,
        activity,
        context,
        ontology);
  }
}
