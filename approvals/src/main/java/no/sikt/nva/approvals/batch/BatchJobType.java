package no.sikt.nva.approvals.batch;

import no.sikt.nva.approvals.persistence.ApprovalDao;

public enum BatchJobType {
  REINDEX_APPROVALS,
  BACKFILL_REVISIONS;

  public String entityType() {
    return switch (this) {
      case REINDEX_APPROVALS, BACKFILL_REVISIONS -> ApprovalDao.TYPE;
    };
  }
}
