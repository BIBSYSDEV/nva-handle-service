package no.sikt.nva.approvals.domain;

import java.net.URI;
import java.util.Collection;
import java.util.UUID;

public class ApprovalServiceImpl implements ApprovalService {

    @Override
    public void create(Collection<NamedIdentifier> namedIdentifiers, URI source) throws ApprovalServiceException {
        throw new ApprovalServiceException("Service not implemented");
    }

    @Override
    public Approval getApprovalByIdentifier(UUID approvalId) throws ApprovalServiceException {
        throw new ApprovalServiceException("Service not implemented");
    }

    @Override
    public Approval getApprovalByHandle(Handle handle) throws ApprovalServiceException {
        throw new ApprovalServiceException("Service not implemented");
    }

    @Override
    public Approval getApprovalByNamedIdentifier(NamedIdentifier namedIdentifier) throws ApprovalServiceException {
        throw new ApprovalServiceException("Service not implemented");
    }
}
