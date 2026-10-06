package no.sikt.nva.approvals.source;

import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;

import org.junit.jupiter.api.Test;

class OAuth2CredentialsTest {

  @Test
  void shouldNotExposeClientSecretInToString() {
    var clientSecret = randomString();
    var credentials =
        new OAuth2Credentials(randomString(), clientSecret, randomString(), randomString());

    assertThat(credentials.toString(), not(containsString(clientSecret)));
  }
}
