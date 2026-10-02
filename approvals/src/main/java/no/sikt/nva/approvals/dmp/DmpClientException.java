package no.sikt.nva.approvals.dmp;

import no.sikt.nva.approvals.source.SourceClientException;

public class DmpClientException extends SourceClientException {

  public DmpClientException(String message) {
    super(message);
  }

  public DmpClientException(String message, Throwable cause) {
    super(message, cause);
  }
}
