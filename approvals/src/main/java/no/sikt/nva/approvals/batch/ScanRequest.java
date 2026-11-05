package no.sikt.nva.approvals.batch;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

import com.fasterxml.jackson.annotation.JsonTypeName;
import no.sikt.nva.approvals.persistence.ScanCursor;

@JsonTypeName("Scan")
public record ScanRequest(
    BatchJobType jobType, ScanCursor cursor, int pageSize, Integer remainingItems)
    implements BatchJobMessage {

  private static final int MINIMUM_POSITIVE = 1;
  private static final String MISSING_CURSOR_MESSAGE = "cursor is required";
  private static final String INVALID_PAGE_SIZE_MESSAGE = "pageSize must be positive";
  private static final String INVALID_REMAINING_ITEMS_MESSAGE =
      "remainingItems must be positive when given";

  public ScanRequest {
    if (isNull(cursor)) {
      throw new IllegalArgumentException(MISSING_CURSOR_MESSAGE);
    }
    if (pageSize < MINIMUM_POSITIVE) {
      throw new IllegalArgumentException(INVALID_PAGE_SIZE_MESSAGE);
    }
    if (nonNull(remainingItems) && remainingItems < MINIMUM_POSITIVE) {
      throw new IllegalArgumentException(INVALID_REMAINING_ITEMS_MESSAGE);
    }
  }
}
