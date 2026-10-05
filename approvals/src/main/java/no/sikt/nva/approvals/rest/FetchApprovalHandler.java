package no.sikt.nva.approvals.rest;

import static java.net.HttpURLConnection.HTTP_OK;
import static no.sikt.nva.approvals.utils.RequestUtils.getApiHost;
import static no.sikt.nva.approvals.utils.RequestUtils.getApprovalIdentifier;
import static no.sikt.nva.approvals.validation.RequestConstraints.CONFLICTING_PARAMETERS_MESSAGE;
import static no.sikt.nva.approvals.validation.RequestValidator.badRequest;
import static no.sikt.nva.approvals.validation.RequestValidator.validateQueryParameters;
import static nva.commons.apigateway.MediaTypes.APPLICATION_JSON_LD;
import static nva.commons.core.StringUtils.isNotBlank;

import com.amazonaws.services.lambda.runtime.Context;
import gg.jte.TemplateEngine;
import gg.jte.output.StringOutput;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import no.sikt.nva.approvals.dmp.DmpClientSupplier;
import no.sikt.nva.approvals.dmp.model.ClinicalTrial;
import no.sikt.nva.approvals.domain.Approval;
import no.sikt.nva.approvals.domain.ApprovalService;
import no.sikt.nva.approvals.domain.ApprovalServiceImpl;
import no.sikt.nva.approvals.domain.NamedIdentifier;
import no.sikt.nva.approvals.source.SourceClient;
import no.sikt.nva.approvals.source.SourceClientException;
import nva.commons.apigateway.ApiGatewayHandler;
import nva.commons.apigateway.MediaType;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.BadRequestException;
import nva.commons.apigateway.exceptions.NotFoundException;
import nva.commons.apigateway.exceptions.UnsupportedAcceptHeaderException;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.StringUtils;
import nva.commons.core.paths.UriWrapper;
import org.apache.hc.core5.http.HttpHeaders;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FetchApprovalHandler extends ApiGatewayHandler<Void, Object> {

  private static final Logger logger = LoggerFactory.getLogger(FetchApprovalHandler.class);
  private static final String APPROVAL_ID_PATH_PARAMETER = "approvalId";
  private static final String APPROVAL_NOT_FOUND_MESSAGE = "Approval not found";
  private static final String CLINICAL_TRIAL_PATH = "clinical-trial";
  private static final String TEMPLATE_NAME = "approval.jte";
  private static final String DMP_IDENTIFIER_NAME = "DMP";
  private static final String APPLICATION_DOMAIN_ENV = "APPLICATION_DOMAIN";

  private final ApprovalService approvalService;
  private final String apiHost;
  private final String applicationDomain;
  private final TemplateEngine templateEngine;
  private final SourceClient dmpClient;

  @JacocoGenerated
  public FetchApprovalHandler() {
    this(
        ApprovalServiceImpl.defaultInstance(new Environment()),
        new Environment(),
        createTemplateEngine(),
        DmpClientSupplier.getDmpClientSupplier().get());
  }

  public FetchApprovalHandler(
      ApprovalService approvalService,
      Environment environment,
      TemplateEngine templateEngine,
      SourceClient dmpClient) {
    super(Void.class, environment);
    this.approvalService = approvalService;
    this.apiHost = getApiHost(environment);
    this.applicationDomain = environment.readEnv(APPLICATION_DOMAIN_ENV);
    this.templateEngine = templateEngine;
    this.dmpClient = dmpClient;
  }

  @JacocoGenerated
  private static TemplateEngine createTemplateEngine() {
    return TemplateEngine.createPrecompiled(gg.jte.ContentType.Html);
  }

  @Override
  protected List<MediaType> listSupportedMediaTypes() {
    return List.of(MediaType.HTML_UTF_8, APPLICATION_JSON_LD, MediaType.JSON_UTF_8);
  }

  @Override
  protected void validateRequest(Void input, RequestInfo requestInfo, Context context)
      throws ApiGatewayException {
    ensurePathAndQueryAreNotCombined(requestInfo);
  }

  @Override
  protected Object processInput(Void input, RequestInfo requestInfo, Context context)
      throws ApiGatewayException {
    var approval =
        hasPathParameter(requestInfo)
            ? fetchByApprovalId(requestInfo)
            : fetchByQueryParameters(requestInfo);
    var foundApproval =
        approval.orElseThrow(() -> new NotFoundException(APPROVAL_NOT_FOUND_MESSAGE));

    if (isJsonRequest(requestInfo)) {
      return ApprovalResponse.fromApproval(foundApproval, apiHost);
    }
    return renderHtml(foundApproval);
  }

  @Override
  protected Integer getSuccessStatusCode(Void input, Object output) {
    return HTTP_OK;
  }

  @Override
  protected Map<String, String> getSuccessHeaders(RequestInfo requestInfo)
      throws UnsupportedAcceptHeaderException {
    var headers = super.getSuccessHeaders(requestInfo);
    if (isMissingAcceptHeader(requestInfo)) {
      headers.put(HttpHeaders.CONTENT_TYPE, MediaType.JSON_UTF_8.toString());
    }
    return headers;
  }

  private void ensurePathAndQueryAreNotCombined(RequestInfo requestInfo)
      throws BadRequestException {
    if (hasPathParameter(requestInfo) && hasQueryParameters(requestInfo)) {
      throw badRequest(CONFLICTING_PARAMETERS_MESSAGE, APPROVAL_ID_PATH_PARAMETER);
    }
  }

  private boolean hasPathParameter(RequestInfo requestInfo) {
    var approvalId = requestInfo.getPathParameters().get(APPROVAL_ID_PATH_PARAMETER);
    return isNotBlank(approvalId);
  }

  private static boolean hasQueryParameters(RequestInfo requestInfo) {
    return !ApprovalQuery.fromQueryParameters(requestInfo.getQueryParameters()).isEmpty();
  }

  private Optional<Approval> fetchByApprovalId(RequestInfo requestInfo) throws ApiGatewayException {
    var approvalId = getApprovalIdentifier(requestInfo);
    return fetchApprovalByIdentifier(approvalId);
  }

  private Optional<Approval> fetchByQueryParameters(RequestInfo requestInfo)
      throws ApiGatewayException {
    var query = ApprovalQuery.fromQueryParameters(requestInfo.getQueryParameters());
    validateQueryParameters(query);
    return query.isHandleLookup()
        ? approvalService.getApprovalByHandle(query.toHandle())
        : approvalService.getApprovalByNamedIdentifier(query.toNamedIdentifier());
  }

  private Optional<Approval> fetchApprovalByIdentifier(UUID approvalId) {
    return approvalService.getApprovalByIdentifier(approvalId);
  }

  private boolean isJsonRequest(RequestInfo requestInfo) {
    if (isMissingAcceptHeader(requestInfo)) {
      return true;
    }
    try {
      var mediaType = getDefaultResponseContentTypeHeaderValue(requestInfo).withoutParameters();
      return mediaType.matches(MediaType.JSON_UTF_8.withoutParameters())
          || mediaType.matches(MediaType.create("application", "ld+json"));
    } catch (UnsupportedAcceptHeaderException e) {
      return true;
    }
  }

  private boolean isMissingAcceptHeader(RequestInfo requestInfo) {
    return requestInfo.getHeaderOptional("Accept").filter(StringUtils::isNotBlank).isEmpty();
  }

  private String renderHtml(Approval approval) {
    var model =
        fetchClinicalTrialIfDmpIdentifierPresent(approval)
            .map(
                clinicalTrial ->
                    ApprovalHtmlModel.fromApprovalAndClinicalTrial(
                        approval, clinicalTrial, applicationDomain))
            .orElseGet(() -> ApprovalHtmlModel.fromApproval(approval));
    var output = new StringOutput();
    templateEngine.render(TEMPLATE_NAME, model, output);
    return output.toString();
  }

  private Optional<ClinicalTrial> fetchClinicalTrialIfDmpIdentifierPresent(Approval approval) {
    return getDmpIdentifierValue(approval).flatMap(this::fetchClinicalTrial);
  }

  private Optional<String> getDmpIdentifierValue(Approval approval) {
    return approval.namedIdentifiers().stream()
        .filter(identifier -> DMP_IDENTIFIER_NAME.equals(identifier.name()))
        .map(NamedIdentifier::value)
        .findFirst();
  }

  private Optional<ClinicalTrial> fetchClinicalTrial(String dmpIdentifier) {
    try {
      var uri = createClinicalTrialUri(dmpIdentifier);
      return dmpClient
          .fetch(uri)
          .filter(ClinicalTrial.class::isInstance)
          .map(ClinicalTrial.class::cast);
    } catch (SourceClientException exception) {
      logger.warn(
          "Failed to fetch clinical trial data for identifier: {}", dmpIdentifier, exception);
      return Optional.empty();
    }
  }

  private URI createClinicalTrialUri(String dmpIdentifier) {
    return UriWrapper.fromUri(dmpClient.getBaseUrl())
        .addChild(CLINICAL_TRIAL_PATH)
        .addChild(dmpIdentifier)
        .getUri();
  }
}
