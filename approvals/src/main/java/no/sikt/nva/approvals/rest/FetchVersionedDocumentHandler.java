package no.sikt.nva.approvals.rest;

import com.amazonaws.services.lambda.runtime.Context;
import java.net.HttpURLConnection;
import java.util.Map;
import java.util.Optional;
import java.util.SequencedCollection;
import no.sikt.nva.approvals.utils.RequestUtils;
import nva.commons.apigateway.ApiGatewayHandler;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.apigateway.exceptions.NotFoundException;
import nva.commons.core.Environment;
import nva.commons.core.paths.UriWrapper;

abstract class FetchVersionedDocumentHandler extends ApiGatewayHandler<Void, String> {

  private static final String VERSION_PATH_PARAMETER = "version";
  private static final String APPROVAL_PATH = "approval";
  private static final String CONTENT_LOCATION_HEADER = "Content-Location";
  private static final String VERSION_NOT_FOUND_MESSAGE = "Version not found: %s";

  private final VersionedDocuments documents;
  private final String apiHost;
  private final String documentPath;

  protected FetchVersionedDocumentHandler(
      Environment environment,
      String documentPath,
      SequencedCollection<String> versions,
      String fileNameFormat) {
    super(Void.class, environment);
    this.apiHost = RequestUtils.getApiHost(environment);
    this.documentPath = documentPath;
    this.documents = VersionedDocuments.load(versions, fileNameFormat, apiHost);
  }

  @Override
  protected String processInput(Void input, RequestInfo requestInfo, Context context)
      throws ApiGatewayException {
    var version = extractRequestedVersion(requestInfo);
    var document =
        documents
            .find(version)
            .orElseThrow(() -> new NotFoundException(VERSION_NOT_FOUND_MESSAGE.formatted(version)));
    addAdditionalHeaders(() -> Map.of(CONTENT_LOCATION_HEADER, createVersionUri(version)));
    return document;
  }

  @Override
  protected Integer getSuccessStatusCode(Void input, String output) {
    return HttpURLConnection.HTTP_OK;
  }

  @Override
  @SuppressWarnings("PMD.EmptyMethodInAbstractClassShouldBeAbstract")
  protected void validateRequest(Void unused, RequestInfo requestInfo, Context context)
      throws ApiGatewayException {
    // noop
  }

  private String extractRequestedVersion(RequestInfo requestInfo) {
    return Optional.ofNullable(requestInfo.getPathParameters())
        .map(pathParameters -> pathParameters.get(VERSION_PATH_PARAMETER))
        .orElse(documents.getLatestVersion());
  }

  private String createVersionUri(String version) {
    return UriWrapper.fromHost(apiHost)
        .addChild(APPROVAL_PATH)
        .addChild(documentPath)
        .addChild(version)
        .toString();
  }
}
