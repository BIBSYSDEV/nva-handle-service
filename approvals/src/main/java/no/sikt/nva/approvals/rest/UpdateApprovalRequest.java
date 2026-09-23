package no.sikt.nva.approvals.rest;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static no.sikt.nva.approvals.utils.ValidationUtils.shouldNotBeEmpty;
import static nva.commons.core.attempt.Try.attempt;

import java.net.URI;
import java.util.Collection;
import java.util.UUID;
import no.sikt.nva.approvals.domain.NamedIdentifier;
import nva.commons.apigateway.exceptions.BadRequestException;
import nva.commons.core.paths.UriWrapper;

public record UpdateApprovalRequest(
    URI id, UUID identifier, Collection<NamedIdentifier> identifiers, URI source, URI handle) {

  private static final String IDENTIFIERS_MANDATORY_MESSAGE =
      "At least one identifier is mandatory for approval update";
  private static final String SOURCE_MANDATORY_MESSAGE = "Source is mandatory for approval update";
  private static final String ID_MISMATCH_MESSAGE = "Provided id %s does not address approval %s";
  private static final String IDENTIFIER_MISMATCH_MESSAGE =
      "Provided identifier %s does not match approval %s";

  public UpdateApprovalRequest {
    shouldNotBeEmpty(identifiers, IDENTIFIERS_MANDATORY_MESSAGE);
  }

  public void validate(UUID approvalIdentifier) throws BadRequestException {
    if (isNull(source)) {
      throw new BadRequestException(SOURCE_MANDATORY_MESSAGE);
    }
    if (nonNull(identifier) && !identifier.equals(approvalIdentifier)) {
      throw new BadRequestException(
          IDENTIFIER_MISMATCH_MESSAGE.formatted(identifier, approvalIdentifier));
    }
    if (nonNull(id) && !addressesApproval(approvalIdentifier)) {
      throw new BadRequestException(ID_MISMATCH_MESSAGE.formatted(id, approvalIdentifier));
    }
  }

  private boolean addressesApproval(UUID approvalIdentifier) throws BadRequestException {
    return approvalIdentifier.toString().equals(getIdentifierFromId());
  }

  private String getIdentifierFromId() throws BadRequestException {
    return attempt(() -> UriWrapper.fromUri(id))
        .map(UriWrapper::getLastPathElement)
        .orElseThrow(failure -> new BadRequestException("Provided id is invalid %s".formatted(id)));
  }
}
