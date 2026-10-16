package no.sikt.nva.approvals.rest;

import static no.sikt.nva.approvals.utils.UriConstants.clientId;
import static no.sikt.nva.approvals.utils.UriConstants.customerId;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.net.URI;
import java.util.Optional;
import java.util.UUID;
import no.sikt.nva.approvals.domain.Agent;

@JsonTypeInfo(use = Id.NAME, property = "type")
@JsonTypeName("SoftwareAgent")
public record SoftwareAgent(URI id, URI actedOnBehalfOf) {

  static SoftwareAgent actingFor(Agent agent, String apiHost) {
    return new SoftwareAgent(
        constructClient(agent.clientIdentifier(), apiHost),
        constructCustomer(agent.customerIdentifier(), apiHost));
  }

  private static URI constructClient(String clientIdentifier, String apiHost) {
    return Optional.ofNullable(clientIdentifier)
        .map(identifier -> clientId(apiHost, identifier))
        .orElse(null);
  }

  private static URI constructCustomer(UUID customerIdentifier, String apiHost) {
    return Optional.ofNullable(customerIdentifier)
        .map(identifier -> customerId(apiHost, identifier))
        .orElse(null);
  }
}
