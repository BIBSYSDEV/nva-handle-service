package no.sikt.nva.approvals.rest;

import static no.sikt.nva.approvals.validation.RequestConstraints.IDENTIFIERS_SIZE_MESSAGE;
import static no.sikt.nva.approvals.validation.RequestConstraints.MANDATORY_MESSAGE;
import static no.sikt.nva.approvals.validation.RequestConstraints.MAX_IDENTIFIERS;
import static no.sikt.nva.approvals.validation.RequestConstraints.MAX_URI_LENGTH;
import static no.sikt.nva.approvals.validation.RequestConstraints.MIN_IDENTIFIERS;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.net.URI;
import java.util.List;
import no.sikt.nva.approvals.domain.NamedIdentifier;
import no.sikt.nva.approvals.validation.UniqueIdentifiers;
import no.sikt.nva.approvals.validation.UriSize;

public record CreateApprovalRequest(
    @NotNull(message = MANDATORY_MESSAGE)
        @Size(min = MIN_IDENTIFIERS, max = MAX_IDENTIFIERS, message = IDENTIFIERS_SIZE_MESSAGE)
        @UniqueIdentifiers
        List<@NotNull(message = MANDATORY_MESSAGE) @Valid NamedIdentifier> identifiers,
    @NotNull(message = MANDATORY_MESSAGE) @UriSize(max = MAX_URI_LENGTH) URI source) {}
