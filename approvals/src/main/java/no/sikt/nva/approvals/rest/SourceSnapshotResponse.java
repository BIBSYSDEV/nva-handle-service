package no.sikt.nva.approvals.rest;

import static no.sikt.nva.approvals.utils.UriConstants.changeId;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.net.URI;
import java.time.Instant;
import no.sikt.nva.approvals.domain.SourceSnapshot;

@JsonTypeName("SourceSnapshot")
public record SourceSnapshotResponse(
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
        changeId(apiHost, snapshot.approvalIdentifier(), snapshot.identifier()),
        snapshot.source(),
        snapshot.createdDate(),
        snapshot.content().type(),
        new Checksum(CHECKSUM_ALGORITHM, snapshot.content().hash()),
        SourceIngestion.create(snapshot, apiHost));
  }

  @JsonTypeInfo(use = Id.NAME, property = "type")
  @JsonTypeName("Checksum")
  public record Checksum(String algorithm, String checksumValue) {}
}
