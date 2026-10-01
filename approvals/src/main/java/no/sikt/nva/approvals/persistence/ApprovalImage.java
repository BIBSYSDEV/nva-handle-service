package no.sikt.nva.approvals.persistence;

import static nva.commons.core.attempt.Try.attempt;

import java.net.URI;
import java.util.Collection;
import java.util.UUID;
import no.sikt.nva.approvals.domain.Approval;
import no.sikt.nva.approvals.domain.ApprovalRevision;
import no.sikt.nva.approvals.domain.Handle;
import no.sikt.nva.approvals.domain.NamedIdentifier;
import no.unit.nva.commons.json.JsonSerializable;
import no.unit.nva.commons.json.JsonUtils;

public record ApprovalImage(
    Collection<IdentifierImage> identifiers, URI source, URI handle, URI context, URI ontology)
    implements JsonSerializable {

  public static final int SCHEMA_VERSION = 1;

  public static ApprovalImage fromApprovalRevision(ApprovalRevision revision) {
    var approval = revision.approval();
    return new ApprovalImage(
        approval.namedIdentifiers().stream().map(IdentifierImage::fromNamedIdentifier).toList(),
        approval.source(),
        approval.handle().value(),
        revision.context(),
        revision.ontology());
  }

  public static ApprovalImage fromJson(String json) {
    return attempt(() -> JsonUtils.dtoObjectMapper.readValue(json, ApprovalImage.class))
        .orElseThrow();
  }

  public Approval toApproval(UUID approvalIdentifier, UUID customerIdentifier) {
    return new Approval(
        approvalIdentifier,
        identifiers.stream().map(IdentifierImage::toNamedIdentifier).toList(),
        source,
        new Handle(handle),
        customerIdentifier);
  }

  public record IdentifierImage(String name, String value) {

    public static IdentifierImage fromNamedIdentifier(NamedIdentifier namedIdentifier) {
      return new IdentifierImage(namedIdentifier.name(), namedIdentifier.value());
    }

    public NamedIdentifier toNamedIdentifier() {
      return new NamedIdentifier(name, value);
    }
  }
}
