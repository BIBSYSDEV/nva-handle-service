package no.sikt.nva.approvals.persistence;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.net.URI;
import no.sikt.nva.approvals.domain.SourceAuthentication;
import no.sikt.nva.approvals.domain.SourceAuthentication.NoAuthentication;
import no.sikt.nva.approvals.domain.SourceAuthentication.OAuth2ClientCredentials;
import no.sikt.nva.approvals.domain.SourceConfig;

public record SourceConfigDao(URI baseUri, AuthenticationDao authentication) {

  public static SourceConfigDao fromSourceConfig(SourceConfig sourceConfig) {
    return new SourceConfigDao(
        sourceConfig.baseUri(),
        AuthenticationDao.fromAuthentication(sourceConfig.authentication()));
  }

  public SourceConfig toSourceConfig() {
    return new SourceConfig(baseUri, authentication.toAuthentication());
  }

  @JsonTypeInfo(use = Id.NAME, property = "type")
  @JsonSubTypes({
    @JsonSubTypes.Type(NoAuthenticationDao.class),
    @JsonSubTypes.Type(OAuth2ClientCredentialsDao.class)
  })
  public sealed interface AuthenticationDao {

    static AuthenticationDao fromAuthentication(SourceAuthentication authentication) {
      return switch (authentication) {
        case NoAuthentication _ -> new NoAuthenticationDao();
        case OAuth2ClientCredentials credentials ->
            new OAuth2ClientCredentialsDao(credentials.secretName());
      };
    }

    SourceAuthentication toAuthentication();
  }

  @JsonTypeName("NoAuthentication")
  public record NoAuthenticationDao() implements AuthenticationDao {

    @Override
    public SourceAuthentication toAuthentication() {
      return new NoAuthentication();
    }
  }

  @JsonTypeName("OAuth2ClientCredentials")
  public record OAuth2ClientCredentialsDao(String secretName) implements AuthenticationDao {

    @Override
    public SourceAuthentication toAuthentication() {
      return new OAuth2ClientCredentials(secretName);
    }
  }
}
