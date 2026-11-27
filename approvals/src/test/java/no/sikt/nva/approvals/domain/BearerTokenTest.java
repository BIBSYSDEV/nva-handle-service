package no.sikt.nva.approvals.domain;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class BearerTokenTest {

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {"  "})
  void shouldRejectMissingKey(String key) {
    assertThrows(IllegalArgumentException.class, () -> new BearerToken(key));
  }
}
