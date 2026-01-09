package no.sikt.nva.approvals.dmp;

import static java.util.Objects.nonNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import no.sikt.nva.approvals.dmp.model.ClinicalTrial;

public class FakeDmpClient implements DmpClientService {

    private final Map<String, ClinicalTrial> clinicalTrials;
    private final DmpClientException exceptionToThrow;

    public FakeDmpClient() {
        this.clinicalTrials = new HashMap<>();
        this.exceptionToThrow = null;
    }

    public FakeDmpClient(Map<String, ClinicalTrial> clinicalTrials) {
        this.clinicalTrials = clinicalTrials;
        this.exceptionToThrow = null;
    }

    public FakeDmpClient(DmpClientException exceptionToThrow) {
        this.clinicalTrials = new HashMap<>();
        this.exceptionToThrow = exceptionToThrow;
    }

    public Optional<ClinicalTrial> getClinicalTrial(String identifier) throws DmpClientException {
        if (nonNull(exceptionToThrow)) {
            throw exceptionToThrow;
        }
        return Optional.ofNullable(clinicalTrials.get(identifier));
    }
}
