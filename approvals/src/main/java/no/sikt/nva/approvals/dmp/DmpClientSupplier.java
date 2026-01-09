package no.sikt.nva.approvals.dmp;

import static nva.commons.secrets.SecretsReader.defaultSecretsManagerClient;
import java.util.function.Supplier;
import nva.commons.core.Environment;
import nva.commons.core.JacocoGenerated;
import nva.commons.secrets.SecretsReader;

@JacocoGenerated
public final class DmpClientSupplier {

    public static final String ENV_DMP_CLIENT_SECRET_NAME = "DMP_CLIENT_SECRET_NAME";

    private DmpClientSupplier() {
    }

    public static Supplier<DmpClient> getDmpClientSupplier() {
        return () -> createDmpClient(new Environment(), new SecretsReader(defaultSecretsManagerClient()));
    }

    private static DmpClient createDmpClient(Environment environment, SecretsReader secretsReader) {
        var secrets = secretsReader.fetchClassSecret(
            environment.readEnv(ENV_DMP_CLIENT_SECRET_NAME),
            DmpClientSecrets.class);
        var tokenService = new OAuth2TokenService(secrets);
        return new DmpClient(tokenService, secrets.baseUrl());
    }
}
