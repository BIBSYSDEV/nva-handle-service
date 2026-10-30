package no.sikt.nva.approvals.index;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;

public class InitHandler implements RequestHandler<Object, Void> {

  private final SearchClient searchClient;

  @JacocoGenerated
  public InitHandler() {
    this(SearchClient.defaultInstance(new Environment()));
  }

  public InitHandler(SearchClient searchClient) {
    this.searchClient = searchClient;
  }

  @Override
  public Void handleRequest(Object input, Context context) {
    searchClient.createIndex();
    return null;
  }
}
