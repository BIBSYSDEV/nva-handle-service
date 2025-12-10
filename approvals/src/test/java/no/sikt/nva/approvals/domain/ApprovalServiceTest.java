package no.sikt.nva.approvals.domain;

import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static org.junit.jupiter.api.Assertions.assertThrows;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ApprovalServiceTest {

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
}
