package no.sikt.nva.approvals.index;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class QueryTest {

  @Test
  void shouldReduceUriToIdentifier() {
    var query = new Query("https://example.com/cristin/person/123456");

    assertEquals("123456", query.searchTerm());
  }

  @Test
  void shouldReduceOnlyUriTokensWhenQueryMixesUriAndText() {
    var query = new Query("Ola Nordmann https://example.com/cristin/person/123456");

    assertEquals("Ola Nordmann 123456", query.searchTerm());
  }

  @Test
  void shouldKeepHandleIdentifierUnchanged() {
    var query = new Query("11250.1/12345");

    assertEquals("11250.1/12345", query.searchTerm());
  }

  @Test
  void shouldKeepPlainTextUnchanged() {
    var query = new Query("Ola Nordmann");

    assertEquals("Ola Nordmann", query.searchTerm());
  }
}
