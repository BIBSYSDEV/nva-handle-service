package no.sikt.nva.approvals.source;

import java.net.URI;
import java.util.UUID;

public class UnregisteredSourceException extends SourceClientException {

  private static final String MESSAGE = "Source %s matches no source config of customer %s";

  public UnregisteredSourceException(URI source, UUID customerIdentifier) {
    super(MESSAGE.formatted(source, customerIdentifier));
  }
}
