package no.sikt.nva.approvals.domain;

import static java.util.Objects.requireNonNull;

import java.net.URI;

public record SourceConfig(URI baseUri, SourceAuthentication authentication) {

  private static final String BASE_URI_MESSAGE = "baseUri is mandatory";
  private static final String AUTHENTICATION_MESSAGE = "authentication is mandatory";

  public SourceConfig {
    requireNonNull(baseUri, BASE_URI_MESSAGE);
    requireNonNull(authentication, AUTHENTICATION_MESSAGE);
  }

  public boolean matches(URI source) {
    return source.isAbsolute() && !baseUri.relativize(source).isAbsolute();
  }
}
