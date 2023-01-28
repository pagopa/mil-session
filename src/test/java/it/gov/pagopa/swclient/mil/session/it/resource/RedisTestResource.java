package it.gov.pagopa.swclient.mil.session.it.resource;

import com.google.common.collect.ImmutableMap;
import io.quarkus.test.common.DevServicesContext;
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

import java.util.Map;


public class RedisTestResource implements QuarkusTestResourceLifecycleManager, DevServicesContext.ContextAware {

    private static final Logger logger = LoggerFactory.getLogger(RedisTestResource.class);
    private static final String REDIS_NETWORK_ALIAS = "redis-it";

    private GenericContainer<?> redisContainer;

    private DevServicesContext devServicesContext;

    public void setIntegrationTestContext(DevServicesContext devServicesContext) {
        this.devServicesContext = devServicesContext;
    }

    @Override
    public Map<String, String> start() {

        // Start the needed container(s)
        redisContainer = new GenericContainer(DockerImageName.parse("redis:latest"))
                .withNetwork(getNetwork())
                .withNetworkAliases(REDIS_NETWORK_ALIAS)
                //.withNetworkMode(devServicesContext.containerNetworkId().get())
                .waitingFor(Wait.forListeningPort());

        redisContainer.withLogConsumer(new Slf4jLogConsumer(logger));

        redisContainer.start();

        final String redisEndpoint = "redis://" + REDIS_NETWORK_ALIAS + ":" + 6379;

        // Pass the configuration to the application under test
        return ImmutableMap.of(
            "redis.connection.string", redisEndpoint
        );
    }

    // create a "fake" network using the same id as the one that will be used by Quarkus
    // using the network is the only way to make the withNetworkAliases work
    private Network getNetwork() {
        logger.info("devServicesContext.containerNetworkId() -> " + devServicesContext.containerNetworkId());
        Network testNetwork = new Network() {
            @Override
            public String getId() {
                return devServicesContext.containerNetworkId().get();
            }

            @Override
            public void close() {

            }

            @Override
            public Statement apply(Statement statement, Description description) {
                return null;
            }
        };
        return testNetwork;
    }

    @Override
    public void stop() {
        // Stop the needed container(s)
        if (redisContainer != null) {
            redisContainer.stop();
        }
    }
}
