package it.pagopa.swclient.mil.session;

import static io.restassured.RestAssured.given;

import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.resteasy.reactive.ClientWebApplicationException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.mockito.Mockito;

import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.smallrye.mutiny.Uni;
import it.pagopa.swclient.mil.bean.CommonHeader;
import it.pagopa.swclient.mil.session.bean.CreateSessionRequest;
import it.pagopa.swclient.mil.session.bean.Outcome;
import it.pagopa.swclient.mil.session.bean.pmwallet.GetSaveNewCardsFlagRequest;
import it.pagopa.swclient.mil.session.bean.termsandconds.CheckResponse;
import it.pagopa.swclient.mil.session.client.PMWalletService;
import it.pagopa.swclient.mil.session.client.TermsAndConditionsService;
import it.pagopa.swclient.mil.session.dao.SessionService;
import it.pagopa.swclient.mil.session.resource.SessionsResource;


@QuarkusTest
@TestHTTPEndpoint(SessionsResource.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SessionsResourceTaxCodeTest {

	@InjectMock
    @RestClient 
    TermsAndConditionsService termsAndCondsService;
	
	@InjectMock
    @RestClient 
    PMWalletService pmWalletService;
	
	@InjectMock
	SessionService sessionService;

	@BeforeAll
	public void setup() {

		Mockito
				.when(sessionService.set(Mockito.anyString(), Mockito.any()))
				.thenReturn(Uni.createFrom().voidItem());

	}

	@Test
	void testInitSession_taxCode_201_accepted_saveCards() {
		
		CreateSessionRequest requestBody = new CreateSessionRequest();
		requestBody.setTaxCode(SessionTestData.CF_MARIO_ROSSI);
		
		CheckResponse termsAndCondsOk = new CheckResponse();
		termsAndCondsOk.setOutcome(Outcome.OK.toString());
		
		GetSaveNewCardsFlagRequest saveNewCardsTrue = new GetSaveNewCardsFlagRequest();
		saveNewCardsTrue.setSaveNewCards(true);
		
		Mockito.when(termsAndCondsService.check(Mockito.eq(SessionTestData.CF_MARIO_ROSSI), Mockito.any(CommonHeader.class)))
			.thenReturn(Uni.createFrom().item(termsAndCondsOk));
		
		Mockito
			.when(pmWalletService.getSaveNewCardsFlag(SessionTestData.CF_MARIO_ROSSI))
			.thenReturn(Uni.createFrom().item(saveNewCardsTrue));

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
     
	}
	
	@Test
	void testInitSession_taxCode_201_accepted_notSaveCards() {
		
		CreateSessionRequest requestBody = new CreateSessionRequest();
		requestBody.setTaxCode(SessionTestData.CF_LUIGI_ROSSI);
		
		CheckResponse termsAndCondsOk = new CheckResponse();
		termsAndCondsOk.setOutcome(Outcome.OK.toString());
		
		GetSaveNewCardsFlagRequest saveNewCardsFalse = new GetSaveNewCardsFlagRequest();
		saveNewCardsFalse.setSaveNewCards(false);
		
		Mockito
			.when(termsAndCondsService.check(Mockito.eq(SessionTestData.CF_LUIGI_ROSSI), Mockito.any(CommonHeader.class)))
			.thenReturn(Uni.createFrom().item(termsAndCondsOk));
		
		Mockito
			.when(pmWalletService.getSaveNewCardsFlag(SessionTestData.CF_LUIGI_ROSSI))
			.thenReturn(Uni.createFrom().item(saveNewCardsFalse));
		
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

	}
	
	@Test
	void testInitSession_taxCode_201_notAccepted() {
		
		CreateSessionRequest requestBody = new CreateSessionRequest();
		requestBody.setTaxCode(SessionTestData.CF_LUIGI_VERDI);

		Mockito
			.when(termsAndCondsService.check(Mockito.eq(SessionTestData.CF_LUIGI_VERDI), Mockito.any(CommonHeader.class)))
				.thenReturn(Uni.createFrom().failure(() -> new ClientWebApplicationException(404)));

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
	void testInitSession_taxCode_201_AcceptedExpired() {

		CreateSessionRequest requestBody = new CreateSessionRequest();
		requestBody.setTaxCode(SessionTestData.CF_MARIO_VERDI);

		CheckResponse termsAndCondsKO = new CheckResponse();
		termsAndCondsKO.setOutcome(Outcome.TERMS_AND_CONDITIONS_NOT_YET_ACCEPTED.toString());

		Mockito
				.when(termsAndCondsService.check(Mockito.eq(SessionTestData.CF_MARIO_VERDI), Mockito.any(CommonHeader.class)))
				.thenReturn(Uni.createFrom().item(termsAndCondsKO));

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
	void testInitSession_taxCode_400_validation() {

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
		Assertions.assertNull(response.jsonPath().getJsonObject("pairingToken"));

	}

	@Test
	void testInitSession_taxCode_500_integrationError_termsAndConditions() {

		CreateSessionRequest requestBody = new CreateSessionRequest();
		requestBody.setTaxCode(SessionTestData.CF_MARIO_ROSSI);

		Mockito
				.when(termsAndCondsService.check(Mockito.anyString(), Mockito.any(CommonHeader.class)))
				.thenReturn(Uni.createFrom().failure(() -> new ClientWebApplicationException(500)));

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
		Assertions.assertNull(response.jsonPath().getJsonObject("pairingToken"));

	}

	@Test
	void testInitSession_taxCode_201_accepted_integrationError_getSaveNewCards() {

		CreateSessionRequest requestBody = new CreateSessionRequest();
		requestBody.setTaxCode(SessionTestData.CF_MARIO_ROSSI);

		CheckResponse termsAndCondsKO = new CheckResponse();
		termsAndCondsKO.setOutcome(Outcome.OK.toString());

		Mockito
				.when(termsAndCondsService.check(Mockito.eq(SessionTestData.CF_MARIO_ROSSI), Mockito.any(CommonHeader.class)))
				.thenReturn(Uni.createFrom().item(termsAndCondsKO));

		Mockito
				.when(pmWalletService.getSaveNewCardsFlag(SessionTestData.CF_MARIO_ROSSI))
				.thenReturn(Uni.createFrom().failure(() -> new ClientWebApplicationException(500)));

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

	}


	@Test
	void testInitSession_taxCode_500_integrationError_saveSession() {

		CreateSessionRequest requestBody = new CreateSessionRequest();
		requestBody.setTaxCode(SessionTestData.CF_MARIO_ROSSI);

		CheckResponse termsAndCondsKO = new CheckResponse();
		termsAndCondsKO.setOutcome(Outcome.OK.toString());

		GetSaveNewCardsFlagRequest saveNewCardsTrue = new GetSaveNewCardsFlagRequest();
		saveNewCardsTrue.setSaveNewCards(true);

		Mockito
				.when(termsAndCondsService.check(Mockito.eq(SessionTestData.CF_MARIO_ROSSI), Mockito.any(CommonHeader.class)))
				.thenReturn(Uni.createFrom().item(termsAndCondsKO));

		Mockito
				.when(pmWalletService.getSaveNewCardsFlag(SessionTestData.CF_MARIO_ROSSI))
				.thenReturn(Uni.createFrom().item(saveNewCardsTrue));

		Mockito
				.when(sessionService.set(Mockito.anyString(), Mockito.any()))
				.thenReturn(Uni.createFrom().failure(() -> new ClientWebApplicationException(500)));

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
		Assertions.assertEquals(true, response.jsonPath().getList("errors").contains(ErrorCode.REDIS_ERROR_WHILE_SAVING_SESSION));
		Assertions.assertNull(response.jsonPath().getJsonObject("outcome"));
		Assertions.assertNull(response.jsonPath().getJsonObject("saveNewCards"));
		Assertions.assertNull(response.jsonPath().getJsonObject("pairingToken"));

	}

}