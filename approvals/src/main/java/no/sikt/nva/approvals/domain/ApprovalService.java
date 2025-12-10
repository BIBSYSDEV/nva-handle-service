package no.sikt.nva.approvals.domain;

import java.net.URI;
import java.util.Collection;

public interface ApprovalService {

    void create(Collection<Identifier> identifiers, URI source) throws ApprovalServiceException,
                                                                       ApprovalConflictException;
}
