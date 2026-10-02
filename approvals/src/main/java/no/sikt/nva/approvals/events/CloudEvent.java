package no.sikt.nva.approvals.events;

import static no.sikt.nva.approvals.validation.RequestConstraints.EVENT_ID_PATTERN;
import static no.sikt.nva.approvals.validation.RequestConstraints.EVENT_TYPE_PATTERN;
import static no.sikt.nva.approvals.validation.RequestConstraints.INVALID_EVENT_ID_MESSAGE;
import static no.sikt.nva.approvals.validation.RequestConstraints.MANDATORY_MESSAGE;
import static no.sikt.nva.approvals.validation.RequestConstraints.MAX_URI_LENGTH;
import static no.sikt.nva.approvals.validation.RequestConstraints.SPEC_VERSION_PATTERN;
import static no.sikt.nva.approvals.validation.RequestConstraints.UNSUPPORTED_EVENT_TYPE_MESSAGE;
import static no.sikt.nva.approvals.validation.RequestConstraints.UNSUPPORTED_SPEC_VERSION_MESSAGE;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.net.URI;
import java.time.Instant;
import java.util.UUID;
import no.sikt.nva.approvals.domain.Handle;
import no.sikt.nva.approvals.validation.HandleUri;
import no.sikt.nva.approvals.validation.UriSize;

public record CloudEvent(
    @NotNull(message = MANDATORY_MESSAGE)
        @Pattern(regexp = SPEC_VERSION_PATTERN, message = UNSUPPORTED_SPEC_VERSION_MESSAGE)
        String specversion,
    @NotNull(message = MANDATORY_MESSAGE)
        @Pattern(regexp = EVENT_ID_PATTERN, message = INVALID_EVENT_ID_MESSAGE)
        String id,
    @NotNull(message = MANDATORY_MESSAGE) @UriSize(max = MAX_URI_LENGTH) URI source,
    @NotNull(message = MANDATORY_MESSAGE)
        @Pattern(regexp = EVENT_TYPE_PATTERN, message = UNSUPPORTED_EVENT_TYPE_MESSAGE)
        String type,
    @NotNull(message = MANDATORY_MESSAGE) @HandleUri URI subject,
    @NotNull(message = MANDATORY_MESSAGE) Instant time) {

  public Handle handle() {
    return new Handle(subject);
  }

  public SourceChangedEvent toSourceChangedEvent(UUID customerIdentifier) {
    return new SourceChangedEvent(id, source, handle(), time, customerIdentifier);
  }
}
