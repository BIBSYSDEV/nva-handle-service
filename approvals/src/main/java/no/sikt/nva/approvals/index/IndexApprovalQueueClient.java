package no.sikt.nva.approvals.index;

import java.util.Collection;
import java.util.UUID;
import no.sikt.nva.approvals.persistence.ScanCursor;

public interface IndexApprovalQueueClient {

  void sendApprovals(Collection<UUID> approvalIdentifiers);

  void enqueueNextScan(ScanCursor cursor);
}
