package no.sikt.nva.approvals.source;

public class SourceClientException extends Exception {

  public SourceClientException(String message) {
    super(message);
  }

  public SourceClientException(String message, Throwable cause) {
    super(message, cause);
  }
}
