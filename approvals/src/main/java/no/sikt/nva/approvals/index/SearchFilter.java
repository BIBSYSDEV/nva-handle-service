package no.sikt.nva.approvals.index;

public sealed interface SearchFilter permits IdentifierFilter, OrganizationFilter, PersonFilter {

  String value();
}
