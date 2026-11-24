package no.sikt.nva.approvals.index;

import static no.sikt.nva.approvals.index.ClinicalTrialFramer.isMissingContent;
import static nva.commons.core.attempt.Try.attempt;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.net.URI;
import java.util.Map.Entry;
import java.util.function.Function;
import no.sikt.nva.approvals.domain.SourceSnapshot;
import no.sikt.nva.approvals.domain.SourceView;
import no.unit.nva.commons.json.JsonUtils;
import no.unit.nva.identifiers.OrganizationIdentifier;
import no.unit.nva.identifiers.PersonIdentifier;
import no.unit.nva.identifiers.UriIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SourceViewGenerator {

  public static final int CURRENT_VIEW_VERSION = 1;

  private static final String EMPTY_CONTENT_MESSAGE = "Snapshot content is empty";
  private static final String FRAMING_FAILED_MESSAGE =
      "Could not create source view for approval %s";
  private static final String CLINICAL_TRIAL_VIEW_FRAME_FILE =
      "index/clinical-trial-view-frame.json";
  private static final ClinicalTrialFramer FRAMER =
      new ClinicalTrialFramer(CLINICAL_TRIAL_VIEW_FRAME_FILE);
  private static final String STATUSES_FIELD = "statuses";
  private static final String REGION_FIELD = "region";
  private static final String STATUS_FIELD = "status";
  private static final String NOT_STATUS_PER_REGION_MESSAGE =
      "Expected statuses to be one status per region, but got: %s";
  private static final String TRIAL_SITES_FIELD = "trialSites";
  private static final String ORGANIZATION_FIELD = "organization";
  private static final String INVESTIGATOR_FIELD = "investigator";
  private static final String PERSON_FIELD = "person";
  private static final String ID_FIELD = "id";
  private static final String IDENTIFIER_FIELD = "identifier";
  private static final String NOT_A_CRISTIN_AGENT_MESSAGE =
      "Leaving out {} of a trial site, {} is not a Cristin URI";
  private static final Logger LOGGER = LoggerFactory.getLogger(SourceViewGenerator.class);

  public SourceView generate(SourceSnapshot snapshot) throws SourceViewGeneratorException {
    if (isMissingContent(snapshot)) {
      throw new SourceViewGeneratorException(EMPTY_CONTENT_MESSAGE);
    }
    return attempt(() -> FRAMER.frame(snapshot.content().body()))
        .map(framed -> (ObjectNode) JsonUtils.dtoObjectMapper.readTree(framed.toString()))
        .map(SourceViewGenerator::withStatusesAsList)
        .map(SourceViewGenerator::withAgentIdentifiers)
        .map(view -> JsonUtils.dtoObjectMapper.treeToValue(view, SourceView.class))
        .orElseThrow(
            failure ->
                new SourceViewGeneratorException(
                    FRAMING_FAILED_MESSAGE.formatted(snapshot.approvalIdentifier()),
                    failure.getException()));
  }

  private static ObjectNode withStatusesAsList(ObjectNode view) {
    var statuses = view.path(STATUSES_FIELD);
    if (statuses instanceof ObjectNode statusPerRegion) {
      view.set(STATUSES_FIELD, toStatusList(statusPerRegion));
    } else if (!statuses.isMissingNode() && !statuses.isNull()) {
      throw new IllegalArgumentException(NOT_STATUS_PER_REGION_MESSAGE.formatted(statuses));
    }
    return view;
  }

  private static ArrayNode toStatusList(ObjectNode statusPerRegion) {
    var statusList = JsonUtils.dtoObjectMapper.createArrayNode();
    statusPerRegion.properties().stream()
        .map(SourceViewGenerator::toStatus)
        .forEach(statusList::add);
    return statusList;
  }

  private static ObjectNode toStatus(Entry<String, JsonNode> regionAndStatus) {
    var status = JsonUtils.dtoObjectMapper.createObjectNode();
    status.put(REGION_FIELD, regionAndStatus.getKey());
    status.set(STATUS_FIELD, regionAndStatus.getValue());
    return status;
  }

  private static ObjectNode withAgentIdentifiers(ObjectNode view) {
    view.path(TRIAL_SITES_FIELD).forEach(SourceViewGenerator::identifyAgentsOfTrialSite);
    return view;
  }

  private static void identifyAgentsOfTrialSite(JsonNode trialSite) {
    identifyAgent(trialSite, ORGANIZATION_FIELD, OrganizationIdentifier::fromUri);
    identifyAgent(trialSite.path(INVESTIGATOR_FIELD), PERSON_FIELD, PersonIdentifier::fromUri);
  }

  private static void identifyAgent(
      JsonNode owner, String agentField, Function<URI, ? extends UriIdentifier<?>> toIdentifier) {
    if (owner instanceof ObjectNode ownerNode
        && ownerNode.path(agentField) instanceof ObjectNode agent) {
      attempt(() -> toIdentifier.apply(URI.create(agent.path(ID_FIELD).asText())))
          .toOptional()
          .ifPresentOrElse(
              identifier -> replaceIdWithIdentifier(agent, identifier),
              () -> leaveOutAgent(ownerNode, agentField));
    }
  }

  private static void replaceIdWithIdentifier(ObjectNode agent, UriIdentifier<?> identifier) {
    agent.remove(ID_FIELD);
    agent.put(IDENTIFIER_FIELD, identifier.value().toString());
  }

  private static void leaveOutAgent(ObjectNode owner, String agentField) {
    var agent = owner.remove(agentField);
    LOGGER.warn(NOT_A_CRISTIN_AGENT_MESSAGE, agentField, agent.path(ID_FIELD).asText());
  }
}
