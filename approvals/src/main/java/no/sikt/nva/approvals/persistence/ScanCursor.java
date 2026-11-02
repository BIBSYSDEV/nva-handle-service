package no.sikt.nva.approvals.persistence;

import java.util.Objects;

public record ScanCursor(int segment, int totalSegments, String partitionKey, String sortKey) {

  private static final int FIRST_SEGMENT = 0;
  private static final int SINGLE_SEGMENT = 1;

  public static ScanCursor start() {
    return start(FIRST_SEGMENT, SINGLE_SEGMENT);
  }

  public static ScanCursor start(int segment, int totalSegments) {
    return new ScanCursor(segment, totalSegments, null, null);
  }

  public boolean isStart() {
    return Objects.isNull(partitionKey);
  }
}
