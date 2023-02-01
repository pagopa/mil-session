package it.gov.pagopa.swclient.mil.session.it;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Iterables;
import io.quarkus.test.common.DevServicesContext;
import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusIntegrationTest;
import io.quarkus.test.junit.TestProfile;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import it.gov.pagopa.swclient.mil.session.ErrorCode;
import it.gov.pagopa.swclient.mil.session.SessionTestData;
import it.gov.pagopa.swclient.mil.session.bean.CreateSessionRequest;
import it.gov.pagopa.swclient.mil.session.bean.Outcome;
import it.gov.pagopa.swclient.mil.session.dao.Session;
import it.gov.pagopa.swclient.mil.session.resource.SessionsResource;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.util.Arrays;

import static io.restassured.RestAssured.given;

@QuarkusIntegrationTest
@TestProfile(IntegrationTestProfile.class)
@TestHTTPEndpoint(SessionsResource.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SessionResourceTaxCodeTestIT implements DevServicesContext.ContextAware {

    private static final Logger logger = LoggerFactory.getLogger(SessionsResourceLifeCycleTestIT.class);

    private DevServicesContext devServicesContext;
    private JedisPool jedisPool;

    public void setIntegrationTestContext(DevServicesContext devServicesContext) {
        this.devServicesContext = devServicesContext;
    }

    @BeforeAll
    void initJedisPool() {
        String redisExposedPort = devServicesContext.devServicesProperties().get("test.redis.exposed-port");
        jedisPool = new JedisPool("127.0.0.1", Integer.parseInt(redisExposedPort));
    }

    @AfterAll
    void destroyJedisPool() {
        jedisPool.destroy();
    }

    @Test
    void testCreateSession_taxCode_201_accepted_saveCards() {

        CreateSessionRequest requestBody = new CreateSessionRequest();
        requestBody.setTaxCode(SessionTestData.CF_MARIO_ROSSI);

        Response response = given()
                .contentType(ContentType.JSON)
                .headers(
                        "RequestId", "1de3c885-5584-4910-b43a-4ad6e3fd55f9",
                        "Version", "1.0.0",
                        "AcquirerId", "12345",
                        "Channel", "ATM",
                        "TerminalId", "12345678")
                .and()
                .body(requestBody)
                .when()
                .post()
                .then()
                .extract()
                .response();

        Assertions.assertEquals(201, response.statusCode());
        Assertions.assertEquals(Outcome.OK.toString(), response.jsonPath().getString("outcome"));
        Assertions.assertTrue(response.jsonPath().getBoolean("saveNewCards"));
        Assertions.assertNull(response.jsonPath().getJsonObject("pairingToken"));
        Assertions.assertNotNull(response.header("Location"));

        // test data on redis
        try (Jedis jedis = jedisPool.getResource()) {
            String sessionId = Iterables.getLast(Arrays.asList(response.header("Location").split("/")));
            String redisSession = jedis.get(sessionId);
            Assertions.assertNotNull(redisSession);
            Session storedSession = new ObjectMapper().readValue(redisSession, Session.class);
            logger.debug("CF_MARIO_ROSSI stored session -> {}", storedSession);
            Assertions.assertEquals(SessionTestData.CF_MARIO_ROSSI, storedSession.getTaxCode());
            Assertions.assertTrue(storedSession.isTermsAndConditionAccepted());
            Assertions.assertTrue(storedSession.isSaveNewCards());

        } catch (JsonMappingException e) {
            throw new RuntimeException(e);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

    }

    @Test
    void testCreateSession_taxCode_201_accepted_notSaveCards() {

        CreateSessionRequest requestBody = new CreateSessionRequest();
        requestBody.setTaxCode(SessionTestData.CF_LUIGI_ROSSI);

        Response response = given()
                .contentType(ContentType.JSON)
                .headers(
                        "RequestId", "1de3c885-5584-4910-b43a-4ad6e3fd55f9",
                        "Version", "1.0.0",
                        "AcquirerId", "12345",
                        "Channel", "ATM",
                        "TerminalId", "12345678")
                .and()
                .body(requestBody)
                .when()
                .post()
                .then()
                .extract()
                .response();

        Assertions.assertEquals(201, response.statusCode());
        Assertions.assertEquals(Outcome.OK.toString(), response.jsonPath().getString("outcome"));
        Assertions.assertFalse(response.jsonPath().getBoolean("saveNewCards"));
        Assertions.assertNull(response.jsonPath().getJsonObject("pairingToken"));
        Assertions.assertNotNull(response.header("Location"));

        // test data on redis
        try (Jedis jedis = jedisPool.getResource()) {
            String sessionId = Iterables.getLast(Arrays.asList(response.header("Location").split("/")));
            String redisSession = jedis.get(sessionId);
            Assertions.assertNotNull(redisSession);
            Session storedSession = new ObjectMapper().readValue(redisSession, Session.class);
            logger.debug("CF_LUIGI_ROSSI stored session -> {}", storedSession);
            Assertions.assertEquals(SessionTestData.CF_LUIGI_ROSSI, storedSession.getTaxCode());
            Assertions.assertTrue(storedSession.isTermsAndConditionAccepted());
            Assertions.assertFalse(storedSession.isSaveNewCards());

        } catch (JsonMappingException e) {
            throw new RuntimeException(e);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

    }

    @Test
    void testCreateSession_taxCode_201_accepted_getSaveNewCardsFlag404() {

        CreateSessionRequest requestBody = new CreateSessionRequest();
        requestBody.setTaxCode(SessionTestData.CF_ALESSANDRO_ROSSI);

        Response response = given()
                .contentType(ContentType.JSON)
                .headers(
                        "RequestId", "1de3c885-5584-4910-b43a-4ad6e3fd55f9",
                        "Version", "1.0.0",
                        "AcquirerId", "12345",
                        "Channel", "ATM",
                        "TerminalId", "12345678")
                .and()
                .body(requestBody)
                .when()
                .post()
                .then()
                .extract()
                .response();

        Assertions.assertEquals(201, response.statusCode());
        Assertions.assertEquals(Outcome.OK.toString(), response.jsonPath().getString("outcome"));
        Assertions.assertFalse(response.jsonPath().getBoolean("saveNewCards"));
        Assertions.assertNotNull(response.header("Location"));
        Assertions.assertNull(response.jsonPath().getJsonObject("pairingToken"));

        // test data on redis
        try (Jedis jedis = jedisPool.getResource()) {
            String sessionId = Iterables.getLast(Arrays.asList(response.header("Location").split("/")));
            String redisSession = jedis.get(sessionId);
            Assertions.assertNotNull(redisSession);
            Session storedSession = new ObjectMapper().readValue(redisSession, Session.class);
            logger.debug("CF_ALESSANDRO_ROSSI stored session -> {}", storedSession);
            Assertions.assertEquals(SessionTestData.CF_ALESSANDRO_ROSSI, storedSession.getTaxCode());
            Assertions.assertTrue(storedSession.isTermsAndConditionAccepted());
            Assertions.assertFalse(storedSession.isSaveNewCards());

        } catch (JsonMappingException e) {
            throw new RuntimeException(e);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void testCreateSession_taxCode_201_accepted_getSaveNewCardsFlag500() {

        CreateSessionRequest requestBody = new CreateSessionRequest();
        requestBody.setTaxCode(SessionTestData.CF_GIOVANNI_ROSSI);

        Response response = given()
                .contentType(ContentType.JSON)
                .headers(
                        "RequestId", "1de3c885-5584-4910-b43a-4ad6e3fd55f9",
                        "Version", "1.0.0",
                        "AcquirerId", "12345",
                        "Channel", "ATM",
                        "TerminalId", "12345678")
                .and()
                .body(requestBody)
                .when()
                .post()
                .then()
                .extract()
                .response();

        Assertions.assertEquals(201, response.statusCode());
        Assertions.assertEquals(Outcome.OK.toString(), response.jsonPath().getString("outcome"));
        Assertions.assertFalse(response.jsonPath().getBoolean("saveNewCards"));
        Assertions.assertNotNull(response.header("Location"));
        Assertions.assertNull(response.jsonPath().getJsonObject("pairingToken"));


        // test data on redis
        try (Jedis jedis = jedisPool.getResource()) {
            String sessionId = Iterables.getLast(Arrays.asList(response.header("Location").split("/")));
            String redisSession = jedis.get(sessionId);
            Assertions.assertNotNull(redisSession);
            Session storedSession = new ObjectMapper().readValue(redisSession, Session.class);
            logger.debug("CF_GIOVANNI_ROSSI stored session -> {}", storedSession);
            Assertions.assertEquals(SessionTestData.CF_GIOVANNI_ROSSI, storedSession.getTaxCode());
            Assertions.assertTrue(storedSession.isTermsAndConditionAccepted());
            Assertions.assertFalse(storedSession.isSaveNewCards());

        } catch (JsonMappingException e) {
            throw new RuntimeException(e);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void testCreateSession_taxCode_201_accepted_getSaveNewCardsFlagTimeout() {

        CreateSessionRequest requestBody = new CreateSessionRequest();
        requestBody.setTaxCode(SessionTestData.CF_GIOVANNA_ROSSI);

        Response response = given()
                .contentType(ContentType.JSON)
                .headers(
                        "RequestId", "1de3c885-5584-4910-b43a-4ad6e3fd55f9",
                        "Version", "1.0.0",
                        "AcquirerId", "12345",
                        "Channel", "ATM",
                        "TerminalId", "12345678")
                .and()
                .body(requestBody)
                .when()
                .post()
                .then()
                .extract()
                .response();

        Assertions.assertEquals(201, response.statusCode());
        Assertions.assertEquals(Outcome.OK.toString(), response.jsonPath().getString("outcome"));
        Assertions.assertFalse(response.jsonPath().getBoolean("saveNewCards"));
        Assertions.assertNotNull(response.header("Location"));
        Assertions.assertNull(response.jsonPath().getJsonObject("pairingToken"));

        // test data on redis
        try (Jedis jedis = jedisPool.getResource()) {
            String sessionId = Iterables.getLast(Arrays.asList(response.header("Location").split("/")));
            String redisSession = jedis.get(sessionId);
            Assertions.assertNotNull(redisSession);
            Session storedSession = new ObjectMapper().readValue(redisSession, Session.class);
            logger.debug("CF_GIOVANNA_ROSSI stored session -> {}", storedSession);
            Assertions.assertEquals(SessionTestData.CF_GIOVANNA_ROSSI, storedSession.getTaxCode());
            Assertions.assertTrue(storedSession.isTermsAndConditionAccepted());
            Assertions.assertFalse(storedSession.isSaveNewCards());

        } catch (JsonMappingException e) {
            throw new RuntimeException(e);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

    }

    @Test
    void testCreateSession_taxCode_201_notAccepted() {

        CreateSessionRequest requestBody = new CreateSessionRequest();
        requestBody.setTaxCode(SessionTestData.CF_MARIO_VERDI);

        Response response = given()
                .contentType(ContentType.JSON)
                .headers(
                        "RequestId", "1de3c885-5584-4910-b43a-4ad6e3fd55f9",
                        "Version", "1.0.0",
                        "AcquirerId", "12345",
                        "Channel", "ATM",
                        "TerminalId", "12345678")
                .and()
                .body(requestBody)
                .when()
                .post()
                .then()
                .extract()
                .response();

        Assertions.assertEquals(201, response.statusCode());
        Assertions.assertEquals(Outcome.TERMS_AND_CONDITIONS_NOT_YET_ACCEPTED.toString(), response.jsonPath().getString("outcome"));
        Assertions.assertNull(response.jsonPath().getJsonObject("saveNewCards"));
        Assertions.assertNotNull(response.header("Location"));
        Assertions.assertNull(response.jsonPath().getJsonObject("pairingToken"));

        // test data on redis
        try (Jedis jedis = jedisPool.getResource()) {
            String sessionId = Iterables.getLast(Arrays.asList(response.header("Location").split("/")));
            String redisSession = jedis.get(sessionId);
            Assertions.assertNotNull(redisSession);
            Session storedSession = new ObjectMapper().readValue(redisSession, Session.class);
            logger.debug("CF_MARIO_VERDI stored session -> {}", storedSession);
            Assertions.assertEquals(SessionTestData.CF_MARIO_VERDI, storedSession.getTaxCode());
            Assertions.assertFalse(storedSession.isTermsAndConditionAccepted());
            Assertions.assertNull(storedSession.isSaveNewCards());

        } catch (JsonMappingException e) {
            throw new RuntimeException(e);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }


    @Test
    void testCreateSession_taxCode_201_tcCheck404() {

        CreateSessionRequest requestBody = new CreateSessionRequest();
        requestBody.setTaxCode(SessionTestData.CF_LUIGI_VERDI);

        Response response = given()
                .contentType(ContentType.JSON)
                .headers(
                        "RequestId", "1de3c885-5584-4910-b43a-4ad6e3fd55f9",
                        "Version", "1.0.0",
                        "AcquirerId", "12345",
                        "Channel", "ATM",
                        "TerminalId", "12345678")
                .and()
                .body(requestBody)
                .when()
                .post()
                .then()
                .extract()
                .response();

        Assertions.assertEquals(201, response.statusCode());
        Assertions.assertEquals(Outcome.TERMS_AND_CONDITIONS_NOT_YET_ACCEPTED.toString(), response.jsonPath().getString("outcome"));
        Assertions.assertNull(response.jsonPath().getJsonObject("saveNewCards"));
        Assertions.assertNotNull(response.header("Location"));
        Assertions.assertNull(response.jsonPath().getJsonObject("pairingToken"));

        // test data on redis
        try (Jedis jedis = jedisPool.getResource()) {
            String sessionId = Iterables.getLast(Arrays.asList(response.header("Location").split("/")));
            String redisSession = jedis.get(sessionId);
            Assertions.assertNotNull(redisSession);
            Session storedSession = new ObjectMapper().readValue(redisSession, Session.class);
            logger.debug("CF_LUIGI_VERDI stored session -> {}", storedSession);
            Assertions.assertEquals(SessionTestData.CF_LUIGI_VERDI, storedSession.getTaxCode());
            Assertions.assertFalse(storedSession.isTermsAndConditionAccepted());
            Assertions.assertNull(storedSession.isSaveNewCards());

        } catch (JsonMappingException e) {
            throw new RuntimeException(e);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

    }

    @Test
    void testCreateSession_taxCode_500_tcCheck500() {

        CreateSessionRequest requestBody = new CreateSessionRequest();
        requestBody.setTaxCode(SessionTestData.CF_MARIO_BIANCHI);

        Response response = given()
                .contentType(ContentType.JSON)
                .headers(
                        "RequestId", "1de3c885-5584-4910-b43a-4ad6e3fd55f9",
                        "Version", "1.0.0",
                        "AcquirerId", "12345",
                        "Channel", "ATM",
                        "TerminalId", "12345678")
                .and()
                .body(requestBody)
                .when()
                .post()
                .then()
                .extract()
                .response();

        Assertions.assertEquals(500, response.statusCode());
        Assertions.assertTrue(response.jsonPath().getList("errors").contains(ErrorCode.ERROR_CALLING_TERMS_AND_CONDITIONS_SERVICE));
        Assertions.assertNull(response.jsonPath().getJsonObject("outcome"));
        Assertions.assertNull(response.jsonPath().getJsonObject("saveNewCards"));
        Assertions.assertNull(response.header("Location"));
        Assertions.assertNull(response.jsonPath().getJsonObject("pairingToken"));

    }

    @Test
    void testCreateSession_taxCode_500_tcCheckTimeout() {

        CreateSessionRequest requestBody = new CreateSessionRequest();
        requestBody.setTaxCode(SessionTestData.CF_MARIA_BIANCHI);

        Response response = given()
                .contentType(ContentType.JSON)
                .headers(
                        "RequestId", "1de3c885-5584-4910-b43a-4ad6e3fd55f9",
                        "Version", "1.0.0",
                        "AcquirerId", "12345",
                        "Channel", "ATM",
                        "TerminalId", "12345678")
                .and()
                .body(requestBody)
                .when()
                .post()
                .then()
                .extract()
                .response();

        Assertions.assertEquals(500, response.statusCode());
        Assertions.assertTrue(response.jsonPath().getList("errors").contains(ErrorCode.ERROR_CALLING_TERMS_AND_CONDITIONS_SERVICE));
        Assertions.assertNull(response.jsonPath().getJsonObject("outcome"));
        Assertions.assertNull(response.jsonPath().getJsonObject("saveNewCards"));
        Assertions.assertNull(response.header("Location"));
        Assertions.assertNull(response.jsonPath().getJsonObject("pairingToken"));

    }

    @Test
    void testCreateSession_taxCode_400_validation() {

        CreateSessionRequest requestBody = new CreateSessionRequest();
        requestBody.setTaxCode("ABCDE");

        Response response = given()
                .contentType(ContentType.JSON)
                .headers(
                        "RequestId", "1de3c885-5584-4910-b43a-4ad6e3fd55f9",
                        "Version", "1.0.0",
                        "AcquirerId", "12345",
                        "Channel", "ATM",
                        "TerminalId", "12345678")
                .and()
                .body(requestBody)
                .when()
                .post()
                .then()
                .extract()
                .response();

        Assertions.assertEquals(400, response.statusCode());
        Assertions.assertEquals(true, response.jsonPath().getList("errors").contains(ErrorCode.TAX_CODE_MUST_MATCH_REGEXP));
        Assertions.assertNull(response.jsonPath().getJsonObject("outcome"));
        Assertions.assertNull(response.jsonPath().getJsonObject("saveNewCards"));
        Assertions.assertNull(response.header("Location"));
        Assertions.assertNull(response.jsonPath().getJsonObject("pairingToken"));

    }






//    @Test
//    void testCreateSession_taxCode_500_integrationError_saveSession() {
//
//        CreateSessionRequest requestBody = new CreateSessionRequest();
//        requestBody.setTaxCode(CF_MARIO_ROSSI);
//
//        TermsAndConditionsResponse termsAndCondsKO = new TermsAndConditionsResponse();
//        termsAndCondsKO.setOutcome(Outcome.OK.toString());
//
//        SaveNewCardsResponse saveNewCardsTrue = new SaveNewCardsResponse();
//        saveNewCardsTrue.setSaveNewCards(true);
//
//        Mockito
//                .when(termsAndCondsService.getTCByTaxCode(Mockito.eq(CF_MARIO_ROSSI), Mockito.any(CommonHeader.class)))
//                .thenReturn(Uni.createFrom().item(termsAndCondsKO));
//
//        Mockito
//                .when(pmWalletService.getSaveNewCards(CF_MARIO_ROSSI))
//                .thenReturn(Uni.createFrom().item(saveNewCardsTrue));
//
//        Mockito
//                .when(sessionService.set(Mockito.anyString(), Mockito.any()))
//                .thenReturn(Uni.createFrom().failure(() -> new RuntimeException()));
//
//        Response response = given()
//                .contentType(ContentType.JSON)
//                .headers(
//                        "RequestId", "1de3c885-5584-4910-b43a-4ad6e3fd55f9",
//                        "Version", "1.0.0",
//                        "AcquirerId", "12345",
//                        "Channel", "ATM",
//                        "TerminalId", "12345678")
//                .and()
//                .body(requestBody)
//                .when()
//                .post()
//                .then()
//                .extract()
//                .response();
//
//        Assertions.assertEquals(500, response.statusCode());
//        Assertions.assertEquals(true, response.jsonPath().getList("errors").contains(ErrorCode.REDIS_ERROR_WHILE_SAVING_SESSION));
//        Assertions.assertNull(response.jsonPath().getJsonObject("outcome"));
//        Assertions.assertNull(response.jsonPath().getJsonObject("saveNewCards"));
//        Assertions.assertNull(response.jsonPath().getJsonObject("pairingToken"));
//
//    }

}
