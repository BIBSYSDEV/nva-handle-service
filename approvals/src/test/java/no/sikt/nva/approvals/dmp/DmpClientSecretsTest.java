package no.sikt.nva.approvals.dmp;

import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import no.sikt.nva.approvals.source.OAuth2Credentials;
import org.junit.jupiter.api.Test;

class DmpClientSecretsTest {

  @Test
  void shouldProvideOAuth2CredentialsFromDmpSecrets() {
    var clientId = randomString();
    var clientSecret = randomString();
    var accessTokenUrl = randomString();
    var scope = randomString();
    var secrets =
        new DmpClientSecrets(clientId, clientSecret, accessTokenUrl, scope, randomString());

    assertThat(
        secrets.toOAuth2Credentials(),
        equalTo(new OAuth2Credentials(clientId, clientSecret, accessTokenUrl, scope)));
  }
}
