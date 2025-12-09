package no.sikt.nva.approvals.domain;

import java.util.Objects;
public record Identifier(String source, String value) {

    public Identifier {
        Objects.requireNonNull(source, "source must not be null");
        Objects.requireNonNull(value, "value must not be null");
    }
}
