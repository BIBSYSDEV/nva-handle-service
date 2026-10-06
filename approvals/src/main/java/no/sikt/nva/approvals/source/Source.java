package no.sikt.nva.approvals.source;

import java.net.URI;

public interface Source {

  URI context();

  @SuppressWarnings("PMD.ShortMethodName")
  URI id();
}
