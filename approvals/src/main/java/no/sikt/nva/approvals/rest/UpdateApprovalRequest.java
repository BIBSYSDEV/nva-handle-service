package no.sikt.nva.approvals.rest;

import static java.util.Objects.nonNull;
import static no.sikt.nva.approvals.validation.RequestConstraints.IDENTIFIERS_SIZE_MESSAGE;
import static no.sikt.nva.approvals.validation.RequestConstraints.MANDATORY_MESSAGE;
import static no.sikt.nva.approvals.validation.RequestConstraints.MAX_IDENTIFIERS;
import static no.sikt.nva.approvals.validation.RequestConstraints.MAX_URI_LENGTH;
import static no.sikt.nva.approvals.validation.RequestConstraints.MIN_IDENTIFIERS;
import static no.sikt.nva.approvals.validation.RequestValidator.badRequest;
import static nva.commons.core.attempt.Try.attempt;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.net.URI;
import java.util.Collection;
import java.util.UUID;
import no.sikt.nva.approvals.domain.NamedIdentifier;
import no.sikt.nva.approvals.validation.UniqueIdentifiers;
import no.sikt.nva.approvals.validation.UriSize;
import nva.commons.apigateway.exceptions.BadRequestException;
import nva.commons.core.paths.UriWrapper;

public record UpdateApprovalRequest(
    URI id,
    UUID identifier,
    @NotNull(message = MANDATORY_MESSAGE)
        @Size(min = MIN_IDENTIFIERS, max = MAX_IDENTIFIERS, message = IDENTIFIERS_SIZE_MESSAGE)
        @UniqueIdentifiers
        Collection<@NotNull(message = MANDATORY_MESSAGE) @Valid NamedIdentifier> identifiers,
    @NotNull(message = MANDATORY_MESSAGE) @UriSize(max = MAX_URI_LENGTH) URI source,
    @UriSize(max = MAX_URI_LENGTH) URI handle) {

  private static final String ID_POINTER = "/id";
  private static final String IDENTIFIER_POINTER = "/identifier";
  private static final String ID_MISMATCH_MESSAGE = "Provided id %s does not address approval %s";
  private static final String IDENTIFIER_MISMATCH_MESSAGE =
      "Provided identifier %s does not match approval %s";
  private static final String INVALID_ID_MESSAGE = "Provided id is invalid %s";

  public void ensureAddresses(UUID approvalIdentifier) throws BadRequestException {
    if (nonNull(identifier) && !identifier.equals(approvalIdentifier)) {
      throw badRequest(
          IDENTIFIER_MISMATCH_MESSAGE.formatted(identifier, approvalIdentifier),
          IDENTIFIER_POINTER);
    }
    if (nonNull(id) && !addressesApproval(approvalIdentifier)) {
      throw badRequest(ID_MISMATCH_MESSAGE.formatted(id, approvalIdentifier), ID_POINTER);
    }
  }

  private boolean addressesApproval(UUID approvalIdentifier) throws BadRequestException {
    return approvalIdentifier.toString().equals(getIdentifierFromId());
  }

  private String getIdentifierFromId() throws BadRequestException {
    return attempt(() -> UriWrapper.fromUri(id))
        .map(UriWrapper::getLastPathElement)
        .orElseThrow(failure -> badRequest(INVALID_ID_MESSAGE.formatted(id), ID_POINTER));
  }
}
