package no.sikt.nva.approvals.domain;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class OAuth2ClientCredentialsTest {

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {"  "})
  void shouldRejectMissingSecretName(String secretName) {
    assertThrows(IllegalArgumentException.class, () -> new OAuth2ClientCredentials(secretName));
  }
}
