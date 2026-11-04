package no.sikt.nva.approvals.persistence;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.Objects;

public record ScanCursor(int segment, int totalSegments, String partitionKey, String sortKey) {

  public static ScanCursor start(int segment, int totalSegments) {
    return new ScanCursor(segment, totalSegments, null, null);
  }

  @JsonIgnore
  public boolean isStart() {
    return Objects.isNull(partitionKey);
  }
}
