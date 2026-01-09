package no.sikt.nva.approvals.dmp;

public record DmpClientSecrets(
    String clientId,
    String clientSecret,
    String accessTokenUrl,
    String scope,
    String baseUrl
) {
}
