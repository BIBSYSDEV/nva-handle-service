package no.sikt.nva.approvals.events;

import java.net.URI;
import java.util.Objects;
import java.util.Optional;
import nva.commons.core.StringUtils;

/**
 * A third-party system NVA accepts approval sources from. A source belongs to the system when it
 * has the same scheme, host and port as {@code baseUri} and its path lies below the path of {@code
 * baseUri}.
 */
public record SourceSystem(String id, URI baseUri) {

  private static final String PATH_SEPARATOR = "/";
  private static final String MISSING_ID_MESSAGE = "Source system id is mandatory";
  private static final String INVALID_BASE_URI_MESSAGE =
      "Source system %s must have an absolute baseUri with a host, but was %s";

  public SourceSystem {
    if (StringUtils.isBlank(id)) {
      throw new IllegalArgumentException(MISSING_ID_MESSAGE);
    }
    if (Objects.isNull(baseUri) || !baseUri.isAbsolute() || Objects.isNull(baseUri.getHost())) {
      throw new IllegalArgumentException(INVALID_BASE_URI_MESSAGE.formatted(id, baseUri));
    }
  }

  public boolean covers(URI source) {
    return baseUri.getScheme().equalsIgnoreCase(source.getScheme())
        && baseUri.getHost().equalsIgnoreCase(source.getHost())
        && baseUri.getPort() == source.getPort()
        && pathOf(source).startsWith(withTrailingSeparator(pathOf(baseUri)));
  }

  private static String pathOf(URI uri) {
    return Optional.ofNullable(uri.getPath()).orElse(StringUtils.EMPTY_STRING);
  }

  private static String withTrailingSeparator(String path) {
    return path.endsWith(PATH_SEPARATOR) ? path : path + PATH_SEPARATOR;
  }
}
