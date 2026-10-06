package no.sikt.nva.approvals.domain;

import static java.util.Objects.requireNonNull;

import java.net.URI;
import java.util.Objects;

public record SourceConfig(URI baseUri, SourceAuthentication authentication) {

  private static final String BASE_URI_MESSAGE = "baseUri is mandatory";
  private static final String AUTHENTICATION_MESSAGE = "authentication is mandatory";
  private static final String PATH_SEPARATOR = "/";

  public SourceConfig {
    requireNonNull(baseUri, BASE_URI_MESSAGE);
    requireNonNull(authentication, AUTHENTICATION_MESSAGE);
  }

  public boolean matches(URI source) {
    return Objects.equals(baseUri.getScheme(), source.getScheme())
        && Objects.equals(baseUri.getRawAuthority(), source.getRawAuthority())
        && isWithinBasePath(source);
  }

  private boolean isWithinBasePath(URI source) {
    var basePath = withTrailingSeparator(baseUri.getRawPath());
    return withTrailingSeparator(source.getRawPath()).startsWith(basePath);
  }

  private static String withTrailingSeparator(String path) {
    return path.endsWith(PATH_SEPARATOR) ? path : path + PATH_SEPARATOR;
  }
}
