package no.sikt.nva.approvals.index;

import com.fasterxml.jackson.annotation.JsonTypeName;

@JsonTypeName("SegmentedScan")
public record SegmentedScanRequest(int totalSegments) implements ReindexMessage {

  private static final int MIN_SEGMENTS = 1;
  private static final int MAX_SEGMENTS = 100;
  private static final String INVALID_TOTAL_SEGMENTS_MESSAGE =
      "totalSegments must be between %s and %s, was %s";

  public SegmentedScanRequest {
    if (totalSegments < MIN_SEGMENTS || totalSegments > MAX_SEGMENTS) {
      throw new IllegalArgumentException(
          INVALID_TOTAL_SEGMENTS_MESSAGE.formatted(MIN_SEGMENTS, MAX_SEGMENTS, totalSegments));
    }
  }
}
