package no.sikt.nva.approvals.source;

import java.net.URI;
import java.util.Optional;

public interface SourceClient {

  URI getBaseUrl();

  Optional<Source> fetch(URI uri) throws SourceClientException;
}
