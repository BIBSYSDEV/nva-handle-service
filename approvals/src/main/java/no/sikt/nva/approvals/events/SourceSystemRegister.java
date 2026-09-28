package no.sikt.nva.approvals.events;

import static nva.commons.core.attempt.Try.attempt;

import java.net.URI;
import java.util.List;
import java.util.Optional;
import no.unit.nva.commons.json.JsonUtils;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;

/** The source systems NVA accepts approval sources from, configured in {@code SOURCE_SYSTEMS}. */
public class SourceSystemRegister {

  private static final String SOURCE_SYSTEMS = "SOURCE_SYSTEMS";
  private static final String MALFORMED_REGISTER_MESSAGE =
      "Source system register is malformed: %s";
  private final List<SourceSystem> sourceSystems;

  public SourceSystemRegister(List<SourceSystem> sourceSystems) {
    this.sourceSystems = List.copyOf(sourceSystems);
  }

  @JacocoGenerated
  public static SourceSystemRegister defaultInstance(Environment environment) {
    return fromJson(environment.readEnv(SOURCE_SYSTEMS));
  }

  public static SourceSystemRegister fromJson(String json) {
    var sourceSystems =
        attempt(() -> JsonUtils.dtoObjectMapper.readValue(json, SourceSystem[].class))
            .toOptional()
            .map(List::of)
            .orElseThrow(
                () -> new IllegalStateException(MALFORMED_REGISTER_MESSAGE.formatted(json)));
    return new SourceSystemRegister(sourceSystems);
  }

  public Optional<SourceSystem> resolve(URI source) {
    return sourceSystems.stream().filter(sourceSystem -> sourceSystem.covers(source)).findFirst();
  }
}
