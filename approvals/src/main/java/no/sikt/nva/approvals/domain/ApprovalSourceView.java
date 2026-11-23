package no.sikt.nva.approvals.domain;

import static java.util.Objects.isNull;

import java.time.Instant;

public record ApprovalSourceView(SourceView view, Instant viewModified, Instant sourceModified) {

  public static final ApprovalSourceView NONE = new ApprovalSourceView(null, null, null);

  public boolean isEmpty() {
    return isNull(viewModified) && isNull(sourceModified);
  }
}
