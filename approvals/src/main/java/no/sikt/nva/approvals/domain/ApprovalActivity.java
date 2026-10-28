package no.sikt.nva.approvals.domain;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Arrays;

public enum ApprovalActivity {
  CREATE_APPROVAL("CreateApproval"),
  UPDATE_APPROVAL("UpdateApproval");

  private static final String UNKNOWN_ACTIVITY_MESSAGE = "Unknown approval activity: %s";

  private final String value;

  ApprovalActivity(String value) {
    this.value = value;
  }

  @JsonCreator
  public static ApprovalActivity fromValue(String value) {
    return Arrays.stream(values())
        .filter(activity -> activity.value.equals(value))
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException(UNKNOWN_ACTIVITY_MESSAGE.formatted(value)));
  }

  @JsonValue
  public String getValue() {
    return value;
  }
}
