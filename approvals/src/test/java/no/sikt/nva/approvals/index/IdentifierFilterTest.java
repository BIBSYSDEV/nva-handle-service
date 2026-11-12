package no.sikt.nva.approvals.index;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class IdentifierFilterTest {

  @Test
  void shouldSplitNamespaceFromValue() {
    var filter = IdentifierFilter.fromParameter("REK:123456");

    assertEquals("REK", filter.namespace());
    assertEquals("123456", filter.identifier());
  }

  @Test
  void shouldKeepColonsInValue() {
    var filter = IdentifierFilter.fromParameter("REK:2022:500027:76");

    assertEquals("2022:500027:76", filter.identifier());
  }

  @Test
  void shouldReturnParameterValueOfFilter() {
    assertEquals("REK:123456", IdentifierFilter.fromParameter("REK:123456").value());
  }

  @ParameterizedTest
  @ValueSource(strings = {"123456", ":123456", "REK:", ""})
  void shouldThrowWhenNamespaceOrValueIsMissing(String parameter) {
    assertThrows(IllegalArgumentException.class, () -> IdentifierFilter.fromParameter(parameter));
  }
}
