package no.sikt.nva.approvals.index;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;

public class DeleteIndexHandler implements RequestHandler<Object, Void> {

  private final SearchClient searchClient;

  @JacocoGenerated
  public DeleteIndexHandler() {
    this(SearchClient.defaultInstance(new Environment()));
  }

  public DeleteIndexHandler(SearchClient searchClient) {
    this.searchClient = searchClient;
  }

  @Override
  public Void handleRequest(Object input, Context context) {
    searchClient.deleteIndex();
    return null;
  }
}
