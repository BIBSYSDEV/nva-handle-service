package no.sikt.nva.approvals.rest;

import static no.sikt.nva.approvals.utils.UriConstants.customerId;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.net.URI;
import java.util.Optional;
import java.util.UUID;

@JsonTypeInfo(use = Id.NAME, property = "type")
@JsonTypeName("SoftwareAgent")
public record SoftwareAgent(URI id, URI actedOnBehalfOf) {

  static SoftwareAgent actingFor(UUID customerIdentifier, String apiHost) {
    return new SoftwareAgent(null, constructCustomer(customerIdentifier, apiHost));
  }

  private static URI constructCustomer(UUID customerIdentifier, String apiHost) {
    return Optional.ofNullable(customerIdentifier)
        .map(identifier -> customerId(apiHost, identifier))
        .orElse(null);
  }
}
