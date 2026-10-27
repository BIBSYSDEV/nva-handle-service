package no.sikt.nva.approvals.rest;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import static no.sikt.nva.approvals.utils.UriConstants.CONTEXT_PROPERTY;
import static no.sikt.nva.approvals.utils.UriConstants.changeId;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.net.URI;
import java.time.Instant;
import no.sikt.nva.approvals.domain.SourceSnapshot;

@JsonInclude(NON_NULL)
@JsonTypeName("SourceSnapshot")
public record SourceSnapshotResponse(
    @JsonProperty(CONTEXT_PROPERTY) URI context,
    URI id,
    URI specializationOf,
    Instant generatedAtTime,
    String mediaType,
    Checksum checksum,
    SourceIngestion wasGeneratedBy)
    implements ChangeResponse {

  private static final String CHECKSUM_ALGORITHM = "sha256";

  public static SourceSnapshotResponse create(SourceSnapshot snapshot, String apiHost) {
    return new SourceSnapshotResponse(
        null,
        changeId(apiHost, snapshot.approvalIdentifier(), snapshot.identifier()),
        snapshot.source(),
        snapshot.createdDate(),
        snapshot.content().type(),
        new Checksum(CHECKSUM_ALGORITHM, snapshot.content().hash()),
        SourceIngestion.create(snapshot, apiHost));
  }

  @Override
  public ChangeResponse withContext(URI context) {
    return new SourceSnapshotResponse(
        context, id, specializationOf, generatedAtTime, mediaType, checksum, wasGeneratedBy);
  }

  @JsonTypeInfo(use = Id.NAME, property = "type")
  @JsonTypeName("Checksum")
  public record Checksum(String algorithm, String checksumValue) {}
}
