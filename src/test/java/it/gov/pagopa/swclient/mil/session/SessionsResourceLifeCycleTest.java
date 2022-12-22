package it.gov.pagopa.swclient.mil.session;

import io.quarkus.test.common.http.TestHTTPEndpoint;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.mockito.InjectMock;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.smallrye.mutiny.Uni;
import it.gov.pagopa.swclient.mil.session.bean.Outcome;
import it.gov.pagopa.swclient.mil.session.dao.Session;
import it.gov.pagopa.swclient.mil.session.dao.SessionService;
import it.gov.pagopa.swclient.mil.session.resource.SessionsResource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.mockito.Mockito;

import javax.ws.rs.NotFoundException;
import java.util.UUID;

import static io.restassured.RestAssured.given;

@QuarkusTest
@TestHTTPEndpoint(SessionsResource.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SessionsResourceLifeCycleTest {

    final static String CF_MARIO_ROSSI = "RSSMRA80A01H501U"; // accepted, save cards
    final static String CF_LUIGI_ROSSI = "RSSLGU80A01H501U"; // accepted, not save cards

    final static String CF_MARIO_VERDI = "VRDMRA80A01H501Q"; // not accepted

    @InjectMock
    SessionService sessionService;

    @Test
    void testGetSession_200_accepted_saveCards() {

        String sessionId = UUID.randomUUID().toString();

        Session session = new Session();
        session.setTaxCode(CF_MARIO_ROSSI);
        session.setTermsAndConditionAccepted(true);
        session.setSaveNewCards(true);

        Mockito
                .when(sessionService.get(sessionId))
                .thenReturn(Uni.createFrom().item(session));

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

        Assertions.assertEquals(200, response.statusCode());
        Assertions.assertEquals(Outcome.OK.toString(), response.jsonPath().getString("outcome"));
        Assertions.assertEquals(CF_MARIO_ROSSI, response.jsonPath().getString("taxCode"));
        Assertions.assertEquals(true, response.jsonPath().getBoolean("saveNewCards"));

    }

    @Test
    void testGetSession_200_accepted_notSaveCards() {

        String sessionId = UUID.randomUUID().toString();

        Session session = new Session();
        session.setTaxCode(CF_LUIGI_ROSSI);
        session.setTermsAndConditionAccepted(true);
        session.setSaveNewCards(false);

        Mockito
                .when(sessionService.get(sessionId))
                .thenReturn(Uni.createFrom().item(session));

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

        Assertions.assertEquals(200, response.statusCode());
        Assertions.assertEquals(Outcome.OK.toString(), response.jsonPath().getString("outcome"));
        Assertions.assertEquals(CF_LUIGI_ROSSI, response.jsonPath().getString("taxCode"));
        Assertions.assertEquals(false, response.jsonPath().getBoolean("saveNewCards"));

    }

    @Test
    void testGetSession_200_notAccepted() {

        String sessionId = UUID.randomUUID().toString();

        Session session = new Session();
        session.setTaxCode(CF_MARIO_VERDI);
        session.setTermsAndConditionAccepted(false);

        Mockito
                .when(sessionService.get(sessionId))
                .thenReturn(Uni.createFrom().item(session));

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

        Assertions.assertEquals(200, response.statusCode());
        Assertions.assertEquals(Outcome.TERMS_AND_CONDITIONS_NOT_YET_ACCEPTED.toString(), response.jsonPath().getString("outcome"));
        Assertions.assertEquals(CF_MARIO_VERDI, response.jsonPath().getString("taxCode"));
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

        String sessionId = UUID.randomUUID().toString();

        Mockito
                .when(sessionService.get(sessionId))
                .thenReturn(Uni.createFrom().nullItem());

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

        Assertions.assertEquals(404, response.statusCode());
        Assertions.assertTrue(response.jsonPath().getList("errors").contains(ErrorCode.REDIS_ERROR_SESSION_NOT_FOUND));
        Assertions.assertNull(response.jsonPath().getJsonObject("outcome"));
        Assertions.assertNull(response.jsonPath().getJsonObject("taxCode"));
        Assertions.assertNull(response.jsonPath().getJsonObject("saveNewCards"));

    }

    @Test
    void testGetSession_500_integrationError() {

        String sessionId = UUID.randomUUID().toString();

        Mockito
                .when(sessionService.get(sessionId))
                .thenReturn(Uni.createFrom().failure(new RuntimeException()));

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

        Assertions.assertEquals(500, response.statusCode());
        Assertions.assertTrue(response.jsonPath().getList("errors").contains(ErrorCode.REDIS_ERROR_WHILE_RETRIEVING_SESSION));
        Assertions.assertNull(response.jsonPath().getJsonObject("outcome"));
        Assertions.assertNull(response.jsonPath().getJsonObject("taxCode"));
        Assertions.assertNull(response.jsonPath().getJsonObject("saveNewCards"));

    }


    @Test
    void testPatchSession_200_accepted_saveCards() {

        String sessionId = UUID.randomUUID().toString();

        Session savedSession = new Session();
        savedSession.setTaxCode(CF_MARIO_VERDI);
        savedSession.setTermsAndConditionAccepted(false);

        Session updatedSession = new Session();
        updatedSession.setTermsAndConditionAccepted(true);
        updatedSession.setSaveNewCards(Boolean.TRUE);

        Mockito
                .when(sessionService.get(sessionId))
                .thenReturn(Uni.createFrom().item(savedSession));

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

        Assertions.assertEquals(202, response.statusCode());
        Assertions.assertEquals("ACCEPTED", response.jsonPath().getString("outcome"));
        Assertions.assertNull(response.jsonPath().getJsonObject("taxCode"));
        Assertions.assertNull(response.jsonPath().getJsonObject("saveNewCards"));

    }

    @Test
    void testPatchSession_400_validation() {

        String sessionId = UUID.randomUUID().toString();
        sessionId = sessionId.replaceAll("-", "");

        Session updatedSession = new Session();
        updatedSession.setTermsAndConditionAccepted(true);
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
    void testPatchSession_404_notFound() {

        String sessionId = UUID.randomUUID().toString();

        Session updatedSession = new Session();
        updatedSession.setTermsAndConditionAccepted(true);
        updatedSession.setSaveNewCards(Boolean.TRUE);

        Mockito
                .when(sessionService.get(sessionId))
                .thenReturn(Uni.createFrom().nullItem());

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

        Assertions.assertEquals(404, response.statusCode());
        Assertions.assertTrue(response.jsonPath().getList("errors").contains(ErrorCode.REDIS_ERROR_SESSION_NOT_FOUND));
        Assertions.assertNull(response.jsonPath().getJsonObject("outcome"));
        Assertions.assertNull(response.jsonPath().getJsonObject("taxCode"));
        Assertions.assertNull(response.jsonPath().getJsonObject("saveNewCards"));

    }

    @Test
    void testPatchSession_500_integrationError_get() {

        String sessionId = UUID.randomUUID().toString();

        Session updatedSession = new Session();
        updatedSession.setTermsAndConditionAccepted(true);
        updatedSession.setSaveNewCards(Boolean.TRUE);

        Mockito
                .when(sessionService.get(sessionId))
                .thenReturn(Uni.createFrom().failure(new RuntimeException()));

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

        Assertions.assertEquals(500, response.statusCode());
        Assertions.assertTrue(response.jsonPath().getList("errors").contains(ErrorCode.REDIS_ERROR_WHILE_RETRIEVING_SESSION));
        Assertions.assertNull(response.jsonPath().getJsonObject("outcome"));
        Assertions.assertNull(response.jsonPath().getJsonObject("taxCode"));
        Assertions.assertNull(response.jsonPath().getJsonObject("saveNewCards"));

    }

    @Test
    void testPatchSession_500_integrationError_set() {

        String sessionId = UUID.randomUUID().toString();

        Session updatedSession = new Session();
        updatedSession.setTermsAndConditionAccepted(true);
        updatedSession.setSaveNewCards(Boolean.TRUE);

        Session session = new Session();
        session.setTaxCode(CF_MARIO_VERDI);
        session.setTermsAndConditionAccepted(false);

        Mockito
                .when(sessionService.get(sessionId))
                .thenReturn(Uni.createFrom().item(session));

        Mockito
                .when(sessionService.set(Mockito.eq(sessionId), Mockito.any(Session.class)))
                .thenReturn(Uni.createFrom().failure(new RuntimeException()));

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

        Assertions.assertEquals(500, response.statusCode());
        Assertions.assertTrue(response.jsonPath().getList("errors").contains(ErrorCode.REDIS_ERROR_WHILE_SAVING_SESSION));
        Assertions.assertNull(response.jsonPath().getJsonObject("outcome"));
        Assertions.assertNull(response.jsonPath().getJsonObject("taxCode"));
        Assertions.assertNull(response.jsonPath().getJsonObject("saveNewCards"));

    }

    @Test
    void testDeleteSession_204() {

        String sessionId = UUID.randomUUID().toString();

        Session deletedSession = new Session();
        deletedSession.setTaxCode(CF_MARIO_VERDI);
        deletedSession.setTermsAndConditionAccepted(true);
        deletedSession.setSaveNewCards(Boolean.TRUE);

        Mockito
                .when(sessionService.getdel(sessionId))
                .thenReturn(Uni.createFrom().item(deletedSession));

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

        Assertions.assertEquals(204, response.statusCode());
        Assertions.assertEquals("", response.body().asString());

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

        String sessionId = UUID.randomUUID().toString();

        Mockito
                .when(sessionService.getdel(sessionId))
                .thenReturn(Uni.createFrom().nullItem());

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

        Assertions.assertEquals(404, response.statusCode());
        Assertions.assertTrue(response.jsonPath().getList("errors").contains(ErrorCode.REDIS_ERROR_SESSION_NOT_FOUND));

    }

    @Test
    void testDeleteSession_500_integrationError() {

        String sessionId = UUID.randomUUID().toString();

        Mockito
                .when(sessionService.getdel(sessionId))
                .thenReturn(Uni.createFrom().failure(new RuntimeException()));

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

        Assertions.assertEquals(500, response.statusCode());
        Assertions.assertTrue(response.jsonPath().getList("errors").contains(ErrorCode.REDIS_ERROR_WHILE_DELETING_SESSION));

    }

}
