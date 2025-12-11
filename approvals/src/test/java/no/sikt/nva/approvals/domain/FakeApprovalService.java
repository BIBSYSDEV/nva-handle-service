package no.sikt.nva.approvals.domain;

import static java.util.UUID.randomUUID;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import java.net.URI;
import java.util.Collection;
import java.util.UUID;
import no.sikt.nva.approvals.utils.TestUtils;

public class FakeApprovalService implements ApprovalService {

    private static final Approval APPROVAL_ON_CREATE = TestUtils.randomApproval(randomUUID(), randomUri());
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
        return APPROVAL_ON_CREATE;
    }

    @Override
    public Approval getApprovalByIdentifier(UUID approvalId)
        throws ApprovalNotFoundException, ApprovalServiceException {
        if (exception instanceof ApprovalNotFoundException) {
            throw (ApprovalNotFoundException) exception;
        }
        if (exception instanceof ApprovalServiceException) {
            throw (ApprovalServiceException) exception;
        }
        return null;
    }

    public Approval getPersistedApproval() {
        return APPROVAL_ON_CREATE;
    }
}
