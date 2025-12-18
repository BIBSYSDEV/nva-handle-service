package no.sikt.nva.approvals.domain;

public class ApprovalServiceException extends Exception {

    public ApprovalServiceException(String message) {
        super(message);
    }

    public ApprovalServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
