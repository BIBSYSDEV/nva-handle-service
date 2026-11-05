package no.sikt.nva.approvals.batch;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNullElse;

import com.fasterxml.jackson.annotation.JsonTypeName;

@JsonTypeName("StartBatchJob")
public record StartBatchJobRequest(
    BatchJobType jobType, Integer totalSegments, Integer pageSize, Integer maxItems)
    implements BatchJobMessage {

  static final int DEFAULT_TOTAL_SEGMENTS = 10;
  static final int DEFAULT_PAGE_SIZE = 700;
  private static final int MIN_SEGMENTS = 1;
  private static final int MINIMUM_POSITIVE = 1;
  private static final int MAX_SEGMENTS = 100;
  private static final String MISSING_JOB_TYPE_MESSAGE = "jobType is required";
  private static final String INVALID_TOTAL_SEGMENTS_MESSAGE =
      "totalSegments must be between %s and %s, was %s";
  private static final String INVALID_PAGE_SIZE_MESSAGE = "pageSize must be positive, was %s";
  private static final String INVALID_MAX_ITEMS_MESSAGE = "maxItems must be positive, was %s";

  public StartBatchJobRequest {
    if (isNull(jobType)) {
      throw new IllegalArgumentException(MISSING_JOB_TYPE_MESSAGE);
    }
    totalSegments = requireNonNullElse(totalSegments, DEFAULT_TOTAL_SEGMENTS);
    pageSize = requireNonNullElse(pageSize, DEFAULT_PAGE_SIZE);
    if (totalSegments < MIN_SEGMENTS || totalSegments > MAX_SEGMENTS) {
      throw new IllegalArgumentException(
          INVALID_TOTAL_SEGMENTS_MESSAGE.formatted(MIN_SEGMENTS, MAX_SEGMENTS, totalSegments));
    }
    if (pageSize < MINIMUM_POSITIVE) {
      throw new IllegalArgumentException(INVALID_PAGE_SIZE_MESSAGE.formatted(pageSize));
    }
    if (nonPositive(maxItems)) {
      throw new IllegalArgumentException(INVALID_MAX_ITEMS_MESSAGE.formatted(maxItems));
    }
  }

  private static boolean nonPositive(Integer maxItems) {
    return nonNull(maxItems) && maxItems < MINIMUM_POSITIVE;
  }
}
