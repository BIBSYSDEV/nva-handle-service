package no.sikt.nva.approvals.domain;

public interface ApprovalService {

    void create(Approval approval) throws ApprovalServiceException, ApprovalConflictException;
}
