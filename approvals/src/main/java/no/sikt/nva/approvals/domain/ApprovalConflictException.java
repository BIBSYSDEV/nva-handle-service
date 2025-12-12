package no.sikt.nva.approvals.domain;

import java.util.Map;
import nva.commons.core.JacocoGenerated;

@JacocoGenerated
public class ApprovalConflictException extends Exception {

    private final Map<String, String> conflictingKeys;

    public ApprovalConflictException(String message, Map<String, String> conflictingKeys) {
        super(message);
        this.conflictingKeys = conflictingKeys;
    }

    public Map<String, String> getConflictingKeys() {
        return conflictingKeys;
    }
}
