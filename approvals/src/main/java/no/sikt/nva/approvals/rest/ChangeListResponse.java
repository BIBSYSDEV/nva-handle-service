package no.sikt.nva.approvals.rest;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.List;

@JsonTypeInfo(use = Id.NAME, property = "type")
@JsonTypeName("ChangeList")
public record ChangeListResponse(List<ChangeResponse> changes) {

  public static ChangeListResponse empty() {
    return new ChangeListResponse(List.of());
  }
}
