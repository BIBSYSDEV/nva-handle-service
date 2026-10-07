package no.sikt.nva.approvals.domain;

import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import org.junit.jupiter.api.Test;

class ContentTest {

  private static final String BODY = "hello";
  private static final String SHA_256_OF_BODY =
      "2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824";

  @Test
  void shouldStoreSha256HexOfBodyAsContentHash() {
    var content = Content.create(randomString(), BODY);

    assertThat(content.hash(), equalTo(SHA_256_OF_BODY));
  }
}
