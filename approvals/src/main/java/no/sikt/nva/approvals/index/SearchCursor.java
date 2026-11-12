package no.sikt.nva.approvals.index;

import static java.nio.charset.StandardCharsets.UTF_8;
import static nva.commons.core.attempt.Try.attempt;

import java.util.Base64;
import java.util.List;
import no.unit.nva.commons.json.JsonUtils;
import org.opensearch.client.opensearch._types.FieldValue;

/** An opaque encoding of the sort values a page ends on, so paging is not tied to any one field. */
public final class SearchCursor {

  private static final String MALFORMED_CURSOR_EXCEPTION = "Cursor cannot be decoded";

  private SearchCursor() {}

  public static String fromSortValues(List<FieldValue> sortValues) {
    var values = sortValues.stream().map(FieldValue::_get).toList();
    var json = JsonUtils.dtoObjectMapper.valueToTree(values).toString();
    return Base64.getUrlEncoder().withoutPadding().encodeToString(json.getBytes(UTF_8));
  }

  public static List<FieldValue> toSortValues(String cursor) {
    return attempt(() -> decode(cursor))
        .orElseThrow(failure -> new IllegalArgumentException(MALFORMED_CURSOR_EXCEPTION));
  }

  private static List<FieldValue> decode(String cursor) throws Exception {
    var json = new String(Base64.getUrlDecoder().decode(cursor), UTF_8);
    List<Object> values = JsonUtils.dtoObjectMapper.readValue(json, List.class);
    return values.stream().map(SearchCursor::toFieldValue).toList();
  }

  private static FieldValue toFieldValue(Object value) {
    return value instanceof Number number
        ? FieldValue.of(number.doubleValue())
        : FieldValue.of(String.valueOf(value));
  }
}
