package no.sikt.nva.approvals.index;

public sealed interface SearchFilter permits PersonFilter {

  String value();
}
