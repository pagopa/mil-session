package it.gov.pagopa.swclient.mil.session.it.resource;

import com.google.common.collect.ImmutableMap;
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;

import java.util.Map;

public class EnvironmentTestResource implements QuarkusTestResourceLifecycleManager {

    @Override
    public Map<String, String> start() {
        return ImmutableMap.of(
                "session.quarkus-log-level", "DEBUG",
                "session.app-log-level", "DEBUG"
        );
    }

    @Override
    public void stop() {
    }
}
