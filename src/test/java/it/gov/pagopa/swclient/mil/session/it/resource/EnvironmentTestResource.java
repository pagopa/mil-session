package it.gov.pagopa.swclient.mil.session.it.resource;

import com.google.common.collect.ImmutableMap;
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;

import java.util.Map;
import java.util.UUID;

public class EnvironmentTestResource implements QuarkusTestResourceLifecycleManager {

    @Override
    public Map<String, String> start() {
        return ImmutableMap.of(
                "session.quarkus-log-level", "DEBUG",
                "session.app-log-level", "DEBUG",
                // reactive test client timeout
                "mil.tc.service.read-timeout", "3000",
                "mil.tc.service.connect-timeout", "3000",
                "pmwallet.service.read-timeout", "3000",
                "pmwallet.service.connect-timeout", "3000",
                "pmwallet-api.apim-subscription-key", UUID.randomUUID().toString(),
                "mil.apim-subscription-key", UUID.randomUUID().toString()
        );
    }

    @Override
    public void stop() {
    }
}
