package no.sikt.nva.approvals.dmp;

import no.sikt.nva.approvals.source.OAuth2Credentials;

public record DmpClientSecrets(
    String clientId, String clientSecret, String accessTokenUrl, String scope, String baseUrl) {

  public OAuth2Credentials toOAuth2Credentials() {
    return new OAuth2Credentials(clientId, clientSecret, accessTokenUrl, scope);
  }
}
