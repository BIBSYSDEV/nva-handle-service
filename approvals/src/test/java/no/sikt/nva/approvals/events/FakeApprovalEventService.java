package no.sikt.nva.approvals.events;

import java.util.ArrayList;
import java.util.List;
import no.sikt.nva.approvals.domain.ApprovalNotFoundException;

public class FakeApprovalEventService implements EventService {

  private final List<SourceChangedEvent> receivedEvents = new ArrayList<>();
  private final Exception exception;

  public FakeApprovalEventService() {
    this.exception = null;
  }

  public FakeApprovalEventService(Exception exception) {
    this.exception = exception;
  }

  @Override
  public void receive(SourceChangedEvent event) throws ApprovalNotFoundException {
    throwExceptionIfConfigured();
    receivedEvents.add(event);
  }

  public List<SourceChangedEvent> getReceivedEvents() {
    return receivedEvents;
  }

  private void throwExceptionIfConfigured() throws ApprovalNotFoundException {
    if (exception instanceof RuntimeException runtimeException) {
      throw runtimeException;
    }
    if (exception instanceof ApprovalNotFoundException notFoundException) {
      throw notFoundException;
    }
  }
}
