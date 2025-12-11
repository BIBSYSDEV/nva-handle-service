package no.sikt.nva.approvals.domain;

import java.net.URI;
import java.util.Collection;
import java.util.UUID;

public class FakeApprovalService implements ApprovalService {

    private final Exception exception;

    public FakeApprovalService() {
        this.exception = null;
    }

    public FakeApprovalService(Exception exception) {
        this.exception = exception;
    }

    @Override
    public void create(Collection<NamedIdentifier> namedIdentifiers, URI source)
        throws ApprovalServiceException, ApprovalConflictException {
        if (exception instanceof ApprovalServiceException) {
            throw (ApprovalServiceException) exception;
        }
        if (exception instanceof ApprovalConflictException) {
            throw (ApprovalConflictException) exception;
        }
    }

    @Override
    public Approval getApprovalByIdentifier(UUID approvalId)
        throws ApprovalNotFoundException, ApprovalServiceException {
        throwExceptionIfConfigured();
        return null;
    }

    @Override
    public Approval getApprovalByHandle(Handle handle)
        throws ApprovalNotFoundException, ApprovalServiceException {
        throwExceptionIfConfigured();
        return null;
    }

    @Override
    public Approval getApprovalByNamedIdentifier(NamedIdentifier namedIdentifier)
        throws ApprovalNotFoundException, ApprovalServiceException {
        throwExceptionIfConfigured();
        return null;
    }

    private void throwExceptionIfConfigured() throws ApprovalNotFoundException, ApprovalServiceException {
        if (exception instanceof ApprovalNotFoundException) {
            throw (ApprovalNotFoundException) exception;
        }
        if (exception instanceof ApprovalServiceException) {
            throw (ApprovalServiceException) exception;
        }
    }
}
