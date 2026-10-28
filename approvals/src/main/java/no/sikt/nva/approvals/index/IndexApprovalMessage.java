package no.sikt.nva.approvals.index;

import static nva.commons.core.attempt.Try.attempt;

import java.util.UUID;
import no.unit.nva.commons.json.JsonUtils;

public record IndexApprovalMessage(UUID approvalIdentifier) {

  public static IndexApprovalMessage fromString(String value) {
    return attempt(() -> JsonUtils.dtoObjectMapper.readValue(value, IndexApprovalMessage.class))
        .orElseThrow(
            _ -> new IllegalArgumentException("Could not parse IndexApprovalMessage"));
  }
}
