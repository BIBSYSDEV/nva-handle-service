package no.sikt.nva.approvals.index;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import java.util.Optional;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;

public class DeleteIndexHandler implements RequestHandler<DeleteIndexInput, Void> {

  private static final String INDEX_NAME_MISMATCH_MESSAGE =
      "Index name %s does not match the indexname";

  private final SearchClient searchClient;
  private final String indexName;

  @JacocoGenerated
  public DeleteIndexHandler() {
    this(new Environment());
  }

  @JacocoGenerated
  private DeleteIndexHandler(Environment environment) {
    this(SearchClient.defaultInstance(environment), environment.readEnv(SearchClient.SEARCH_INDEX));
  }

  public DeleteIndexHandler(SearchClient searchClient, String indexName) {
    this.searchClient = searchClient;
    this.indexName = indexName;
  }

  @Override
  public Void handleRequest(DeleteIndexInput input, Context context) {
    var requestedIndexName = Optional.ofNullable(input).map(DeleteIndexInput::indexName);
    if (requestedIndexName.filter(indexName::equals).isEmpty()) {
      throw new IllegalArgumentException(
          INDEX_NAME_MISMATCH_MESSAGE.formatted(requestedIndexName.orElse(null)));
    }
    searchClient.deleteIndex();
    return null;
  }
}
