package no.sikt.nva.approvals.persistence;

import static nva.commons.core.attempt.Try.attempt;

import no.sikt.nva.approvals.domain.Change;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.identifiers.SortableIdentifier;

public sealed interface ChangeDao extends DatabaseEntry
    permits ApprovalRevisionDao, SourceSnapshotDao {

  String INVALID_CHANGE = "Could not read change item: %s";
  String CHANGES_PARTITION = "Change";

  static ChangeDao fromJson(String json) {
    return attempt(() -> JsonUtils.dtoObjectMapper.readValue(json, ChangeDao.class))
        .orElseThrow(
            failure ->
                new IllegalStateException(INVALID_CHANGE.formatted(json), failure.getException()));
  }

  SortableIdentifier identifier();

  Change toChange();
}
