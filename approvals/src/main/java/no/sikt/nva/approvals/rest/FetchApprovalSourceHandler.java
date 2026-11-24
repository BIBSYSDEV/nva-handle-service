package no.sikt.nva.approvals.rest;

import static java.net.HttpURLConnection.HTTP_NOT_MODIFIED;
import static java.net.HttpURLConnection.HTTP_OK;
import static no.sikt.nva.approvals.utils.RequestUtils.getApprovalIdentifier;
import static no.sikt.nva.approvals.utils.RequestUtils.getChangeIdentifier;
import static no.sikt.nva.approvals.utils.RequestUtils.hasChangeIdentifier;
import static no.sikt.nva.approvals.utils.RequestUtils.toApiGatewayException;
import static nva.commons.apigateway.MediaTypes.APPLICATION_JSON_LD;
import static nva.commons.core.attempt.Try.attempt;

import com.amazonaws.services.lambda.runtime.Context;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import no.sikt.nva.approvals.domain.ChangeService;
import no.sikt.nva.approvals.domain.ChangeServiceImpl;
import no.sikt.nva.approvals.domain.SourceSnapshot;
import no.sikt.nva.approvals.utils.EntityTag;
import no.unit.nva.identifiers.SortableIdentifier;
import nva.commons.apigateway.ApiGatewayHandler;
import nva.commons.apigateway.MediaType;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.NotFoundException;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;
import org.apache.hc.core5.http.HttpHeaders;

public class FetchApprovalSourceHandler extends ApiGatewayHandler<Void, String> {

  private static final String SOURCE_NOT_FOUND_MESSAGE =
      "No source document is recorded for the approval";
  private static final String CHANGE_SOURCE_NOT_FOUND_MESSAGE =
      "No source document is recorded for the change";
  private static final String NOT_MODIFIED_BODY = "";

  private final ChangeService changeService;

  @JacocoGenerated
  public FetchApprovalSourceHandler() {
    this(new Environment());
  }

  @JacocoGenerated
  private FetchApprovalSourceHandler(Environment environment) {
    this(ChangeServiceImpl.defaultInstance(environment), environment);
  }

  public FetchApprovalSourceHandler(ChangeService changeService, Environment environment) {
    super(Void.class, environment);
    this.changeService = changeService;
  }

  @Override
  protected void validateRequest(Void input, RequestInfo requestInfo, Context context)
      throws ApiGatewayException {
    getDefaultResponseContentTypeHeaderValue(requestInfo);
  }

  @Override
  protected String processInput(Void input, RequestInfo requestInfo, Context context)
      throws ApiGatewayException {
    var snapshot = findSnapshot(requestInfo);
    var entityTag = EntityTag.forVersion(snapshot.identifier());
    addAdditionalHeaders(() -> representationHeaders(snapshot, entityTag));
    return EntityTag.isMatchedByIfNoneMatch(requestInfo, entityTag)
        ? NOT_MODIFIED_BODY
        : snapshot.content().body();
  }

  @Override
  protected Integer getSuccessStatusCode(Void input, String output) {
    return NOT_MODIFIED_BODY.equals(output) ? HTTP_NOT_MODIFIED : HTTP_OK;
  }

  @Override
  protected List<MediaType> listSupportedMediaTypes() {
    return List.of(APPLICATION_JSON_LD, MediaType.JSON_UTF_8);
  }

  private SourceSnapshot findSnapshot(RequestInfo requestInfo) throws ApiGatewayException {
    var approvalIdentifier = getApprovalIdentifier(requestInfo);
    return hasChangeIdentifier(requestInfo)
        ? findChangeSnapshot(approvalIdentifier, getChangeIdentifier(requestInfo))
        : findLatestSnapshot(approvalIdentifier);
  }

  private SourceSnapshot findLatestSnapshot(UUID approvalIdentifier) throws ApiGatewayException {
    return resolveSnapshot(
        () -> changeService.findLatestSnapshot(approvalIdentifier), SOURCE_NOT_FOUND_MESSAGE);
  }

  private SourceSnapshot findChangeSnapshot(
      UUID approvalIdentifier, SortableIdentifier changeIdentifier) throws ApiGatewayException {
    return resolveSnapshot(
        () -> changeService.findSnapshot(approvalIdentifier, changeIdentifier),
        CHANGE_SOURCE_NOT_FOUND_MESSAGE);
  }

  private static SourceSnapshot resolveSnapshot(
      Supplier<Optional<SourceSnapshot>> lookup, String notFoundMessage)
      throws ApiGatewayException {
    return attempt(lookup::get)
        .orElseThrow(failure -> toApiGatewayException(failure.getException()))
        .orElseThrow(() -> new NotFoundException(notFoundMessage));
  }

  private static Map<String, String> representationHeaders(
      SourceSnapshot snapshot, String entityTag) {
    return Map.of(HttpHeaders.CONTENT_TYPE, snapshot.content().type(), HttpHeaders.ETAG, entityTag);
  }
}
