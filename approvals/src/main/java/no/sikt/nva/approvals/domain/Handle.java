package no.sikt.nva.approvals.domain;

import static java.util.Objects.isNull;
import static nva.commons.core.attempt.Try.attempt;
import java.net.URI;
import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Stream;
import nva.commons.core.JacocoGenerated;
import nva.commons.core.StringUtils;
import nva.commons.core.paths.UriWrapper;

public class Handle {

    public static final String HANDLE_HOST = "www.handle.net";
    public static final String INVALID_HANDLE_EXCEPTION = "Provided uri is not a handle!";
    private final URI value;

    private Handle(URI value) {
        this.value = value;
    }

    public static Handle fromUri(URI uri) {
        validate(uri);
        return new Handle(uri);
    }

    public URI getValue() {
        return value;
    }

    @JacocoGenerated
    @Override
    public int hashCode() {
        return Objects.hashCode(getValue());
    }

    @JacocoGenerated
    @Override
    public boolean equals(Object object) {
        if (!(object instanceof Handle handle)) {
            return false;
        }
        return Objects.equals(getValue(), handle.getValue());
    }

    private static void validate(URI uri) {
        if (isNull(uri) || StringUtils.isBlank(uri.toString())) {
            throw new IllegalArgumentException(INVALID_HANDLE_EXCEPTION);
        }
        if (!HANDLE_HOST.equals(uri.getHost())) {
            throw new IllegalArgumentException(INVALID_HANDLE_EXCEPTION);
        }
        if (!hasPrefixAndSuffix(uri)) {
            throw new IllegalArgumentException(INVALID_HANDLE_EXCEPTION);
        }
    }

    private static boolean hasPrefixAndSuffix(URI uri) {
        return Arrays.stream(uri.getPath().split("/"))
            .filter(StringUtils::isNotBlank)
            .toList().size() == 2;
    }
}
