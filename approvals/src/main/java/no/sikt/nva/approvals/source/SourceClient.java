package no.sikt.nva.approvals.source;

import java.net.URI;
import java.util.Optional;

public interface SourceClient<T extends SourceDocument> {

  URI getBaseUrl();

  Optional<T> fetch(URI source) throws SourceClientException;
}
