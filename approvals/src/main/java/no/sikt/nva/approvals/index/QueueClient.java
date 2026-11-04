package no.sikt.nva.approvals.index;

import java.util.Collection;
import no.unit.nva.commons.json.JsonSerializable;

@FunctionalInterface
public interface QueueClient {

  void send(Collection<? extends JsonSerializable> messages, String queueUrl);
}
