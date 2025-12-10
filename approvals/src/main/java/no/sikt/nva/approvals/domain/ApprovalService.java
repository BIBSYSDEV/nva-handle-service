package no.sikt.nva.approvals.domain;

import java.net.URI;
import java.util.Collection;

public interface ApprovalService {

    Approval create(Collection<NamedIdentifier> namedIdentifiers, URI source) throws ApprovalServiceException,
                                                                                 ApprovalConflictException;
}
