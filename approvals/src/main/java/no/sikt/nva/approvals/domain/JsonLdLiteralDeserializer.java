package no.sikt.nva.approvals.domain;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;

final class JsonLdLiteralDeserializer extends JsonDeserializer<String> {

  private static final String VALUE_KEYWORD = "@value";

  @Override
  public String deserialize(JsonParser parser, DeserializationContext context) throws IOException {
    JsonNode node = parser.readValueAsTree();
    return node.has(VALUE_KEYWORD) ? node.get(VALUE_KEYWORD).asText() : node.asText();
  }
}
