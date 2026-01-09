package no.sikt.nva.approvals.dmp;

public class DmpClientException extends Exception {

    public DmpClientException(String message) {
        super(message);
    }

    public DmpClientException(String message, Throwable cause) {
        super(message, cause);
    }
}
