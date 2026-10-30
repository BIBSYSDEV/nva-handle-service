package no.sikt.nva.approvals.index;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import no.unit.nva.stubs.FakeContext;
import org.junit.jupiter.api.Test;

class DeleteIndexHandlerTest {

  @Test
  void shouldDeleteIndex() {
    var searchClient = mock(SearchClient.class);

    new DeleteIndexHandler(searchClient).handleRequest(new Object(), new FakeContext());

    verify(searchClient).deleteIndex();
  }
}
