package no.sikt.nva.approvals.batch;

import static java.util.Objects.nonNull;
import static nva.commons.core.attempt.Try.attempt;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import no.unit.nva.commons.json.JsonUtils;

record BatchJobEvent(@JsonProperty("Records") List<BatchJobEventRecord> records) {

  private static final String PARSE_FAILURE_MESSAGE = "Could not parse BatchJobEvent";
  private static final String TOO_MANY_RECORDS_MESSAGE =
      "BatchJobEvent carries more than one record, batch size must be one";
  private static final int SINGLE_RECORD = 1;

  static BatchJobMessage toMessage(String event) {
    return BatchJobMessage.fromString(requestBody(event));
  }

  private static String requestBody(String event) {
    var batchJobEvent = parse(event);
    return batchJobEvent.isQueueEvent() ? batchJobEvent.onlyBody() : event;
  }

  private static BatchJobEvent parse(String event) {
    return attempt(() -> JsonUtils.dtoObjectMapper.readValue(event, BatchJobEvent.class))
        .orElseThrow(
            failure -> new IllegalArgumentException(PARSE_FAILURE_MESSAGE, failure.getException()));
  }

  private boolean isQueueEvent() {
    return nonNull(records()) && !records().isEmpty();
  }

  private String onlyBody() {
    if (records().size() > SINGLE_RECORD) {
      throw new IllegalArgumentException(TOO_MANY_RECORDS_MESSAGE);
    }
    return records().getFirst().body();
  }

  record BatchJobEventRecord(String body) {}
}
