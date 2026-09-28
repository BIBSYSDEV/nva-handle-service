package no.sikt.nva.approvals.rest;

import static nva.commons.apigateway.RequestInfoConstants.BACKEND_SCOPE_AS_DEFINED_IN_IDENTITY_SERVICE;

public final class ApprovalScopes {

  public static final String APPROVAL_UPSERT_SCOPE =
      "https://api.nva.unit.no/scopes/third-party/approval-upsert";
  public static final String BACKEND_SCOPE = BACKEND_SCOPE_AS_DEFINED_IN_IDENTITY_SERVICE;

  private ApprovalScopes() {}
}
