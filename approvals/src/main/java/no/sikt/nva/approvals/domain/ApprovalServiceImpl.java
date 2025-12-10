package no.sikt.nva.approvals.domain;

import java.net.URI;
import java.util.Collection;

public class ApprovalServiceImpl implements ApprovalService {

    @Override
    public void create(Collection<NamedIdentifier> namedIdentifiers, URI source) throws ApprovalServiceException {
        throw new ApprovalServiceException("Service not implemented");
    }
}
