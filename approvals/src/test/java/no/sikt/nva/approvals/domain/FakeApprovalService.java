package no.sikt.nva.approvals.domain;

import static no.sikt.nva.approvals.utils.TestUtils.randomApproval;
import static no.sikt.nva.approvals.utils.TestUtils.randomHandle;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class FakeApprovalService implements ApprovalService {

    private final List<Approval> approvals;
    private final Exception exception;

    public FakeApprovalService() {
        this(new ArrayList<>());
    }

    public FakeApprovalService(List<Approval> approvals) {
        this.approvals = approvals;
        this.exception = null;
    }

    public FakeApprovalService(Exception exception) {
        this.approvals = new ArrayList<>();
        this.exception = exception;
    }

    @Override
    public Approval create(Collection<NamedIdentifier> namedIdentifiers, URI source)
        throws ApprovalServiceException, ApprovalConflictException {
        throwExceptionIfConfigured();
        var approval = randomApproval(randomHandle());
        approvals.add(approval);
        return approval;
    }

    @Override
    public Optional<Approval> getApprovalByIdentifier(UUID approvalId) {
        return approvals.stream()
                   .filter(approval -> approval.identifier().equals(approvalId))
                   .findFirst();
    }

    @Override
    public Optional<Approval> getApprovalByHandle(Handle handle) {
        return approvals.stream()
                   .filter(approval -> approval.handle().equals(handle))
                   .findFirst();
    }

    @Override
    public Optional<Approval> getApprovalByNamedIdentifier(NamedIdentifier namedIdentifier) {
        return approvals.stream()
                   .filter(approval -> approval.namedIdentifiers().contains(namedIdentifier))
                   .findFirst();
    }

    @Override
    public Approval updateApprovalIdentifiers(UUID approvalId, Collection<NamedIdentifier> identifiers)
        throws ApprovalServiceException, ApprovalConflictException {
        throwExceptionIfConfigured();
        return new Approval(approvalId, identifiers, randomUri(), randomHandle());
    }

    public Approval getPersistedApproval() {
        return approvals.stream().findFirst().orElseThrow();
    }

    private void throwExceptionIfConfigured() throws ApprovalServiceException, ApprovalConflictException {
        if (exception instanceof ApprovalServiceException approvalServiceException) {
            throw approvalServiceException;
        }
        if (exception instanceof ApprovalConflictException approvalConflictException) {
            throw approvalConflictException;
        }
    }
}
