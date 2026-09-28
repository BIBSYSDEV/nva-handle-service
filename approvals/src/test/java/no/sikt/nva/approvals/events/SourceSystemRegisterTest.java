package no.sikt.nva.approvals.events;

import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class SourceSystemRegisterTest {

  private static final String REK = "rek";
  private static final URI REK_BASE_URI = URI.create("https://rek.example.com/api/approvals");
  private static final String REGISTER_JSON =
      """
      [
        { "id": "rek", "baseUri": "https://rek.example.com/api/approvals" }
      ]
      """;
  private static final String EMPTY_REGISTER_JSON = "[]";
  private static final List<String> MALFORMED_REGISTER_JSONS =
      List.of(
          "",
          "null",
          "{ \"id\": \"rek\" }",
          "[ { \"baseUri\": \"https://rek.example.com\" } ]",
          "[ { \"id\": \"rek\" } ]",
          "[ { \"id\": \"rek\", \"baseUri\": \"/relative/path\" } ]",
          "[ { \"id\": \" \", \"baseUri\": \"https://rek.example.com\" } ]");
  private static final List<URI> COVERED_SOURCES =
      List.of(
          URI.create("https://rek.example.com/api/approvals/123"),
          URI.create("https://REK.example.com/api/approvals/123"),
          URI.create("https://rek.example.com/api/approvals/nested/123"));
  private static final List<URI> UNCOVERED_SOURCES =
      List.of(
          URI.create("https://rek.example.com/api/approvalsfoo/123"),
          URI.create("https://rek.example.com/api/other/123"),
          URI.create("http://rek.example.com/api/approvals/123"),
          URI.create("https://rek.example.com:8443/api/approvals/123"),
          URI.create("https://rek.example.com.evil.com/api/approvals/123"),
          URI.create("urn:rek:approvals:123"));

  @Test
  void shouldLoadSourceSystemsFromJson() {
    var register = SourceSystemRegister.fromJson(REGISTER_JSON);

    assertEquals(
        Optional.of(new SourceSystem(REK, REK_BASE_URI)),
        register.resolve(COVERED_SOURCES.getFirst()));
  }

  @Test
  void shouldResolveNothingFromEmptyRegister() {
    var register = SourceSystemRegister.fromJson(EMPTY_REGISTER_JSON);

    assertTrue(register.resolve(COVERED_SOURCES.getFirst()).isEmpty());
  }

  @ParameterizedTest
  @MethodSource("malformedRegisterJsons")
  void shouldFailFastWhenRegisterIsMalformed(String json) {
    assertThrows(IllegalStateException.class, () -> SourceSystemRegister.fromJson(json));
  }

  @ParameterizedTest
  @MethodSource("coveredSources")
  void shouldResolveSourceBelowBaseUri(URI source) {
    var register = new SourceSystemRegister(List.of(new SourceSystem(REK, REK_BASE_URI)));

    assertEquals(REK, register.resolve(source).orElseThrow().id());
  }

  @ParameterizedTest
  @MethodSource("uncoveredSources")
  void shouldNotResolveSourceOutsideBaseUri(URI source) {
    var register = new SourceSystemRegister(List.of(new SourceSystem(REK, REK_BASE_URI)));

    assertTrue(register.resolve(source).isEmpty());
  }

  @Test
  void shouldRejectSourceSystemWithoutBaseUri() {
    assertThrows(IllegalArgumentException.class, () -> new SourceSystem(randomString(), null));
  }

  private static Stream<String> malformedRegisterJsons() {
    return MALFORMED_REGISTER_JSONS.stream();
  }

  private static Stream<URI> coveredSources() {
    return COVERED_SOURCES.stream();
  }

  private static Stream<URI> uncoveredSources() {
    return UNCOVERED_SOURCES.stream();
  }
}
