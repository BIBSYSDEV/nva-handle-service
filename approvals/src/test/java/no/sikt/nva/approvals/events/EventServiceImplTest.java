package no.sikt.nva.approvals.events;

import static java.util.UUID.randomUUID;
import static no.sikt.nva.approvals.utils.TestUtils.randomApproval;
import static no.sikt.nva.approvals.utils.TestUtils.randomHandle;
import static no.unit.nva.testutils.RandomDataGenerator.randomUri;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import no.sikt.nva.approvals.domain.ApprovalNotFoundException;
import no.sikt.nva.approvals.domain.ApprovalServiceException;
import no.sikt.nva.approvals.domain.CustomerMismatchException;
import no.sikt.nva.approvals.domain.SourceMismatchException;
import no.sikt.nva.approvals.persistence.ApprovalRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class EventServiceImplTest {

  private ApprovalRepository approvalRepository;
  private EventService eventService;

  @BeforeEach
  void setUp() {
    approvalRepository = mock(ApprovalRepository.class);
    eventService = new EventServiceImpl(approvalRepository);
  }

  @Test
  void shouldPersistEventWhenApprovalExistsForHandle() throws ApprovalServiceException {
    var customerIdentifier = randomUUID();
    var source = randomUri();
    var event = randomEvent(customerIdentifier, source);
    when(approvalRepository.findByHandle(event.handle()))
        .thenReturn(Optional.of(randomApproval(source, event.handle(), customerIdentifier)));

    eventService.receive(event);

    verify(approvalRepository).save(event);
  }

  @Test
  void shouldThrowNotFoundAndNotPersistEventWhenApprovalDoesNotExistForHandle() {
    var event = randomEvent(randomUUID(), randomUri());
    when(approvalRepository.findByHandle(event.handle())).thenReturn(Optional.empty());

    assertThrows(ApprovalNotFoundException.class, () -> eventService.receive(event));
    verify(approvalRepository, never()).save((SourceChangedEvent) any());
  }

  @Test
  void shouldThrowCustomerMismatchExceptionAndNotPersistEventWhenCustomerDoesNotOwnApproval() {
    var customerIdentifier = randomUUID();
    var event = randomEvent(customerIdentifier, randomUri());

    when(approvalRepository.findByHandle(event.handle()))
        .thenReturn(Optional.of(randomApproval(event.source(), event.handle(), randomUUID())));

    assertThrows(CustomerMismatchException.class, () -> eventService.receive(event));
  }

  @Test
  void
      shouldThrowApprovalServiceExceptionAndNotPersistEventWhenSourceDifferFromSourceAssignedToApproval() {
    var customerIdentifier = randomUUID();
    var event = randomEvent(customerIdentifier, randomUri());

    when(approvalRepository.findByHandle(event.handle()))
        .thenReturn(Optional.of(randomApproval(randomUri(), event.handle(), customerIdentifier)));

    assertThrows(SourceMismatchException.class, () -> eventService.receive(event));
  }

  private static SourceChangedEvent randomEvent(UUID customerIdentifier, URI source) {
    return new SourceChangedEvent(
        randomUUID().toString(), source, randomHandle(), Instant.now(), customerIdentifier);
  }
}
