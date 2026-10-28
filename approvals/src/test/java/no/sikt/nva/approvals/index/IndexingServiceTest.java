package no.sikt.nva.approvals.index;

import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class IndexingServiceTest {

  private IndexingService indexingService;

  @BeforeEach
  void setUp() {
    indexingService = new IndexingService();
  }

  @Test
  void shouldThrowUnsupportedOperationExceptionWhenIndexingApproval() {
    assertThrows(UnsupportedOperationException.class, () -> indexingService.index(randomUUID()));
  }
}
