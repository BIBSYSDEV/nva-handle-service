package no.sikt.nva.approvals.rest;

import static no.sikt.nva.approvals.rest.RestConstants.CONTEXT_PROPERTY;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.net.URI;
import java.time.Instant;
import java.util.Optional;

@JsonTypeName("SourceSnapshot")
public record SourceSnapshotResponse(
    @JsonProperty(CONTEXT_PROPERTY) URI context,
    URI id,
    URI approval,
    URI source,
    Instant retrievedAt,
    String contentType,
    String contentHash,
    WasGeneratedBy wasGeneratedBy)
    implements ChangeResponse {

  private static final String CONTENT_HASH_PREFIX = "sha256:%s";

  public SourceSnapshotResponse {
    contentHash = Optional.ofNullable(contentHash).map(CONTENT_HASH_PREFIX::formatted).orElse(null);
  }
}
