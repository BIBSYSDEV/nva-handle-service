package no.sikt.nva.approvals.source;

public record OAuth2Credentials(
    String clientId, String clientSecret, String accessTokenUrl, String scope) {}
