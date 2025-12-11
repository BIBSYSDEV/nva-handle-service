package no.sikt.nva.approvals.domain;

import static no.unit.nva.testutils.RandomDataGenerator.randomString;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static org.junit.jupiter.api.Assertions.assertThrows;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ApprovalServiceTest {

    private static final URI VALID_HANDLE_URI = URI.create("https://hdl.handle.net/11250.1/12345");
    public ApprovalService approvalService;

    @BeforeEach
    void setup() {
        this.approvalService = new ApprovalServiceImpl();
    }

    @Test
    void shouldThrowApprovalServiceExceptionOnCreate() {
        assertThrows(ApprovalServiceException.class, () -> approvalService.create(List.of(), randomUri()));
    }

    @Test
    void shouldThrowApprovalServiceExceptionOnGetApprovalByIdentifier() {
        assertThrows(ApprovalServiceException.class, () -> approvalService.getApprovalByIdentifier(UUID.randomUUID()));
    }

    @Test
    void shouldThrowApprovalServiceExceptionOnGetApprovalByHandle() {
        var handle = new Handle(VALID_HANDLE_URI);
        assertThrows(ApprovalServiceException.class, () -> approvalService.getApprovalByHandle(handle));
    }

    @Test
    void shouldThrowApprovalServiceExceptionOnGetApprovalByNamedIdentifier() {
        var namedIdentifier = new NamedIdentifier(randomString(), randomString());
        assertThrows(ApprovalServiceException.class,
            () -> approvalService.getApprovalByNamedIdentifier(namedIdentifier));
    }
}
