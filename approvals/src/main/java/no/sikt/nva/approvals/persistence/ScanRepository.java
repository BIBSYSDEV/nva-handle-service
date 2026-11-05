package no.sikt.nva.approvals.persistence;

@FunctionalInterface
public interface ScanRepository {

  ItemKeyPage scan(ScanQuery query);
}
