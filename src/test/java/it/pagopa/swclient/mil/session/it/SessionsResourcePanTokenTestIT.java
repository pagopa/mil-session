package it.pagopa.swclient.mil.session.it;

import static io.restassured.RestAssured.given;

import java.util.Arrays;
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
import com.google.common.collect.Iterables;

import io.quarkus.test.common.DevServicesContext;
import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusIntegrationTest;
import io.quarkus.test.junit.TestProfile;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import it.pagopa.swclient.mil.session.ErrorCode;
import it.pagopa.swclient.mil.session.SessionTestData;
import it.pagopa.swclient.mil.session.bean.CreateSessionRequest;
import it.pagopa.swclient.mil.session.bean.Outcome;
import it.pagopa.swclient.mil.session.dao.Session;
import it.pagopa.swclient.mil.session.resource.SessionsResource;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

@QuarkusIntegrationTest
@TestProfile(IntegrationTestProfile.class)
@TestHTTPEndpoint(SessionsResource.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SessionsResourcePanTokenTestIT implements DevServicesContext.ContextAware {

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
	void testCreateSession_panToken_201_accepted_saveCards() {
		
		CreateSessionRequest requestBody = new CreateSessionRequest();
		requestBody.setPanToken(SessionTestData.PAN_MARIO_ROSSI);

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
        Assertions.assertNull(response.jsonPath().getJsonObject("saveNewCards"));
		Assertions.assertNotNull(response.header("Location"));
		Assertions.assertNull(response.jsonPath().getJsonObject("pairingToken"));

		// test data on redis
		try (Jedis jedis = jedisPool.getResource()) {
			String sessionId = Iterables.getLast(Arrays.asList(response.header("Location").split("/")));
			String redisSession = jedis.get(sessionId);
			Assertions.assertNotNull(redisSession);
			Session storedSession = new ObjectMapper().readValue(redisSession, Session.class);
			logger.debug("PAN_MARIO_ROSSI stored session -> {}", storedSession);
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
	void testCreateSession_panToken_201_accepted_saveCards_presave500() {

		CreateSessionRequest requestBody = new CreateSessionRequest();
		requestBody.setPanToken(SessionTestData.PAN_MARIA_ROSSI);

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
		Assertions.assertNull(response.jsonPath().getJsonObject("saveNewCards"));
		Assertions.assertNotNull(response.header("Location"));
		Assertions.assertNull(response.jsonPath().getJsonObject("pairingToken"));

		// test data on redis
		try (Jedis jedis = jedisPool.getResource()) {
			String sessionId = Iterables.getLast(Arrays.asList(response.header("Location").split("/")));
			String redisSession = jedis.get(sessionId);
			Assertions.assertNotNull(redisSession);
			Session storedSession = new ObjectMapper().readValue(redisSession, Session.class);
			logger.debug("PAN_MARIA_ROSSI stored session -> {}", storedSession);
			Assertions.assertEquals(SessionTestData.CF_MARIA_ROSSI, storedSession.getTaxCode());
			Assertions.assertTrue(storedSession.isTermsAndConditionAccepted());
			Assertions.assertTrue(storedSession.isSaveNewCards());

		} catch (JsonMappingException e) {
			throw new RuntimeException(e);
		} catch (JsonProcessingException e) {
			throw new RuntimeException(e);
		}

	}

	@Test
	void testCreateSession_panToken_201_accepted_saveCards_presaveTimeout() {

		CreateSessionRequest requestBody = new CreateSessionRequest();
		requestBody.setPanToken(SessionTestData.PAN_MARTA_ROSSI);

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
		Assertions.assertNull(response.jsonPath().getJsonObject("saveNewCards"));
		Assertions.assertNotNull(response.header("Location"));
		Assertions.assertNull(response.jsonPath().getJsonObject("pairingToken"));

		// test data on redis
		try (Jedis jedis = jedisPool.getResource()) {
			String sessionId = Iterables.getLast(Arrays.asList(response.header("Location").split("/")));
			String redisSession = jedis.get(sessionId);
			Assertions.assertNotNull(redisSession);
			Session storedSession = new ObjectMapper().readValue(redisSession, Session.class);
			logger.debug("PAN_MARTA_ROSSI stored session -> {}", storedSession);
			Assertions.assertEquals(SessionTestData.CF_MARTA_ROSSI, storedSession.getTaxCode());
			Assertions.assertTrue(storedSession.isTermsAndConditionAccepted());
			Assertions.assertTrue(storedSession.isSaveNewCards());

		} catch (JsonMappingException e) {
			throw new RuntimeException(e);
		} catch (JsonProcessingException e) {
			throw new RuntimeException(e);
		}

	}
	
	@Test
	void testCreateSession_panToken_201_accepted_notSaveCards() {

		CreateSessionRequest requestBody = new CreateSessionRequest();
		requestBody.setPanToken(SessionTestData.PAN_LUIGI_ROSSI);

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
		Assertions.assertNull(response.jsonPath().getJsonObject("saveNewCards"));
		Assertions.assertNotNull(response.header("Location"));
		Assertions.assertNull(response.jsonPath().getJsonObject("pairingToken"));

		// test data on redis
		try (Jedis jedis = jedisPool.getResource()) {
			String sessionId = Iterables.getLast(Arrays.asList(response.header("Location").split("/")));
			String redisSession = jedis.get(sessionId);
			Assertions.assertNotNull(redisSession);
			Session storedSession = new ObjectMapper().readValue(redisSession, Session.class);
			logger.debug("PAN_LUIGI_ROSSI stored session -> {}", storedSession);
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
	void testCreateSession_panToken_201_accepted_getSaveNewCardsFlag404() {

		CreateSessionRequest requestBody = new CreateSessionRequest();
		requestBody.setPanToken(SessionTestData.PAN_ALESSANDRO_ROSSI);

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
		Assertions.assertNull(response.jsonPath().getJsonObject("saveNewCards"));
		Assertions.assertNotNull(response.header("Location"));
		Assertions.assertNull(response.jsonPath().getJsonObject("pairingToken"));

		// test data on redis
		try (Jedis jedis = jedisPool.getResource()) {
			String sessionId = Iterables.getLast(Arrays.asList(response.header("Location").split("/")));
			String redisSession = jedis.get(sessionId);
			Assertions.assertNotNull(redisSession);
			Session storedSession = new ObjectMapper().readValue(redisSession, Session.class);
			logger.debug("PAN_ALESSANDRO_ROSSI stored session -> {}", storedSession);
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
	void testCreateSession_panToken_201_accepted_getSaveNewCardsFlag500() {

		CreateSessionRequest requestBody = new CreateSessionRequest();
		requestBody.setPanToken(SessionTestData.PAN_GIOVANNI_ROSSI);

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
		Assertions.assertNull(response.jsonPath().getJsonObject("saveNewCards"));
		Assertions.assertNotNull(response.header("Location"));
		Assertions.assertNull(response.jsonPath().getJsonObject("pairingToken"));

		// test data on redis
		try (Jedis jedis = jedisPool.getResource()) {
			String sessionId = Iterables.getLast(Arrays.asList(response.header("Location").split("/")));
			String redisSession = jedis.get(sessionId);
			Assertions.assertNotNull(redisSession);
			Session storedSession = new ObjectMapper().readValue(redisSession, Session.class);
			logger.debug("PAN_GIOVANNI_ROSSI stored session -> {}", storedSession);
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
	void testCreateSession_panToken_500_tcCheckTimeout() {

		CreateSessionRequest requestBody = new CreateSessionRequest();
		requestBody.setPanToken(SessionTestData.PAN_MARIA_BIANCHI);

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
		Assertions.assertNull(response.jsonPath().getJsonObject("outcome"));
		Assertions.assertNull(response.jsonPath().getJsonObject("saveNewCards"));
		Assertions.assertNull(response.header("Location"));
		Assertions.assertNull(response.jsonPath().getJsonObject("pairingToken"));

	}


	@Test
	void testCreateSession_panToken_400_validation() {

		CreateSessionRequest requestBody = new CreateSessionRequest();
		requestBody.setPanToken("ABC$DE");

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
		Assertions.assertTrue(response.jsonPath().getList("errors").contains(ErrorCode.PAN_TOKEN_MUST_MATCH_REGEXP));
		Assertions.assertNull(response.jsonPath().getJsonObject("outcome"));
		Assertions.assertNull(response.jsonPath().getJsonObject("saveNewCards"));
		Assertions.assertNull(response.jsonPath().getJsonObject("pairingToken"));

	}

	@Test
	void testCreateSession_panToken_500_retrieveTaxCode500() {

		CreateSessionRequest requestBody = new CreateSessionRequest();
		requestBody.setPanToken(UUID.randomUUID().toString()
				.replaceAll("-", "")
				.replaceFirst("^.", "z"));

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
		Assertions.assertTrue(response.jsonPath().getList("errors").contains(ErrorCode.ERROR_CALLING_GET_TAX_CODE_SERVICE));
		Assertions.assertNull(response.jsonPath().getJsonObject("outcome"));
		Assertions.assertNull(response.jsonPath().getJsonObject("saveNewCards"));
		Assertions.assertNull(response.jsonPath().getJsonObject("pairingToken"));

	}

	@Test
	void testCreateSession_panToken_500_retrieveTaxCodeTimeout() {

		CreateSessionRequest requestBody = new CreateSessionRequest();
		requestBody.setPanToken(UUID.randomUUID().toString()
				.replaceAll("-", "")
				.replaceFirst("^.", "y"));

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
		Assertions.assertTrue(response.jsonPath().getList("errors").contains(ErrorCode.ERROR_CALLING_GET_TAX_CODE_SERVICE));
		Assertions.assertNull(response.jsonPath().getJsonObject("outcome"));
		Assertions.assertNull(response.jsonPath().getJsonObject("saveNewCards"));
		Assertions.assertNull(response.jsonPath().getJsonObject("pairingToken"));

	}

	@Test
	void testCreateSession_panToken_204_retrieveTaxCode404() {

		CreateSessionRequest requestBody = new CreateSessionRequest();
		requestBody.setPanToken(UUID.randomUUID().toString()
				.replaceAll("-", "")
				.replaceFirst("^.", "b"));

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

		Assertions.assertEquals(202, response.statusCode());
		Assertions.assertEquals(Outcome.PAIR_WITH_IO.toString(), response.jsonPath().getJsonObject("outcome"));
		Assertions.assertNull(response.jsonPath().getJsonObject("saveNewCards"));
		Assertions.assertNotNull(response.jsonPath().getJsonObject("pairingToken"));
		Assertions.assertTrue(response.header("Location") != null &&
						response.header("Location").endsWith("/sessions?pairingToken="+response.jsonPath().getJsonObject("pairingToken")));
		Assertions.assertNotNull(response.header("Max-Retries"));
		Assertions.assertNotNull(response.header("Retry-After"));

		// test data on redis
		try (Jedis jedis = jedisPool.getResource()) {
			String pairingToken = Iterables.getLast(Arrays.asList(response.header("Location").split("=")));
			String redisPairing = jedis.get(pairingToken);
			Assertions.assertNull(redisPairing);
		}

	}
	
	@Test
	void testCreateSession_panToken_201_dbLocal_accepted() {

		CreateSessionRequest requestBody = new CreateSessionRequest();
		requestBody.setPanToken(SessionTestData.PAN_FRANCO_ROSSI);

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
        Assertions.assertNull(response.jsonPath().getJsonObject("saveNewCards"));
		Assertions.assertNotNull(response.header("Location"));
		Assertions.assertNull(response.jsonPath().getJsonObject("pairingToken"));

		// test data on redis
		try (Jedis jedis = jedisPool.getResource()) {
			String sessionId = Iterables.getLast(Arrays.asList(response.header("Location").split("/")));
			String redisSession = jedis.get(sessionId);
			Assertions.assertNotNull(redisSession);
			Session storedSession = new ObjectMapper().readValue(redisSession, Session.class);
			logger.debug("CF_FRANCO_ROSSI stored session -> {}", storedSession);
			Assertions.assertEquals(SessionTestData.CF_FRANCO_ROSSI, storedSession.getTaxCode());
			Assertions.assertTrue(storedSession.isTermsAndConditionAccepted());

		} catch (JsonMappingException e) {
			throw new RuntimeException(e);
		} catch (JsonProcessingException e) {
			throw new RuntimeException(e);
		}

	}
	
	
	@Test
	void testCreateSession_panToken_201_dbLocal_notAccepted() {

		CreateSessionRequest requestBody = new CreateSessionRequest();
		requestBody.setPanToken(SessionTestData.PAN_FRANCO_VERDI);

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
			logger.debug("CF_FRANCO_VERDI stored session -> {}", storedSession);
			Assertions.assertEquals(SessionTestData.CF_FRANCO_VERDI, storedSession.getTaxCode());
			Assertions.assertFalse(storedSession.isTermsAndConditionAccepted());

		} catch (JsonMappingException e) {
			throw new RuntimeException(e);
		} catch (JsonProcessingException e) {
			throw new RuntimeException(e);
		}

	}
	

}