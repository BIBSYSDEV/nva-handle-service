package no.sikt.nva.approvals.source;

import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;

import java.util.Map;
import no.unit.nva.commons.json.JsonUtils;
import org.junit.jupiter.api.Test;

class SourceCredentialsTest {

  private static final String SECRET =
      """
      {
        "credentials": {
          "oauth": {
            "type": "OAuth2Credentials",
            "clientId": "id",
            "clientSecret": "secret",
            "accessTokenUrl": "https://example.org/token",
            "scope": "scope"
          },
          "bearer": { "type": "BearerTokenCredentials", "token": "token" }
        }
      }
      """;

  @Test
  void shouldReadOAuth2AndBearerTokenCredentialsFromSecret() throws Exception {
    var sourceCredentials = JsonUtils.dtoObjectMapper.readValue(SECRET, SourceCredentials.class);

    var expected =
        new SourceCredentials(
            Map.of(
                "oauth",
                new OAuth2Credentials("id", "secret", "https://example.org/token", "scope"),
                "bearer",
                new BearerTokenCredentials("token")));
    assertThat(sourceCredentials, equalTo(expected));
  }

  @Test
  void shouldNotExposeBearerTokenInToString() {
    var token = randomString();

    assertThat(new BearerTokenCredentials(token).toString(), not(containsString(token)));
  }
}
