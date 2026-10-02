package no.sikt.nva.approvals.rest;

import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.SequencedCollection;
import java.util.function.Function;
import java.util.stream.Collectors;
import nva.commons.core.ioutils.IoUtils;

final class VersionedDocuments {

  private final Map<String, String> documentsByVersion;
  private final String latestVersion;

  private VersionedDocuments(Map<String, String> documentsByVersion, String latestVersion) {
    this.documentsByVersion = documentsByVersion;
    this.latestVersion = latestVersion;
  }

  static VersionedDocuments load(
      SequencedCollection<String> versions, String fileNameFormat, String apiHost) {
    var documentsByVersion =
        versions.stream()
            .collect(
                Collectors.toUnmodifiableMap(
                    Function.identity(),
                    version -> readDocument(fileNameFormat.formatted(version), apiHost)));
    return new VersionedDocuments(documentsByVersion, versions.getLast());
  }

  String getLatestVersion() {
    return latestVersion;
  }

  Optional<String> find(String version) {
    return Optional.ofNullable(documentsByVersion.get(version));
  }

  private static String readDocument(String fileName, String apiHost) {
    return OntologyIri.resolve(IoUtils.stringFromResources(Path.of(fileName)), apiHost);
  }
}
