package it.gov.pagopa.swclient.mil.session;

import static io.restassured.RestAssured.given;

import org.eclipse.microprofile.rest.client.inject.RestClient;
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
import it.gov.pagopa.swclient.mil.bean.CommonHeader;
import it.gov.pagopa.swclient.mil.session.bean.SaveCardRequest;
import it.gov.pagopa.swclient.mil.session.bean.SaveNewCardsResponse;
import it.gov.pagopa.swclient.mil.session.bean.TermsAndConditionsResponse;
import it.gov.pagopa.swclient.mil.session.bean.InitSessionRequest;
import it.gov.pagopa.swclient.mil.session.client.PMWalletService;
import it.gov.pagopa.swclient.mil.session.client.TermsAndConditionsService;
import it.gov.pagopa.swclient.mil.session.dao.SessionService;
import it.gov.pagopa.swclient.mil.session.resource.SessionsResource;


@QuarkusTest
@TestHTTPEndpoint(SessionsResource.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SessionsResourceTest {
	
	final static String CF_MARIO_ROSSI = "RSSMRA80A01H501U"; // accepted, save cards
	final static String CF_LUIGI_ROSSI = "RSSLGU80A01H501U"; // accepted, not save cards
	
	final static String CF_MARIO_VERDI = "VRDMRA80A01H501Q"; // not accepted, save cards
	final static String CF_LUIGI_VERDI = "VRDLGU80A01H501Q"; // not accepted, not save cards
	
	@BeforeAll
	public void setup() {
		
		Mockito
			.when(pmWalletService.saveCard(Mockito.any(SaveCardRequest.class)))
			.thenReturn(Uni.createFrom().item(javax.ws.rs.core.Response.ok().build()));
		
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
	void testCreateSession_taxCode_200_accepted_saveCards() {
		
		InitSessionRequest requestBody = new InitSessionRequest();
		requestBody.setTaxCode(CF_MARIO_ROSSI);
		
		TermsAndConditionsResponse termsAndCondsOk = new TermsAndConditionsResponse();
		termsAndCondsOk.setOutcome("OK");
		
		SaveNewCardsResponse saveNewCardsTrue = new SaveNewCardsResponse();
		saveNewCardsTrue.setSaveNewCards(true);
		
		Mockito
			.when(termsAndCondsService.getTCByTaxCode(Mockito.eq(CF_MARIO_ROSSI), Mockito.any(CommonHeader.class)))
			.thenReturn(Uni.createFrom().item(termsAndCondsOk));
		
		Mockito
			.when(pmWalletService.getSaveNewCards(CF_MARIO_ROSSI))
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
        Assertions.assertEquals("OK", response.jsonPath().getString("outcome"));
        Assertions.assertEquals(true, response.jsonPath().getBoolean("saveNewCards"));
     
	}
	
	@Test
	void testCreateSession_taxCode_200_accepted_notSaveCards() {
		
		InitSessionRequest requestBody = new InitSessionRequest();
		requestBody.setTaxCode(CF_LUIGI_ROSSI);
		
		TermsAndConditionsResponse termsAndCondsOk = new TermsAndConditionsResponse();
		termsAndCondsOk.setOutcome("OK");
		
		SaveNewCardsResponse saveNewCardsFalse = new SaveNewCardsResponse();
		saveNewCardsFalse.setSaveNewCards(false);
		
		Mockito
			.when(termsAndCondsService.getTCByTaxCode(Mockito.eq(CF_LUIGI_ROSSI), Mockito.any(CommonHeader.class)))
			.thenReturn(Uni.createFrom().item(termsAndCondsOk));
		
		Mockito
			.when(pmWalletService.getSaveNewCards(CF_LUIGI_ROSSI))
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
        Assertions.assertEquals("OK", response.jsonPath().getString("outcome"));
        Assertions.assertEquals(false, response.jsonPath().getBoolean("saveNewCards"));
     
	}
	
	@Test
	void testCreateSession_taxCode_200_notAccepted_saveCards() {
		
		InitSessionRequest requestBody = new InitSessionRequest();
		requestBody.setTaxCode(CF_MARIO_VERDI);
		
		TermsAndConditionsResponse termsAndCondsKO = new TermsAndConditionsResponse();
		termsAndCondsKO.setOutcome("TERMS_AND_CONDITIONS_NOT_YET_ACCEPTED");
		
		SaveNewCardsResponse saveNewCardsTrue = new SaveNewCardsResponse();
		saveNewCardsTrue.setSaveNewCards(true);
		
		Mockito
			.when(termsAndCondsService.getTCByTaxCode(Mockito.eq(CF_MARIO_VERDI), Mockito.any(CommonHeader.class)))
			.thenReturn(Uni.createFrom().item(termsAndCondsKO));
	
		Mockito
			.when(pmWalletService.getSaveNewCards(CF_MARIO_VERDI))
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
        Assertions.assertEquals("TERMS_AND_CONDITIONS_NOT_YET_ACCEPTED", response.jsonPath().getString("outcome"));
        Assertions.assertEquals(true, response.jsonPath().getBoolean("saveNewCards"));
     
	}
	
	@Test
	void testCreateSession_taxCode_200_notAccepted_notSaveCards() {
		
		InitSessionRequest requestBody = new InitSessionRequest();
		requestBody.setTaxCode(CF_LUIGI_VERDI);
		
		TermsAndConditionsResponse termsAndCondsKO = new TermsAndConditionsResponse();
		termsAndCondsKO.setOutcome("TERMS_AND_CONDITIONS_NOT_YET_ACCEPTED");
		
		SaveNewCardsResponse saveNewCardsFalse = new SaveNewCardsResponse();
		saveNewCardsFalse.setSaveNewCards(false);
		
		Mockito
			.when(termsAndCondsService.getTCByTaxCode(Mockito.eq(CF_LUIGI_VERDI), Mockito.any(CommonHeader.class)))
			.thenReturn(Uni.createFrom().item(termsAndCondsKO));
		
		Mockito
			.when(pmWalletService.getSaveNewCards(CF_LUIGI_VERDI))
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
        Assertions.assertEquals("TERMS_AND_CONDITIONS_NOT_YET_ACCEPTED", response.jsonPath().getString("outcome"));
        Assertions.assertEquals(false, response.jsonPath().getBoolean("saveNewCards"));
     
	}
	
	
	
//
//	@Test
//	void testGetServices_404() {
//		given()
//			.headers("RequestId", "1de3c885-5584-4910-b43a-4ad6e3fd55f9",
//				"Version", "1.0.0",
//				"AcquirerId", "12345",
//				"Channel", "POS",
//				"TerminalId", "12345678")
//			.when()
//			.get()
//			.then()
//			.statusCode(404);
//	}
}