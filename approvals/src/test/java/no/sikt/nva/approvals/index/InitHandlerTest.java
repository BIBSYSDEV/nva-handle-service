package no.sikt.nva.approvals.index;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import no.unit.nva.stubs.FakeContext;
import org.junit.jupiter.api.Test;

class InitHandlerTest {

  @Test
  void shouldCreateIndex() {
    var searchClient = mock(SearchClient.class);

    new InitHandler(searchClient).handleRequest(new Object(), new FakeContext());

    verify(searchClient).createIndex();
  }
}
