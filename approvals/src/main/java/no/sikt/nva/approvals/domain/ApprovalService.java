package no.sikt.nva.approvals.domain;

import java.net.URI;
import java.util.Collection;
import java.util.UUID;

public interface ApprovalService {

    Approval create(Collection<NamedIdentifier> namedIdentifiers, URI source) throws ApprovalServiceException,
                                                                                 ApprovalConflictException;

    Approval getApprovalByIdentifier(UUID approvalId) throws ApprovalNotFoundException, ApprovalServiceException;
}
