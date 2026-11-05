package no.sikt.nva.approvals.migration;

import java.net.URI;
import java.util.Collection;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.stream.Stream;
import no.sikt.nva.approvals.persistence.ApprovalSourceRepository;
import no.sikt.nva.approvals.persistence.DynamoDbApprovalSourceRepository;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.paths.UriWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ApprovalSourceMigrationService {

  private static final String REPEATED_SEGMENTS = "/ctis/ctis";
  private static final String SINGLE_SEGMENT = "/ctis";
  private static final String REPLACED_MESSAGE = "Replaced source of approval {}";
  private static final Logger LOGGER =
      LoggerFactory.getLogger(ApprovalSourceMigrationService.class);

  private final ApprovalSourceRepository approvalSourceRepository;

  public ApprovalSourceMigrationService(ApprovalSourceRepository approvalSourceRepository) {
    this.approvalSourceRepository = approvalSourceRepository;
  }

  @JacocoGenerated
  public static ApprovalSourceMigrationService defaultInstance(Environment environment) {
    return new ApprovalSourceMigrationService(
        DynamoDbApprovalSourceRepository.defaultInstance(environment));
  }

  public int migrateApprovals(Collection<UUID> approvalIdentifiers) {
    return (int) affected(approvalIdentifiers).filter(this::replaceSource).count();
  }

  private Stream<Entry<UUID, URI>> affected(Collection<UUID> approvalIdentifiers) {
    return approvalSourceRepository.findSources(approvalIdentifiers).entrySet().stream()
        .filter(approval -> hasRepeatedSegment(approval.getValue()));
  }

  private boolean replaceSource(Entry<UUID, URI> approval) {
    var source = approval.getValue();
    var replaced =
        approvalSourceRepository.replaceSource(approval.getKey(), source, repairSource(source));
    if (replaced) {
      LOGGER.info(REPLACED_MESSAGE, approval.getKey());
    }
    return replaced;
  }

  private static boolean hasRepeatedSegment(URI source) {
    return !repairSource(source).equals(source);
  }

  private static URI repairSource(URI source) {
    var path = source.getRawPath();
    return UriWrapper.fromHost(source.getHost())
        .addChild(path.replace(REPEATED_SEGMENTS, SINGLE_SEGMENT))
        .getUri();
  }
}
