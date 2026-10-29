package no.sikt.nva.approvals.index;

import static nva.commons.core.attempt.Try.attempt;

import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.UUID;
import no.unit.nva.commons.json.JsonUtils;

public record IndexDocument(UUID identifier, String title) {

  private static final String PARSING_INDEX_DOCUMENT_MESSAGE =
      "Failed to parse IndexDocument from json";
  private static final String IDENTIFIER = "identifier";

  public static IndexDocument create(String json, UUID identifier) {
    return attempt(() -> JsonUtils.dtoObjectMapper.readValue(json, ObjectNode.class))
        .map(approval -> approval.put(IDENTIFIER, identifier.toString()))
        .map(approval -> JsonUtils.dtoObjectMapper.treeToValue(approval, IndexDocument.class))
        .orElseThrow(failure -> new IllegalArgumentException(PARSING_INDEX_DOCUMENT_MESSAGE));
  }
}
