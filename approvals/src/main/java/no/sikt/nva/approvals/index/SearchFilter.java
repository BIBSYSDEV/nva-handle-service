package no.sikt.nva.approvals.index;

public sealed interface SearchFilter permits OrganizationFilter, PersonFilter {

  String value();
}
