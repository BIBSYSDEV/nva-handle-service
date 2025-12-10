package no.sikt.nva.approvals.domain;

import static java.util.Objects.isNull;
import java.net.URI;
import java.util.Arrays;
import nva.commons.core.StringUtils;

public record Handle(URI value) {

    private static final String HANDLE_HOST = "handle.net";
    private static final String INVALID_HANDLE_EXCEPTION = "Provided uri is not a handle!";

    public Handle {
        validate(value);
    }

    private static void validate(URI uri) {
        if (isNull(uri) || StringUtils.isBlank(uri.toString())) {
            throw new IllegalArgumentException(INVALID_HANDLE_EXCEPTION);
        }
        if (!uri.getHost().contains(HANDLE_HOST)) {
            throw new IllegalArgumentException(INVALID_HANDLE_EXCEPTION);
        }
        if (!hasPrefixAndSuffix(uri)) {
            throw new IllegalArgumentException(INVALID_HANDLE_EXCEPTION);
        }
    }

    private static boolean hasPrefixAndSuffix(URI uri) {
        return Arrays.stream(uri.getPath().split("/")).filter(StringUtils::isNotBlank).toList().size() == 2;
    }
}
