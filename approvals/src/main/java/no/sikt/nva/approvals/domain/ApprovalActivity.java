package no.sikt.nva.approvals.domain;

import com.fasterxml.jackson.annotation.JsonValue;

public enum ApprovalActivity {
  CREATE_APPROVAL("CreateApproval"),
  UPDATE_APPROVAL("UpdateApproval");

  private final String value;

  ApprovalActivity(String value) {
    this.value = value;
  }

  @JsonValue
  public String getValue() {
    return value;
  }
}
