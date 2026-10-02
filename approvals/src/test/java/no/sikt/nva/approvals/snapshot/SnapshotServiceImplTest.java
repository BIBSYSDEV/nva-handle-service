package no.sikt.nva.approvals.snapshot;

import static no.sikt.nva.approvals.utils.TestUtils.randomSourceChange;
import static org.mockito.ArgumentMatchers.refEq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import no.sikt.nva.approvals.persistence.ApprovalRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SnapshotServiceImplTest {

  private ApprovalRepository approvalRepository;
  private SnapshotService snapshotService;

  @BeforeEach
  void setUp() {
    approvalRepository = mock(ApprovalRepository.class);
    snapshotService = new SnapshotServiceImpl(approvalRepository);
  }

  @Test
  void shouldCreateSnapshotOfSourceChange() {
    var sourceChange = randomSourceChange();

    snapshotService.createSnapshot(sourceChange);

    var expected = SourceSnapshot.create(sourceChange);
    verify(approvalRepository).save(refEq(expected, "timestamp"));
  }
}
