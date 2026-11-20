package no.sikt.nva.approvals.persistence;

import java.util.Optional;
import java.util.UUID;
import no.sikt.nva.approvals.domain.SourceViewUpdate;
import no.sikt.nva.approvals.domain.StoredSourceView;

public interface SourceViewRepository {

  Optional<StoredSourceView> find(UUID approvalIdentifier);

  boolean save(SourceViewUpdate update);
}
