package no.sikt.nva.approvals.migration;

import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import no.sikt.nva.approvals.persistence.ApprovalSourceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ApprovalSourceMigrationServiceTest {

  private static final URI DOUBLED_SOURCE =
      URI.create("https://api.example.org/ctis/ctis/trial/2024-512733-32-00");
  private static final URI REPAIRED_SOURCE =
      URI.create("https://api.example.org/ctis/trial/2024-512733-32-00");
  private static final URI UNAFFECTED_SOURCE =
      URI.create("https://api.example.org/ctis/trial/2024-000000-00-00");

  private ApprovalSourceRepository approvalRepository;
  private ApprovalSourceMigrationService migrationService;
  private Map<UUID, URI> sources;

  @BeforeEach
  void setUp() {
    approvalRepository = mock(ApprovalSourceRepository.class);
    migrationService = new ApprovalSourceMigrationService(approvalRepository);
    sources = new LinkedHashMap<>();
  }

  @Test
  void shouldReplaceDoubledSegmentWithSingleSegment() {
    var approvalIdentifier = approvalWithSource(DOUBLED_SOURCE);

    migrationService.migrateApprovals(List.of(approvalIdentifier));

    verify(approvalRepository).replaceSource(approvalIdentifier, DOUBLED_SOURCE, REPAIRED_SOURCE);
  }

  @Test
  void shouldNotWriteWhenSourceHasSingleSegment() {
    var approvalIdentifier = approvalWithSource(UNAFFECTED_SOURCE);

    migrationService.migrateApprovals(List.of(approvalIdentifier));

    verify(approvalRepository).findSources(List.of(approvalIdentifier));
    verifyNoMoreInteractions(approvalRepository);
  }

  @Test
  void shouldNotCollapseUnexpctedPath() {
    var source = URI.create("https://api.example.org/myctis/ctis/trial/1");
    var approvalIdentifier = approvalWithSource(source);

    assertEquals(0, migrationService.migrateApprovals(List.of(approvalIdentifier)));
  }

  @Test
  void shouldReturnNumberOfRepairedApprovals() {
    var approvalIdentifier = approvalWithSource(DOUBLED_SOURCE);
    when(approvalRepository.replaceSource(approvalIdentifier, DOUBLED_SOURCE, REPAIRED_SOURCE))
        .thenReturn(true);

    assertEquals(1, migrationService.migrateApprovals(List.of(approvalIdentifier)));
  }

  private UUID approvalWithSource(URI source) {
    var approvalIdentifier = randomUUID();
    sources.put(approvalIdentifier, source);
    when(approvalRepository.findSources(List.copyOf(sources.keySet()))).thenReturn(sources);
    return approvalIdentifier;
  }
}
