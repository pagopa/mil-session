package it.gov.pagopa.swclient.mil.session.it;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusIntegrationTest;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import it.gov.pagopa.swclient.mil.session.ErrorCode;
import it.gov.pagopa.swclient.mil.session.bean.*;
import it.gov.pagopa.swclient.mil.session.it.resource.EnvironmentTestResource;
import it.gov.pagopa.swclient.mil.session.it.resource.RedisTestResource;
import it.gov.pagopa.swclient.mil.session.it.resource.WiremockTestResource;
import it.gov.pagopa.swclient.mil.session.resource.SessionsResource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import static io.restassured.RestAssured.given;

@QuarkusIntegrationTest
@QuarkusTestResource(value = EnvironmentTestResource.class, restrictToAnnotatedClass = true)
@QuarkusTestResource(value = WiremockTestResource.class, restrictToAnnotatedClass = true)
@QuarkusTestResource(value = RedisTestResource.class, restrictToAnnotatedClass = true)
@TestHTTPEndpoint(SessionsResource.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SessionsResourcePanTokenTestIT {

	final static String PAN_MARIO_ROSSI = "a5069caab6a149008426508e1a966eeb"; // accepted, save cards, 204
	final static String PAN_MARIA_ROSSI = "a5069caab6a149008426508e1a966eeb"; // accepted, save cards, 500
	final static String PAN_LUIGI_ROSSI = "a5430e624c4a46c2a0953c770019b97e"; // accepted, not save cards
	final static String PAN_GIOVANNI_ROSSI = "a6ea9a71c04c459694a1b9d239fa59fc"; // accepted, 500

	final static String PAN_MARIO_VERDI = "ab64da842a334d8e9e7c6c7d2fd706e3"; // never accepted
	final static String PAN_LUIGI_VERDI = "a358c16d12114ae89f4c504818185c2a"; // accepted but expired

	final static String PAN_MARIO_BIANCHI = "a2c52680f72745978ed2e991a86d86dd"; // 500


	@Test
	void testCreateSession_panToken_201_accepted_saveCards() {
		
		CreateSessionRequest requestBody = new CreateSessionRequest();
		requestBody.setPanToken(PAN_MARIO_ROSSI);

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
     
	}

	@Test
	void testCreateSession_panToken_201_accepted_saveCards_integrationError_presave() {

		CreateSessionRequest requestBody = new CreateSessionRequest();
		requestBody.setPanToken(PAN_MARIA_ROSSI);

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

	}
	
	@Test
	void testCreateSession_panToken_201_accepted_notSaveCards() {

		CreateSessionRequest requestBody = new CreateSessionRequest();
		requestBody.setPanToken(PAN_LUIGI_ROSSI);

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

	}


	@Test
	void testCreateSession_taxCode_201_notAccepted() {

		CreateSessionRequest requestBody = new CreateSessionRequest();
		requestBody.setPanToken(PAN_LUIGI_VERDI);

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
		Assertions.assertNull(response.jsonPath().getJsonObject("pairingToken"));
		Assertions.assertNotNull(response.header("Location"));

	}


	@Test
	void testCreateSession_taxCode_201_AcceptedExpired() {

		CreateSessionRequest requestBody = new CreateSessionRequest();
		requestBody.setPanToken(PAN_MARIO_VERDI);

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
		Assertions.assertNull(response.jsonPath().getJsonObject("pairingToken"));
		Assertions.assertNotNull(response.header("Location"));

	}


	@Test
	void testCreateSession_panToken_201_notAccepted() {

		CreateSessionRequest requestBody = new CreateSessionRequest();
		requestBody.setPanToken(PAN_MARIO_VERDI);

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

//	@Test
//	void testCreateSession_panToken_500_integrationError_getTaxCode() {
//
//		CreateSessionRequest requestBody = new CreateSessionRequest();
//		requestBody.setPanToken(PAN_MARIO_ROSSI);
//
//		Mockito
//				.when(pmWalletService.getTaxCode(PAN_MARIO_ROSSI))
//				.thenReturn(Uni.createFrom().failure(() -> new ClientWebApplicationException()));
//
//		Response response = given()
//				.contentType(ContentType.JSON)
//				.headers(
//						"RequestId", "1de3c885-5584-4910-b43a-4ad6e3fd55f9",
//						"Version", "1.0.0",
//						"AcquirerId", "12345",
//						"Channel", "ATM",
//						"TerminalId", "12345678")
//				.and()
//				.body(requestBody)
//				.when()
//				.post()
//				.then()
//				.extract()
//				.response();
//
//		Assertions.assertEquals(500, response.statusCode());
//		Assertions.assertTrue(response.jsonPath().getList("errors").contains(ErrorCode.ERROR_CALLING_GET_TAX_CODE_SERVICE));
//		Assertions.assertNull(response.jsonPath().getJsonObject("outcome"));
//		Assertions.assertNull(response.jsonPath().getJsonObject("saveNewCards"));
//		Assertions.assertNull(response.jsonPath().getJsonObject("pairingToken"));
//
//	}
//
//	@Test
//	void testCreateSession_panToken_204_getTaxCode_notFound() {
//
//		CreateSessionRequest requestBody = new CreateSessionRequest();
//		requestBody.setPanToken(PAN_MARIO_ROSSI);
//
//		Mockito
//				.when(pmWalletService.getTaxCode(PAN_MARIO_ROSSI))
//				.thenReturn(Uni.createFrom().failure(() -> new NotFoundException()));
//
//		Response response = given()
//				.contentType(ContentType.JSON)
//				.headers(
//						"RequestId", "1de3c885-5584-4910-b43a-4ad6e3fd55f9",
//						"Version", "1.0.0",
//						"AcquirerId", "12345",
//						"Channel", "ATM",
//						"TerminalId", "12345678")
//				.and()
//				.body(requestBody)
//				.when()
//				.post()
//				.then()
//				.extract()
//				.response();
//
//		Assertions.assertEquals(202, response.statusCode());
//		Assertions.assertEquals(Outcome.PAIR_WITH_IO.toString(), response.jsonPath().getJsonObject("outcome"));
//		Assertions.assertNull(response.jsonPath().getJsonObject("saveNewCards"));
//		Assertions.assertNotNull(response.jsonPath().getJsonObject("pairingToken"));
//		Assertions.assertTrue(response.header("Location") != null &&
//						response.header("Location").endsWith("/sessions?pairingToken="+response.jsonPath().getJsonObject("pairingToken")));
//		Assertions.assertNotNull(response.header("Max-Retries"));
//		Assertions.assertNotNull(response.header("Retry-After"));
//
//	}
//
//	@Test
//	void testCreateSession_panToken_500_integrationError_termsAndConditions() {
//
//		CreateSessionRequest requestBody = new CreateSessionRequest();
//		requestBody.setPanToken(PAN_MARIO_ROSSI);
//
//		GetTaxCodeResponse getTaxCodeResponse = new GetTaxCodeResponse();
//		getTaxCodeResponse.setTaxCode(CF_MARIO_ROSSI);
//
//		Mockito
//				.when(pmWalletService.getTaxCode(PAN_MARIO_ROSSI))
//				.thenReturn(Uni.createFrom().item(getTaxCodeResponse));
//
//		Mockito
//				.when(termsAndCondsService.getTCByTaxCode(Mockito.anyString(), Mockito.any(CommonHeader.class)))
//				.thenReturn(Uni.createFrom().failure(() -> new ClientWebApplicationException()));
//
//		Response response = given()
//				.contentType(ContentType.JSON)
//				.headers(
//						"RequestId", "1de3c885-5584-4910-b43a-4ad6e3fd55f9",
//						"Version", "1.0.0",
//						"AcquirerId", "12345",
//						"Channel", "ATM",
//						"TerminalId", "12345678")
//				.and()
//				.body(requestBody)
//				.when()
//				.post()
//				.then()
//				.extract()
//				.response();
//
//		Assertions.assertEquals(500, response.statusCode());
//		Assertions.assertTrue(response.jsonPath().getList("errors").contains(ErrorCode.ERROR_CALLING_TERMS_AND_CONDITIONS_SERVICE));
//		Assertions.assertNull(response.jsonPath().getJsonObject("outcome"));
//		Assertions.assertNull(response.jsonPath().getJsonObject("saveNewCards"));
//		Assertions.assertNull(response.jsonPath().getJsonObject("pairingToken"));
//
//	}
//
//	@Test
//	void testCreateSession_panToken_201_notFound_termsAndConditions() {
//
//		CreateSessionRequest requestBody = new CreateSessionRequest();
//		requestBody.setPanToken(PAN_MARIO_ROSSI);
//
//		GetTaxCodeResponse getTaxCodeResponse = new GetTaxCodeResponse();
//		getTaxCodeResponse.setTaxCode(CF_MARIO_ROSSI);
//
//		Mockito
//				.when(pmWalletService.getTaxCode(PAN_MARIO_ROSSI))
//				.thenReturn(Uni.createFrom().item(getTaxCodeResponse));
//
//		Mockito
//				.when(termsAndCondsService.getTCByTaxCode(Mockito.anyString(), Mockito.any(CommonHeader.class)))
//				.thenReturn(Uni.createFrom().failure(() -> new NotFoundException()));
//
//		Response response = given()
//				.contentType(ContentType.JSON)
//				.headers(
//						"RequestId", "1de3c885-5584-4910-b43a-4ad6e3fd55f9",
//						"Version", "1.0.0",
//						"AcquirerId", "12345",
//						"Channel", "ATM",
//						"TerminalId", "12345678")
//				.and()
//				.body(requestBody)
//				.when()
//				.post()
//				.then()
//				.extract()
//				.response();
//
//		Assertions.assertEquals(201, response.statusCode());
//		Assertions.assertEquals(Outcome.TERMS_AND_CONDITIONS_NOT_YET_ACCEPTED.toString(), response.jsonPath().getJsonObject("outcome"));
//		Assertions.assertNull(response.jsonPath().getJsonObject("saveNewCards"));
//		Assertions.assertNull(response.jsonPath().getJsonObject("pairingToken"));
//
//	}
//
//	@Test
//	void testCreateSession_panToken_201_integrationError_getSaveNewCards() {
//
//		CreateSessionRequest requestBody = new CreateSessionRequest();
//		requestBody.setPanToken(PAN_MARIO_ROSSI);
//
//		GetTaxCodeResponse getTaxCodeResponse = new GetTaxCodeResponse();
//		getTaxCodeResponse.setTaxCode(CF_MARIO_ROSSI);
//
//		TermsAndConditionsResponse termsAndCondsKO = new TermsAndConditionsResponse();
//		termsAndCondsKO.setOutcome(Outcome.OK.toString());
//
//		Mockito
//				.when(pmWalletService.getTaxCode(PAN_MARIO_ROSSI))
//				.thenReturn(Uni.createFrom().item(getTaxCodeResponse));
//
//		Mockito
//				.when(termsAndCondsService.getTCByTaxCode(Mockito.eq(CF_MARIO_ROSSI), Mockito.any(CommonHeader.class)))
//				.thenReturn(Uni.createFrom().item(termsAndCondsKO));
//
//		Mockito
//				.when(pmWalletService.getSaveNewCards(Mockito.anyString()))
//				.thenReturn(Uni.createFrom().failure(() -> new ClientWebApplicationException()));
//
//		Response response = given()
//				.contentType(ContentType.JSON)
//				.headers(
//						"RequestId", "1de3c885-5584-4910-b43a-4ad6e3fd55f9",
//						"Version", "1.0.0",
//						"AcquirerId", "12345",
//						"Channel", "ATM",
//						"TerminalId", "12345678")
//				.and()
//				.body(requestBody)
//				.when()
//				.post()
//				.then()
//				.extract()
//				.response();
//
//		Assertions.assertEquals(201, response.statusCode());
//		Assertions.assertEquals(Outcome.OK.toString(), response.jsonPath().getString("outcome"));
//		Assertions.assertNull(response.jsonPath().getJsonObject("saveNewCards"));
//		Assertions.assertNull(response.jsonPath().getJsonObject("pairingToken"));
//	}
//
//	@Test
//	void testCreateSession_panToken_201_integrationError_saveCard() {
//
//		CreateSessionRequest requestBody = new CreateSessionRequest();
//		requestBody.setPanToken(PAN_MARIO_ROSSI);
//
//		GetTaxCodeResponse getTaxCodeResponse = new GetTaxCodeResponse();
//		getTaxCodeResponse.setTaxCode(CF_MARIO_ROSSI);
//
//		TermsAndConditionsResponse termsAndCondsKO = new TermsAndConditionsResponse();
//		termsAndCondsKO.setOutcome(Outcome.OK.toString());
//
//		SaveNewCardsResponse saveNewCardsTrue = new SaveNewCardsResponse();
//		saveNewCardsTrue.setSaveNewCards(true);
//
//		Mockito
//				.when(pmWalletService.getTaxCode(PAN_MARIO_ROSSI))
//				.thenReturn(Uni.createFrom().item(getTaxCodeResponse));
//
//		Mockito
//				.when(termsAndCondsService.getTCByTaxCode(Mockito.eq(CF_MARIO_ROSSI), Mockito.any(CommonHeader.class)))
//				.thenReturn(Uni.createFrom().item(termsAndCondsKO));
//
//		Mockito
//				.when(pmWalletService.getSaveNewCards(CF_MARIO_ROSSI))
//				.thenReturn(Uni.createFrom().item(saveNewCardsTrue));
//
//		Mockito
//				.when(pmWalletService.saveCard(Mockito.any(SaveCardRequest.class)))
//				.thenReturn(Uni.createFrom().failure(() -> new ClientWebApplicationException()));
//
//		Response response = given()
//				.contentType(ContentType.JSON)
//				.headers(
//						"RequestId", "1de3c885-5584-4910-b43a-4ad6e3fd55f9",
//						"Version", "1.0.0",
//						"AcquirerId", "12345",
//						"Channel", "ATM",
//						"TerminalId", "12345678")
//				.and()
//				.body(requestBody)
//				.when()
//				.post()
//				.then()
//				.extract()
//				.response();
//
//		Assertions.assertEquals(201, response.statusCode());
//		Assertions.assertEquals(Outcome.OK.toString(), response.jsonPath().getString("outcome"));
//		Assertions.assertNull(response.jsonPath().getJsonObject("saveNewCards"));
//		Assertions.assertNull(response.jsonPath().getJsonObject("pairingToken"));
//
//	}
//
//	@Test
//	void testCreateSession_panToken_500_integrationError_saveSession() {
//
//		CreateSessionRequest requestBody = new CreateSessionRequest();
//		requestBody.setPanToken(PAN_MARIO_ROSSI);
//
//		GetTaxCodeResponse getTaxCodeResponse = new GetTaxCodeResponse();
//		getTaxCodeResponse.setTaxCode(CF_MARIO_ROSSI);
//
//		TermsAndConditionsResponse termsAndCondsKO = new TermsAndConditionsResponse();
//		termsAndCondsKO.setOutcome(Outcome.OK.toString());
//
//		SaveNewCardsResponse saveNewCardsTrue = new SaveNewCardsResponse();
//		saveNewCardsTrue.setSaveNewCards(true);
//
//		Mockito
//				.when(pmWalletService.getTaxCode(PAN_MARIO_ROSSI))
//				.thenReturn(Uni.createFrom().item(getTaxCodeResponse));
//
//		Mockito
//				.when(termsAndCondsService.getTCByTaxCode(Mockito.anyString(), Mockito.any(CommonHeader.class)))
//				.thenReturn(Uni.createFrom().item(termsAndCondsKO));
//
//		Mockito
//				.when(pmWalletService.getSaveNewCards(CF_MARIO_ROSSI))
//				.thenReturn(Uni.createFrom().item(saveNewCardsTrue));
//
//		Mockito
//				.when(pmWalletService.saveCard(Mockito.any(SaveCardRequest.class)))
//				.thenReturn(Uni.createFrom().item(javax.ws.rs.core.Response.ok().build()));
//
//		Mockito
//				.when(sessionService.set(Mockito.anyString(), Mockito.any()))
//				.thenReturn(Uni.createFrom().failure(() -> new RuntimeException()));
//
//		Response response = given()
//				.contentType(ContentType.JSON)
//				.headers(
//						"RequestId", "1de3c885-5584-4910-b43a-4ad6e3fd55f9",
//						"Version", "1.0.0",
//						"AcquirerId", "12345",
//						"Channel", "ATM",
//						"TerminalId", "12345678")
//				.and()
//				.body(requestBody)
//				.when()
//				.post()
//				.then()
//				.extract()
//				.response();
//
//		Assertions.assertEquals(500, response.statusCode());
//		Assertions.assertEquals(true, response.jsonPath().getList("errors").contains(ErrorCode.REDIS_ERROR_WHILE_SAVING_SESSION));
//		Assertions.assertNull(response.jsonPath().getJsonObject("outcome"));
//		Assertions.assertNull(response.jsonPath().getJsonObject("saveNewCards"));
//		Assertions.assertNull(response.jsonPath().getJsonObject("pairingToken"));
//
//	}

}