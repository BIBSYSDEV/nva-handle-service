package no.sikt.nva.approvals.domain;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import java.io.IOException;

final class JsonLdLiteralDeserializer extends JsonDeserializer<String> {

  private static final String VALUE_KEYWORD = "@value";
  private static final String NOT_A_SINGLE_VALUE_MESSAGE =
      "Expected a single value or a JSON-LD value object, but got: %s";

  @Override
  public String deserialize(JsonParser parser, DeserializationContext context) throws IOException {
    JsonNode node = parser.readValueAsTree();
    var literal = node.has(VALUE_KEYWORD) ? node.get(VALUE_KEYWORD) : node;
    if (!literal.isValueNode()) {
      throw MismatchedInputException.from(
          parser, String.class, NOT_A_SINGLE_VALUE_MESSAGE.formatted(node));
    }
    return literal.asText();
  }
}
