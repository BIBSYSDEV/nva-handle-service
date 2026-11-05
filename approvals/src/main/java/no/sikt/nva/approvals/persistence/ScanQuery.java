package no.sikt.nva.approvals.persistence;

public record ScanQuery(String entityType, int pageSize, ScanCursor cursor) {}
