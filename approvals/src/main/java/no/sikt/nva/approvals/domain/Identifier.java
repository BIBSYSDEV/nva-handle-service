package no.sikt.nva.approvals.domain;

import java.util.Objects;
public record Identifier(String type, String value) {

    public Identifier {
        Objects.requireNonNull(type, "type must not be null");
        Objects.requireNonNull(value, "value must not be null");
    }
}
