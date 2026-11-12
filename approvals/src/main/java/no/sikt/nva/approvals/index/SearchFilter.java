package no.sikt.nva.approvals.index;

public sealed interface SearchFilter
    permits HandleFilter, IdentifierFilter, OrganizationFilter, PersonFilter {

  String value();
}
