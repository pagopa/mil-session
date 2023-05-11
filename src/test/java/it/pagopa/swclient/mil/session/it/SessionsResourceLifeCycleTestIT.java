package it.pagopa.swclient.mil.session.it;

import static io.restassured.RestAssured.given;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

import io.quarkus.test.common.DevServicesContext;
import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusIntegrationTest;
import io.quarkus.test.junit.TestProfile;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import it.pagopa.swclient.mil.session.ErrorCode;
import it.pagopa.swclient.mil.session.SessionTestData;
import it.pagopa.swclient.mil.session.bean.Outcome;
import it.pagopa.swclient.mil.session.bean.UpdateSessionRequest;
import it.pagopa.swclient.mil.session.dao.Session;
import it.pagopa.swclient.mil.session.resource.SessionsResource;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

@QuarkusIntegrationTest
@TestProfile(IntegrationTestProfile.class)
@TestHTTPEndpoint(SessionsResource.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SessionsResourceLifeCycleTestIT implements DevServicesContext.ContextAware  {

    private static final Logger logger = LoggerFactory.getLogger(SessionsResourceLifeCycleTestIT.class);

    private Map<String, String> sessions;

    private DevServicesContext devServicesContext;

    private JedisPool jedisPool;

    public void setIntegrationTestContext(DevServicesContext devServicesContext) {
        this.devServicesContext = devServicesContext;
    }

    @BeforeAll
    void initRedis() {

        ObjectWriter ow = new ObjectMapper().writer();
        sessions = new HashMap<>();
        String sessionId;

        logger.debug("devServicesContext.devServicesProperties() -> " + devServicesContext.devServicesProperties());
        String redisExposedPort = devServicesContext.devServicesProperties().get("test.redis.exposed-port");

        jedisPool = new JedisPool("127.0.0.1", Integer.parseInt(redisExposedPort));
        try (Jedis jedis = jedisPool.getResource()) {

            // MARIO ROSSI
            Session session = new Session();
            session.setTaxCode(SessionTestData.CF_MARIO_ROSSI);
            session.setTermsAndConditionAccepted(true);
            session.setSaveNewCards(true);

            sessionId = UUID.randomUUID().toString();

            jedis.set(sessionId, ow.writeValueAsString(session));

            sessions.put(SessionTestData.CF_MARIO_ROSSI, sessionId);

            // MARIA ROSSI
            session = new Session();
            session.setTaxCode(SessionTestData.CF_MARIA_ROSSI);
            session.setTermsAndConditionAccepted(true);
            session.setSaveNewCards(true);

            sessionId = UUID.randomUUID().toString();

            jedis.set(sessionId, ow.writeValueAsString(session));

            sessions.put(SessionTestData.CF_MARIA_ROSSI, sessionId);

            // LUIGI ROSSI

            session = new Session();
            session.setTaxCode(SessionTestData.CF_LUIGI_ROSSI);
            session.setTermsAndConditionAccepted(true);
            session.setSaveNewCards(Boolean.FALSE);

            sessionId = UUID.randomUUID().toString();

            jedis.set(sessionId, ow.writeValueAsString(session));

            sessions.put(SessionTestData.CF_LUIGI_ROSSI, sessionId);

            // MARIO VERDI

            session = new Session();
            session.setTaxCode(SessionTestData.CF_MARIO_VERDI);
            session.setTermsAndConditionAccepted(false);

            sessionId = UUID.randomUUID().toString();

            jedis.set(sessionId, ow.writeValueAsString(session));

            sessions.put(SessionTestData.CF_MARIO_VERDI, sessionId);

            // LUIGI VERDI

            session = new Session();
            session.setTaxCode(SessionTestData.CF_LUIGI_VERDI);
            session.setTermsAndConditionAccepted(false);

            sessionId = UUID.randomUUID().toString();

            jedis.set(sessionId, ow.writeValueAsString(session));

            sessions.put(SessionTestData.CF_LUIGI_VERDI, sessionId);

        }
        catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

    }

    @AfterAll
    void destroyJedisPool() {
        jedisPool.destroy();
    }

    @Test
    void testGetSession_200_accepted_saveCards() {

        Response response = given()
                .headers(
                        "RequestId", "1de3c885-5584-4910-b43a-4ad6e3fd55f9",
                        "Version", "1.0.0",
                        "AcquirerId", "12345",
                        "Channel", "ATM",
                        "TerminalId", "12345678")
                .when()
                .get("/"+sessions.get(SessionTestData.CF_MARIO_ROSSI))
                .then()
                .extract()
                .response();

        Assertions.assertEquals(200, response.statusCode());
        Assertions.assertEquals(Outcome.OK.toString(), response.jsonPath().getString("outcome"));
        Assertions.assertEquals(SessionTestData.CF_MARIO_ROSSI, response.jsonPath().getString("taxCode"));
        Assertions.assertEquals(true, response.jsonPath().getBoolean("saveNewCards"));

    }

    @Test
    void testGetSession_200_accepted_notSaveCards() {

        Response response = given()
                .headers(
                        "RequestId", "1de3c885-5584-4910-b43a-4ad6e3fd55f9",
                        "Version", "1.0.0",
                        "AcquirerId", "12345",
                        "Channel", "ATM",
                        "TerminalId", "12345678")
                .when()
                .get("/"+sessions.get(SessionTestData.CF_LUIGI_ROSSI))
                .then()
                .extract()
                .response();

        Assertions.assertEquals(200, response.statusCode());
        Assertions.assertEquals(Outcome.OK.toString(), response.jsonPath().getString("outcome"));
        Assertions.assertEquals(SessionTestData.CF_LUIGI_ROSSI, response.jsonPath().getString("taxCode"));
        Assertions.assertEquals(false, response.jsonPath().getBoolean("saveNewCards"));

    }

    @Test
    void testGetSession_200_notAccepted() {

        Response response = given()
                .headers(
                        "RequestId", "1de3c885-5584-4910-b43a-4ad6e3fd55f9",
                        "Version", "1.0.0",
                        "AcquirerId", "12345",
                        "Channel", "ATM",
                        "TerminalId", "12345678")
                .when()
                .get("/"+sessions.get(SessionTestData.CF_MARIO_VERDI))
                .then()
                .extract()
                .response();

        Assertions.assertEquals(200, response.statusCode());
        Assertions.assertEquals(Outcome.TERMS_AND_CONDITIONS_NOT_YET_ACCEPTED.toString(), response.jsonPath().getString("outcome"));
        Assertions.assertEquals(SessionTestData.CF_MARIO_VERDI, response.jsonPath().getString("taxCode"));
        Assertions.assertNull(response.jsonPath().getJsonObject("saveNewCards"));

    }

    @Test
    void testGetSession_400_validation() {

        String sessionId = UUID.randomUUID().toString();
        sessionId = sessionId.replaceAll("-", "");

        Response response = given()
                .headers(
                        "RequestId", "1de3c885-5584-4910-b43a-4ad6e3fd55f9",
                        "Version", "1.0.0",
                        "AcquirerId", "12345",
                        "Channel", "ATM",
                        "TerminalId", "12345678")
                .when()
                .get("/"+sessionId)
                .then()
                .extract()
                .response();

        Assertions.assertEquals(400, response.statusCode());
        Assertions.assertTrue(response.jsonPath().getList("errors").contains(ErrorCode.SESSION_ID_MUST_MATCH_REGEXP));
        Assertions.assertNull(response.jsonPath().getJsonObject("outcome"));
        Assertions.assertNull(response.jsonPath().getJsonObject("taxCode"));
        Assertions.assertNull(response.jsonPath().getJsonObject("saveNewCards"));

    }

    @Test
    void testGetSession_404_notFound() {

        Response response = given()
                .headers(
                        "RequestId", "1de3c885-5584-4910-b43a-4ad6e3fd55f9",
                        "Version", "1.0.0",
                        "AcquirerId", "12345",
                        "Channel", "ATM",
                        "TerminalId", "12345678")
                .when()
                .get("/"+UUID.randomUUID().toString())
                .then()
                .extract()
                .response();

        Assertions.assertEquals(404, response.statusCode());
        Assertions.assertTrue(response.jsonPath().getList("errors").contains(ErrorCode.REDIS_ERROR_SESSION_NOT_FOUND));
        Assertions.assertNull(response.jsonPath().getJsonObject("outcome"));
        Assertions.assertNull(response.jsonPath().getJsonObject("taxCode"));
        Assertions.assertNull(response.jsonPath().getJsonObject("saveNewCards"));

    }


    @Test
    void testUpdateSession_200_accepted_saveCards() {

        UpdateSessionRequest updatedSession = new UpdateSessionRequest();
        updatedSession.setTermsAndCondsAccepted(Boolean.TRUE);
        updatedSession.setSaveNewCards(Boolean.TRUE);

        Response response = given()
                .contentType(ContentType.JSON)
                .headers(
                        "RequestId", "1de3c885-5584-4910-b43a-4ad6e3fd55f9",
                        "Version", "1.0.0",
                        "AcquirerId", "12345",
                        "Channel", "ATM",
                        "TerminalId", "12345678")
                .and()
                .body(updatedSession)
                .when()
                .patch("/"+sessions.get(SessionTestData.CF_LUIGI_VERDI))
                .then()
                .extract()
                .response();

        Assertions.assertEquals(202, response.statusCode());
        Assertions.assertEquals("ACCEPTED", response.jsonPath().getString("outcome"));
        Assertions.assertNull(response.jsonPath().getJsonObject("taxCode"));
        Assertions.assertNull(response.jsonPath().getJsonObject("saveNewCards"));

        // test data on redis
        try (Jedis jedis = jedisPool.getResource()) {
            String redisSession = jedis.get(sessions.get(SessionTestData.CF_LUIGI_VERDI));
            Assertions.assertNotNull(redisSession);
            Session storedSession = new ObjectMapper().readValue(redisSession, Session.class);
            logger.debug("CF_LUIGI_VERDI stored session -> {}", storedSession);
            Assertions.assertEquals(SessionTestData.CF_LUIGI_VERDI, storedSession.getTaxCode());
            Assertions.assertTrue(storedSession.isTermsAndConditionAccepted());
            Assertions.assertTrue(storedSession.isSaveNewCards());

        } catch (JsonMappingException e) {
            throw new RuntimeException(e);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

    }

    @Test
    void testUpdateSession_400_validation() {

        String sessionId = UUID.randomUUID().toString();
        sessionId = sessionId.replaceAll("-", "");

        UpdateSessionRequest updatedSession = new UpdateSessionRequest();
        updatedSession.setTermsAndCondsAccepted(Boolean.TRUE);
        updatedSession.setSaveNewCards(Boolean.TRUE);

        Response response = given()
                .contentType(ContentType.JSON)
                .headers(
                        "RequestId", "1de3c885-5584-4910-b43a-4ad6e3fd55f9",
                        "Version", "1.0.0",
                        "AcquirerId", "12345",
                        "Channel", "ATM",
                        "TerminalId", "12345678")
                .and()
                .body(updatedSession)
                .when()
                .patch("/"+sessionId)
                .then()
                .extract()
                .response();

        Assertions.assertEquals(400, response.statusCode());
        Assertions.assertTrue(response.jsonPath().getList("errors").contains(ErrorCode.SESSION_ID_MUST_MATCH_REGEXP));
        Assertions.assertNull(response.jsonPath().getJsonObject("outcome"));
        Assertions.assertNull(response.jsonPath().getJsonObject("taxCode"));
        Assertions.assertNull(response.jsonPath().getJsonObject("saveNewCards"));

    }

    @Test
    void testUpdateSession_404_notFound() {

        UpdateSessionRequest updatedSession = new UpdateSessionRequest();
        updatedSession.setTermsAndCondsAccepted(Boolean.TRUE);
        updatedSession.setSaveNewCards(Boolean.TRUE);

        Response response = given()
                .contentType(ContentType.JSON)
                .headers(
                        "RequestId", "1de3c885-5584-4910-b43a-4ad6e3fd55f9",
                        "Version", "1.0.0",
                        "AcquirerId", "12345",
                        "Channel", "ATM",
                        "TerminalId", "12345678")
                .and()
                .body(updatedSession)
                .when()
                .patch("/"+UUID.randomUUID().toString())
                .then()
                .extract()
                .response();

        Assertions.assertEquals(404, response.statusCode());
        Assertions.assertTrue(response.jsonPath().getList("errors").contains(ErrorCode.REDIS_ERROR_SESSION_NOT_FOUND));
        Assertions.assertNull(response.jsonPath().getJsonObject("outcome"));
        Assertions.assertNull(response.jsonPath().getJsonObject("taxCode"));
        Assertions.assertNull(response.jsonPath().getJsonObject("saveNewCards"));

    }


    @Test
    void testDeleteSession_204() {

        Response response = given()
                .headers(
                        "RequestId", "1de3c885-5584-4910-b43a-4ad6e3fd55f9",
                        "Version", "1.0.0",
                        "AcquirerId", "12345",
                        "Channel", "ATM",
                        "TerminalId", "12345678")
                .when()
                .delete("/"+sessions.get(SessionTestData.CF_MARIA_ROSSI))
                .then()
                .extract()
                .response();

        Assertions.assertEquals(204, response.statusCode());
        Assertions.assertEquals("", response.body().asString());

        // test data on redis
        try (Jedis jedis = jedisPool.getResource()) {
            String redisSession = jedis.get(sessions.get(SessionTestData.CF_MARIA_ROSSI));
            Assertions.assertNull(redisSession);
        }

    }

    @Test
    void testDeleteSession_400_validation() {

        String sessionId = UUID.randomUUID().toString();
        sessionId = sessionId.replaceAll("-", "");

        Response response = given()
                .headers(
                        "RequestId", "1de3c885-5584-4910-b43a-4ad6e3fd55f9",
                        "Version", "1.0.0",
                        "AcquirerId", "12345",
                        "Channel", "ATM",
                        "TerminalId", "12345678")
                .when()
                .delete("/"+sessionId)
                .then()
                .extract()
                .response();

        Assertions.assertEquals(400, response.statusCode());
        Assertions.assertTrue(response.jsonPath().getList("errors").contains(ErrorCode.SESSION_ID_MUST_MATCH_REGEXP));

    }

    @Test
    void testDeleteSession_404_notFound() {

        Response response = given()
                .headers(
                        "RequestId", "1de3c885-5584-4910-b43a-4ad6e3fd55f9",
                        "Version", "1.0.0",
                        "AcquirerId", "12345",
                        "Channel", "ATM",
                        "TerminalId", "12345678")
                .when()
                .delete("/"+ UUID.randomUUID().toString())
                .then()
                .extract()
                .response();

        Assertions.assertEquals(404, response.statusCode());
        Assertions.assertTrue(response.jsonPath().getList("errors").contains(ErrorCode.REDIS_ERROR_SESSION_NOT_FOUND));

    }

}
