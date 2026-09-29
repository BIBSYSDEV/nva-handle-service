package no.sikt.nva.approvals.rest;

import com.amazonaws.services.lambda.runtime.Context;
import java.net.HttpURLConnection;
import java.nio.file.Path;
import java.util.List;
import no.sikt.nva.approvals.utils.RequestUtils;
import nva.commons.apigateway.ApiGatewayHandler;
import nva.commons.apigateway.MediaType;
import nva.commons.apigateway.RequestInfo;
import nva.commons.apigateway.exceptions.ApiGatewayException;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.ioutils.IoUtils;

public class FetchOntologyHandler extends ApiGatewayHandler<Void, String> {

  private static final String ONTOLOGY_TEMPLATE =
      IoUtils.stringFromResources(Path.of("approval-ontology.ttl"));
  private static final MediaType TEXT_TURTLE = MediaType.parse("text/turtle");
  private final String ontology;

  @JacocoGenerated
  public FetchOntologyHandler() {
    this(new Environment());
  }

  public FetchOntologyHandler(Environment environment) {
    super(Void.class, environment);
    this.ontology =
        OntologyNamespace.resolve(ONTOLOGY_TEMPLATE, RequestUtils.getApiHost(environment));
  }

  @Override
  protected String processInput(Void input, RequestInfo requestInfo, Context context)
      throws ApiGatewayException {
    return ontology;
  }

  @Override
  protected Integer getSuccessStatusCode(Void input, String output) {
    return HttpURLConnection.HTTP_OK;
  }

  @Override
  protected List<MediaType> listSupportedMediaTypes() {
    return List.of(TEXT_TURTLE);
  }

  @Override
  protected void validateRequest(Void unused, RequestInfo requestInfo, Context context)
      throws ApiGatewayException {
    // noop
  }
}
