package no.sikt.nva.approvals.index;

import static java.util.Objects.isNull;

import com.fasterxml.jackson.annotation.JsonTypeName;
import no.sikt.nva.approvals.persistence.ScanCursor;

@JsonTypeName("Scan")
public record ScanRequest(ScanCursor cursor) implements ReindexMessage {

  private static final String MISSING_CURSOR_MESSAGE = "cursor is required";

  public ScanRequest {
    if (isNull(cursor)) {
      throw new IllegalArgumentException(MISSING_CURSOR_MESSAGE);
    }
  }
}
