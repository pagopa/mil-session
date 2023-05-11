package it.pagopa.swclient.mil.session.it;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import io.quarkus.test.junit.QuarkusTestProfile;
import it.pagopa.swclient.mil.session.it.resource.RedisTestResource;
import it.pagopa.swclient.mil.session.it.resource.WiremockTestResource;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public class IntegrationTestProfile implements QuarkusTestProfile {

    @Override
    public Map<String, String> getConfigOverrides() {
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
    public List<TestResourceEntry> testResources() {
        return ImmutableList.of(
               // new TestResourceEntry(EnvironmentTestResource.class),
                new TestResourceEntry(WiremockTestResource.class),
                new TestResourceEntry(RedisTestResource.class)
        );
    }

    @Override
    public boolean disableGlobalTestResources() {
        return true;
    }

}
