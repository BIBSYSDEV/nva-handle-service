package no.sikt.nva.approvals.source;

public record OAuth2Credentials(
    String clientId, String clientSecret, String accessTokenUrl, String scope) {

  private static final String DESCRIPTION =
      "OAuth2Credentials[clientId=%s, clientSecret=%s, accessTokenUrl=%s, scope=%s]";
  private static final String MASKED_SECRET = "****";

  @Override
  public String toString() {
    return DESCRIPTION.formatted(clientId, MASKED_SECRET, accessTokenUrl, scope);
  }
}
