package it.gov.pagopa.swclient.mil.session.it;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusIntegrationTest;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import it.gov.pagopa.swclient.mil.session.ErrorCode;
import it.gov.pagopa.swclient.mil.session.it.resource.EnvironmentTestResource;
import it.gov.pagopa.swclient.mil.session.bean.CreateSessionRequest;
import it.gov.pagopa.swclient.mil.session.bean.Outcome;
import it.gov.pagopa.swclient.mil.session.it.resource.RedisTestResource;
import it.gov.pagopa.swclient.mil.session.resource.SessionsResource;
import it.gov.pagopa.swclient.mil.session.it.resource.WiremockTestResource;
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
class SessionResourceTaxCodeTestIT {


    final static String CF_MARIO_ROSSI = "RSSMRA80A01H501U"; // accepted, save cards
    final static String CF_LUIGI_ROSSI = "RSSLGU80A01H501U"; // accepted, not save cards
    final static String CF_GIOVANNI_ROSSI = "RSSGNN80A01H501N"; // accepted, 500

    final static String CF_MARIO_VERDI = "VRDMRA80A01H501Q"; // never accepted
    final static String CF_LUIGI_VERDI = "VRDLGU80A01H501Q"; // accepted but expired

    final static String CF_MARIO_BIANCHI = "BNCMRA80A01H501A"; // 500


    @Test
    void testCreateSession_taxCode_201_accepted_saveCards() {

        CreateSessionRequest requestBody = new CreateSessionRequest();
        requestBody.setTaxCode(CF_MARIO_ROSSI);

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
    void testCreateSession_taxCode_201_accepted_notSaveCards() {

        CreateSessionRequest requestBody = new CreateSessionRequest();
        requestBody.setTaxCode(CF_LUIGI_ROSSI);

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
    void testCreateSession_taxCode_201_notAccepted() {

        CreateSessionRequest requestBody = new CreateSessionRequest();
        requestBody.setTaxCode(CF_LUIGI_VERDI);

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
        requestBody.setTaxCode(CF_MARIO_VERDI);

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
        Assertions.assertNull(response.jsonPath().getJsonObject("pairingToken"));

    }

    @Test
    void testCreateSession_taxCode_500_integrationError_termsAndConditions() {

        CreateSessionRequest requestBody = new CreateSessionRequest();
        requestBody.setTaxCode(CF_MARIO_BIANCHI);

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
    void testCreateSession_taxCode_201_accepted_integrationError_getSaveNewCards() {

        CreateSessionRequest requestBody = new CreateSessionRequest();
        requestBody.setTaxCode(CF_GIOVANNI_ROSSI);

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
