package no.sikt.nva.approvals.persistence;

import java.time.Instant;

public record OrderRecord(
    String customerId, String orderId, String status, Instant createdAt, Long version)  {

  public static OrderRecord from(String customerId, String orderId) {
    return builder().customerId(customerId).orderId(orderId).build();
  }

  public static Builder builder() {
    return new Builder();
  }

  public Builder copy() {
    return new Builder()
        .customerId(customerId)
        .orderId(orderId)
        .status(status)
        .createdAt(createdAt)
        .version(version);
  }

  public static class Builder {
    private String customerId;
    private String orderId;
    private String status;
    private Instant createdAt;
    private Long version;

    public Builder customerId(String v) {
      this.customerId = v;
      return this;
    }

    public Builder orderId(String v) {
      this.orderId = v;
      return this;
    }

    public Builder status(String v) {
      this.status = v;
      return this;
    }

    public Builder createdAt(Instant v) {
      this.createdAt = v;
      return this;
    }

    public Builder version(Long v) {
      this.version = v;
      return this;
    }

    public OrderRecord build() {
      return new OrderRecord(customerId, orderId, status, createdAt, version);
    }
  }
}
