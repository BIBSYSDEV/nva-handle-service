package no.sikt.nva.approvals.dmp;

import java.util.Optional;
import no.sikt.nva.approvals.dmp.model.ClinicalTrial;

public interface DmpClientService {

    Optional<ClinicalTrial> getClinicalTrial(String identifier) throws DmpClientException;
}
