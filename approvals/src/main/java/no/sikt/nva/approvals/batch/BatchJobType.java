package no.sikt.nva.approvals.batch;

import no.sikt.nva.approvals.persistence.ApprovalDao;

public enum BatchJobType {
  REINDEX_APPROVALS;

  public String entityType() {
    return switch (this) {
      case REINDEX_APPROVALS -> ApprovalDao.TYPE;
    };
  }
}
