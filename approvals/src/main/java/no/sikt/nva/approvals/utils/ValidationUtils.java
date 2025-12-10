package no.sikt.nva.approvals.utils;

import static java.util.Objects.isNull;
import java.util.Collection;
import java.util.Objects;

public final class ValidationUtils {

    private ValidationUtils() {
    }

    public static void shouldNotBeEmpty(Collection<?> collection, String errorMessage) {
        if (isNull(collection) || collection.stream().filter(Objects::nonNull).toList().isEmpty()) {
            throw new IllegalArgumentException(errorMessage);
        }
    }
}
