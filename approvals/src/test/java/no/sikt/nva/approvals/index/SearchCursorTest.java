package no.sikt.nva.approvals.index;

import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.opensearch.client.opensearch._types.FieldValue;

class SearchCursorTest {

  @Test
  void shouldRestoreSortValuesFromCursor() {
    var identifier = randomString();
    var sortValues = List.of(FieldValue.of(1.5), FieldValue.of(identifier));

    var restored = SearchCursor.toSortValues(SearchCursor.fromSortValues(sortValues));

    assertEquals(1.5, restored.getFirst().doubleValue());
    assertEquals(identifier, restored.getLast().stringValue());
  }

  @Test
  void shouldThrowWhenCursorIsNotDecodable() {
    assertThrows(IllegalArgumentException.class, () -> SearchCursor.toSortValues("not a cursor"));
  }
}
