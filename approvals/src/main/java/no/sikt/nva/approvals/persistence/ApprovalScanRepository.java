package no.sikt.nva.approvals.persistence;

@FunctionalInterface
public interface ApprovalScanRepository {

  ApprovalIdentifierPage scanApprovals(ScanCursor cursor);
}
