package no.sikt.nva.approvals.batch;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.ioutils.IoUtils;

public class StartBatchJobHandler implements RequestStreamHandler {

  private final BatchJobService batchJobService;

  @JacocoGenerated
  public StartBatchJobHandler() {
    this(BatchJobService.defaultInstance(new Environment()));
  }

  public StartBatchJobHandler(BatchJobService batchJobService) {
    this.batchJobService = batchJobService;
  }

  @Override
  public void handleRequest(InputStream input, OutputStream output, Context context)
      throws IOException {
    var message = BatchJobEvent.toMessage(IoUtils.streamToString(input));
    var result = batchJobService.execute(message).toJsonString();
    output.write(result.getBytes(StandardCharsets.UTF_8));
  }
}
