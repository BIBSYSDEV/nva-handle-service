package no.sikt.nva.approvals.domain;

import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static org.junit.jupiter.api.Assertions.assertThrows;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ApprovalServiceTest {

    public ApprovalService approvalService;

    @BeforeEach
    public void setup() {
        this.approvalService = new ApprovalServiceImpl();
    }

    @Test
    void shouldThrowApprovalServiceException() {
        assertThrows(ApprovalServiceException.class, () -> approvalService.create(List.of(), randomUri()));
    }
}
