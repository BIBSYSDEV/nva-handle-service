package no.sikt.nva.approvals.domain;

import static no.sikt.nva.approvals.utils.TestUtils.randomApproval;
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
    public Approval create(Collection<NamedIdentifier> namedIdentifiers, URI source)
        throws ApprovalServiceException, ApprovalConflictException {
        if (exception instanceof ApprovalServiceException) {
            throw (ApprovalServiceException) exception;
        }
        if (exception instanceof ApprovalConflictException) {
            throw (ApprovalConflictException) exception;
        }
        return randomApproval(namedIdentifiers, UUID.randomUUID());
    }

    @Override
    public Approval getApprovalByIdentifier(UUID approvalId)
        throws ApprovalNotFoundException, ApprovalServiceException {
        throwExceptionIfConfigured();
        return randomApproval(approvalId, URI.create("https://example.com/source"));
    }

    @Override
    public Approval getApprovalByHandle(Handle handle)
        throws ApprovalNotFoundException, ApprovalServiceException {
        throwExceptionIfConfigured();
        return randomApproval(handle);
    }

    @Override
    public Approval getApprovalByNamedIdentifier(NamedIdentifier namedIdentifier)
        throws ApprovalNotFoundException, ApprovalServiceException {
        throwExceptionIfConfigured();
        return randomApproval(namedIdentifier);
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
