package no.sikt.nva.approvals.index;

import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import no.unit.nva.stubs.FakeContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class DeleteIndexHandlerTest {

  private static final String INDEX_NAME = "approvals";

  private SearchClient searchClient;
  private DeleteIndexHandler handler;

  @BeforeEach
  void setUp() {
    searchClient = mock(SearchClient.class);
    handler = new DeleteIndexHandler(searchClient, INDEX_NAME);
  }

  @Test
  void shouldDeleteIndexWhenInputMatchesIndexName() {
    handler.handleRequest(new DeleteIndexInput(INDEX_NAME), new FakeContext());

    verify(searchClient).deleteIndex();
  }

  @Test
  void shouldThrowExceptionWhenInputDoesNotMatchIndexName() {
    var input = new DeleteIndexInput(randomString());

    assertThrows(
        IllegalArgumentException.class, () -> handler.handleRequest(input, new FakeContext()));
    verify(searchClient, never()).deleteIndex();
  }

  @Test
  void shouldThrowExceptionWhenInputIsMissing() {
    assertThrows(
        IllegalArgumentException.class, () -> handler.handleRequest(null, new FakeContext()));
    verify(searchClient, never()).deleteIndex();
  }
}
