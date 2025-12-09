package no.sikt.nva.approvals.domain;

import static no.unit.nva.testutils.RandomDataGenerator.randomString;
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
        assertThrows(ApprovalServiceException.class, () -> approvalService.create(randomApproval()));
    }

    private static Approval randomApproval() {
        return new Approval(null, randomIdentifiers(), randomUri(), null);
    }

    private static List<Identifier> randomIdentifiers() {
        return List.of(randomIdentifier());
    }

    private static Identifier randomIdentifier() {
        return new Identifier(randomString(), randomString());
    }
}
