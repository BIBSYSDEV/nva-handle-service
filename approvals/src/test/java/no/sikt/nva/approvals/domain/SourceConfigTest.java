package no.sikt.nva.approvals.domain;

import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import nva.commons.core.paths.UriWrapper;
import org.junit.jupiter.api.Test;

class SourceConfigTest {

  private static final URI BASE_URI = URI.create("https://source.example.org/trials");
  private static final URI SOURCE_ON_LOOKALIKE_HOST =
      URI.create("https://source.example.org.attacker.test/trials/1");
  private static final URI SOURCE_ON_LOOKALIKE_PATH =
      URI.create("https://source.example.org/trials-archive/1");
  private static final URI RELATIVE_SOURCE_ON_OTHER_HOST = URI.create("//attacker.test/trials/1");
  private static final URI SOURCE_LEAVING_BASE_PATH =
      URI.create("https://source.example.org/trials/../admin");

  @Test
  void shouldMatchSourceUnderBaseUri() {
    var baseUri = randomUri();
    var source = UriWrapper.fromUri(baseUri).addChild(randomString()).getUri();

    assertTrue(new SourceConfig(baseUri, new NoAuthentication()).matches(source));
  }

  @Test
  void shouldNotMatchSourceOnHostThatOnlyStartsWithBaseHost() {
    assertFalse(
        new SourceConfig(BASE_URI, new NoAuthentication()).matches(SOURCE_ON_LOOKALIKE_HOST));
  }

  @Test
  void shouldNotMatchSourceWhosePathOnlyStartsWithBasePath() {
    assertFalse(
        new SourceConfig(BASE_URI, new NoAuthentication()).matches(SOURCE_ON_LOOKALIKE_PATH));
  }

  @Test
  void shouldNotMatchSourceThatLeavesBasePathThroughDotSegments() {
    assertFalse(
        new SourceConfig(BASE_URI, new NoAuthentication()).matches(SOURCE_LEAVING_BASE_PATH));
  }

  @Test
  void shouldNotMatchRelativeSource() {
    assertFalse(
        new SourceConfig(BASE_URI, new NoAuthentication()).matches(RELATIVE_SOURCE_ON_OTHER_HOST));
  }
}
