package no.sikt.nva.approvals.domain;

import static no.sikt.nva.approvals.utils.TestUtils.randomApproval;
import static no.sikt.nva.approvals.utils.TestUtils.randomHandle;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public class FakeApprovalService implements ApprovalService {

    private final Exception exception;
    private final List<Approval> approvals;

    public FakeApprovalService() {
        this.exception = null;
        this.approvals = new ArrayList<>();
    }

    public FakeApprovalService(Exception exception) {
        this.exception = exception;
        this.approvals = new ArrayList<>();
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
        var approval = randomApproval(randomHandle());
        approvals.add(approval);
        return approval;
    }

    @Override
    public Approval getApprovalByIdentifier(UUID approvalId)
        throws ApprovalNotFoundException, ApprovalServiceException {
        throwExceptionIfConfigured();
        return randomApproval(approvalId, URI.create("https://example.com/source"));
    }

    @Override
    public Approval getApprovalByHandle(Handle handle) throws ApprovalNotFoundException, ApprovalServiceException {
        throwExceptionIfConfigured();
        return randomApproval(handle);
    }

    @Override
    public Approval getApprovalByNamedIdentifier(NamedIdentifier namedIdentifier)
        throws ApprovalNotFoundException, ApprovalServiceException {
        throwExceptionIfConfigured();
        return randomApproval(namedIdentifier);
    }

    @Override
    public Approval updateApprovalIdentifiers(UUID approvalId, Collection<NamedIdentifier> identifiers)
        throws ApprovalServiceException, ApprovalNotFoundException {
        if (exception instanceof ApprovalNotFoundException) {
            throw (ApprovalNotFoundException) exception;
        }
        if (exception instanceof ApprovalServiceException) {
            throw (ApprovalServiceException) exception;
        }
        return new Approval(approvalId, identifiers, randomUri(), randomHandle());
    }

    public Approval getPersistedApproval() {
        return approvals.getFirst();
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
