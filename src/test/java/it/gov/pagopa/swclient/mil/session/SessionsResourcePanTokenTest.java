package it.gov.pagopa.swclient.mil.session;

import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.smallrye.mutiny.Uni;
import it.gov.pagopa.swclient.mil.bean.CommonHeader;
import it.gov.pagopa.swclient.mil.session.bean.CreateSessionRequest;
import it.gov.pagopa.swclient.mil.session.bean.pmwallet.RetrieveTaxCodeResponse;
import it.gov.pagopa.swclient.mil.session.bean.Outcome;
import it.gov.pagopa.swclient.mil.session.bean.pmwallet.PresaveRequest;
import it.gov.pagopa.swclient.mil.session.bean.pmwallet.GetSaveNewCardsFlagRequest;
import it.gov.pagopa.swclient.mil.session.bean.termsandconds.CheckResponse;
import it.gov.pagopa.swclient.mil.session.client.PMWalletService;
import it.gov.pagopa.swclient.mil.session.client.TermsAndConditionsService;
import it.gov.pagopa.swclient.mil.session.dao.SessionService;
import it.gov.pagopa.swclient.mil.session.resource.SessionsResource;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.resteasy.reactive.ClientWebApplicationException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.mockito.Mockito;

import static io.restassured.RestAssured.given;


@QuarkusTest
@TestHTTPEndpoint(SessionsResource.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SessionsResourcePanTokenTest {


	@BeforeAll
	public void setup() {

		Mockito
			.when(sessionService.set(Mockito.anyString(), Mockito.any()))
			.thenReturn(Uni.createFrom().voidItem());
			
	}

	@InjectMock
    @RestClient 
    TermsAndConditionsService termsAndCondsService;
	
	@InjectMock
    @RestClient 
    PMWalletService pmWalletService;
	
	@InjectMock
	SessionService sessionService;

	@Test
	void testInitSession_panToken_201_accepted_saveCards() {
		
		CreateSessionRequest requestBody = new CreateSessionRequest();
		requestBody.setPanToken(SessionTestData.PAN_MARIO_ROSSI);

		RetrieveTaxCodeResponse retrieveTaxCodeResponse = new RetrieveTaxCodeResponse();
		retrieveTaxCodeResponse.setTaxCode(SessionTestData.CF_MARIO_ROSSI);

		CheckResponse termsAndCondsOk = new CheckResponse();
		termsAndCondsOk.setOutcome(Outcome.OK.toString());

		GetSaveNewCardsFlagRequest saveNewCardsTrue = new GetSaveNewCardsFlagRequest();
		saveNewCardsTrue.setSaveNewCards(true);

		Mockito
				.when(pmWalletService.retrieveTaxCode(SessionTestData.PAN_MARIO_ROSSI))
				.thenReturn(Uni.createFrom().item(retrieveTaxCodeResponse));

		Mockito
				.when(termsAndCondsService.check(Mockito.eq(SessionTestData.CF_MARIO_ROSSI), Mockito.any(CommonHeader.class)))
				.thenReturn(Uni.createFrom().item(termsAndCondsOk));
		
		Mockito
				.when(pmWalletService.getSaveNewCardsFlag(SessionTestData.CF_MARIO_ROSSI))
				.thenReturn(Uni.createFrom().item(saveNewCardsTrue));

		Mockito
				.when(pmWalletService.presave(Mockito.any(PresaveRequest.class)))
				.thenReturn(Uni.createFrom().item(javax.ws.rs.core.Response.ok().build()));

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
	void testInitSession_panToken_201_accepted_notSaveCards() {

		CreateSessionRequest requestBody = new CreateSessionRequest();
		requestBody.setPanToken(SessionTestData.PAN_LUIGI_ROSSI);

		RetrieveTaxCodeResponse retrieveTaxCodeResponse = new RetrieveTaxCodeResponse();
		retrieveTaxCodeResponse.setTaxCode(SessionTestData.CF_LUIGI_ROSSI);

		CheckResponse termsAndCondsOk = new CheckResponse();
		termsAndCondsOk.setOutcome(Outcome.OK.toString());

		GetSaveNewCardsFlagRequest saveNewCardsFalse = new GetSaveNewCardsFlagRequest();
		saveNewCardsFalse.setSaveNewCards(false);

		Mockito
				.when(pmWalletService.retrieveTaxCode(SessionTestData.PAN_LUIGI_ROSSI))
				.thenReturn(Uni.createFrom().item(retrieveTaxCodeResponse));

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
		Assertions.assertNull(response.jsonPath().getJsonObject("saveNewCards"));
		Assertions.assertNotNull(response.header("Location"));
		Assertions.assertNull(response.jsonPath().getJsonObject("pairingToken"));

	}

	@Test
	void testInitSession_panToken_201_notAccepted() {

		CreateSessionRequest requestBody = new CreateSessionRequest();
		requestBody.setPanToken(SessionTestData.PAN_MARIO_VERDI);

		RetrieveTaxCodeResponse retrieveTaxCodeResponse = new RetrieveTaxCodeResponse();
		retrieveTaxCodeResponse.setTaxCode(SessionTestData.CF_MARIO_VERDI);

		CheckResponse termsAndCondsKO = new CheckResponse();
		termsAndCondsKO.setOutcome(Outcome.TERMS_AND_CONDITIONS_NOT_YET_ACCEPTED.toString());

		Mockito
				.when(pmWalletService.retrieveTaxCode(SessionTestData.PAN_MARIO_VERDI))
				.thenReturn(Uni.createFrom().item(retrieveTaxCodeResponse));

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
		Assertions.assertNotNull(response.header("Location"));
		Assertions.assertNull(response.jsonPath().getJsonObject("pairingToken"));

	}

	@Test
	void testInitSession_panToken_400_validation() {

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
	void testInitSession_panToken_500_integrationError_getTaxCode() {

		CreateSessionRequest requestBody = new CreateSessionRequest();
		requestBody.setPanToken(SessionTestData.PAN_MARIO_ROSSI);

		Mockito
				.when(pmWalletService.retrieveTaxCode(SessionTestData.PAN_MARIO_ROSSI))
				.thenReturn(Uni.createFrom().failure(() -> new ClientWebApplicationException()));

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
	void testInitSession_panToken_204_getTaxCode_notFound() {

		CreateSessionRequest requestBody = new CreateSessionRequest();
		requestBody.setPanToken(SessionTestData.PAN_MARIO_ROSSI);

		Mockito
				.when(pmWalletService.retrieveTaxCode(SessionTestData.PAN_MARIO_ROSSI))
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

		Assertions.assertEquals(202, response.statusCode());
		Assertions.assertEquals(Outcome.PAIR_WITH_IO.toString(), response.jsonPath().getJsonObject("outcome"));
		Assertions.assertNull(response.jsonPath().getJsonObject("saveNewCards"));
		Assertions.assertNotNull(response.jsonPath().getJsonObject("pairingToken"));
		Assertions.assertTrue(response.header("Location") != null &&
						response.header("Location").endsWith("/sessions?pairingToken="+response.jsonPath().getJsonObject("pairingToken")));
		Assertions.assertNotNull(response.header("Max-Retries"));
		Assertions.assertNotNull(response.header("Retry-After"));

	}

	@Test
	void testInitSession_panToken_500_integrationError_termsAndConditions() {

		CreateSessionRequest requestBody = new CreateSessionRequest();
		requestBody.setPanToken(SessionTestData.PAN_MARIO_ROSSI);

		RetrieveTaxCodeResponse retrieveTaxCodeResponse = new RetrieveTaxCodeResponse();
		retrieveTaxCodeResponse.setTaxCode(SessionTestData.CF_MARIO_ROSSI);

		Mockito
				.when(pmWalletService.retrieveTaxCode(SessionTestData.PAN_MARIO_ROSSI))
				.thenReturn(Uni.createFrom().item(retrieveTaxCodeResponse));

		Mockito
				.when(termsAndCondsService.check(Mockito.anyString(), Mockito.any(CommonHeader.class)))
				.thenReturn(Uni.createFrom().failure(() -> new ClientWebApplicationException()));

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
	void testInitSession_panToken_201_notFound_termsAndConditions() {

		CreateSessionRequest requestBody = new CreateSessionRequest();
		requestBody.setPanToken(SessionTestData.PAN_MARIO_ROSSI);

		RetrieveTaxCodeResponse retrieveTaxCodeResponse = new RetrieveTaxCodeResponse();
		retrieveTaxCodeResponse.setTaxCode(SessionTestData.CF_MARIO_ROSSI);

		Mockito
				.when(pmWalletService.retrieveTaxCode(SessionTestData.PAN_MARIO_ROSSI))
				.thenReturn(Uni.createFrom().item(retrieveTaxCodeResponse));

		Mockito
				.when(termsAndCondsService.check(Mockito.anyString(), Mockito.any(CommonHeader.class)))
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
		Assertions.assertEquals(Outcome.TERMS_AND_CONDITIONS_NOT_YET_ACCEPTED.toString(), response.jsonPath().getJsonObject("outcome"));
		Assertions.assertNull(response.jsonPath().getJsonObject("saveNewCards"));
		Assertions.assertNull(response.jsonPath().getJsonObject("pairingToken"));

	}

	@Test
	void testInitSession_panToken_201_integrationError_getSaveNewCards() {

		CreateSessionRequest requestBody = new CreateSessionRequest();
		requestBody.setPanToken(SessionTestData.PAN_MARIO_ROSSI);

		RetrieveTaxCodeResponse retrieveTaxCodeResponse = new RetrieveTaxCodeResponse();
		retrieveTaxCodeResponse.setTaxCode(SessionTestData.CF_MARIO_ROSSI);

		CheckResponse termsAndCondsKO = new CheckResponse();
		termsAndCondsKO.setOutcome(Outcome.OK.toString());

		Mockito
				.when(pmWalletService.retrieveTaxCode(SessionTestData.PAN_MARIO_ROSSI))
				.thenReturn(Uni.createFrom().item(retrieveTaxCodeResponse));

		Mockito
				.when(termsAndCondsService.check(Mockito.eq(SessionTestData.CF_MARIO_ROSSI), Mockito.any(CommonHeader.class)))
				.thenReturn(Uni.createFrom().item(termsAndCondsKO));

		Mockito
				.when(pmWalletService.getSaveNewCardsFlag(Mockito.anyString()))
				.thenReturn(Uni.createFrom().failure(() -> new ClientWebApplicationException()));

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
		Assertions.assertNull(response.jsonPath().getJsonObject("pairingToken"));
	}

	@Test
	void testInitSession_panToken_201_integrationError_saveCard() {

		CreateSessionRequest requestBody = new CreateSessionRequest();
		requestBody.setPanToken(SessionTestData.PAN_MARIO_ROSSI);

		RetrieveTaxCodeResponse retrieveTaxCodeResponse = new RetrieveTaxCodeResponse();
		retrieveTaxCodeResponse.setTaxCode(SessionTestData.CF_MARIO_ROSSI);

		CheckResponse termsAndCondsKO = new CheckResponse();
		termsAndCondsKO.setOutcome(Outcome.OK.toString());

		GetSaveNewCardsFlagRequest saveNewCardsTrue = new GetSaveNewCardsFlagRequest();
		saveNewCardsTrue.setSaveNewCards(true);

		Mockito
				.when(pmWalletService.retrieveTaxCode(SessionTestData.PAN_MARIO_ROSSI))
				.thenReturn(Uni.createFrom().item(retrieveTaxCodeResponse));

		Mockito
				.when(termsAndCondsService.check(Mockito.eq(SessionTestData.CF_MARIO_ROSSI), Mockito.any(CommonHeader.class)))
				.thenReturn(Uni.createFrom().item(termsAndCondsKO));

		Mockito
				.when(pmWalletService.getSaveNewCardsFlag(SessionTestData.CF_MARIO_ROSSI))
				.thenReturn(Uni.createFrom().item(saveNewCardsTrue));

		Mockito
				.when(pmWalletService.presave(Mockito.any(PresaveRequest.class)))
				.thenReturn(Uni.createFrom().failure(() -> new ClientWebApplicationException()));

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
		Assertions.assertNull(response.jsonPath().getJsonObject("pairingToken"));

	}

	@Test
	void testInitSession_panToken_500_integrationError_saveSession() {

		CreateSessionRequest requestBody = new CreateSessionRequest();
		requestBody.setPanToken(SessionTestData.PAN_MARIO_ROSSI);

		RetrieveTaxCodeResponse retrieveTaxCodeResponse = new RetrieveTaxCodeResponse();
		retrieveTaxCodeResponse.setTaxCode(SessionTestData.CF_MARIO_ROSSI);

		CheckResponse termsAndCondsKO = new CheckResponse();
		termsAndCondsKO.setOutcome(Outcome.OK.toString());

		GetSaveNewCardsFlagRequest saveNewCardsTrue = new GetSaveNewCardsFlagRequest();
		saveNewCardsTrue.setSaveNewCards(true);

		Mockito
				.when(pmWalletService.retrieveTaxCode(SessionTestData.PAN_MARIO_ROSSI))
				.thenReturn(Uni.createFrom().item(retrieveTaxCodeResponse));

		Mockito
				.when(termsAndCondsService.check(Mockito.anyString(), Mockito.any(CommonHeader.class)))
				.thenReturn(Uni.createFrom().item(termsAndCondsKO));

		Mockito
				.when(pmWalletService.getSaveNewCardsFlag(SessionTestData.CF_MARIO_ROSSI))
				.thenReturn(Uni.createFrom().item(saveNewCardsTrue));

		Mockito
				.when(pmWalletService.presave(Mockito.any(PresaveRequest.class)))
				.thenReturn(Uni.createFrom().item(javax.ws.rs.core.Response.ok().build()));

		Mockito
				.when(sessionService.set(Mockito.anyString(), Mockito.any()))
				.thenReturn(Uni.createFrom().failure(() -> new RuntimeException()));

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