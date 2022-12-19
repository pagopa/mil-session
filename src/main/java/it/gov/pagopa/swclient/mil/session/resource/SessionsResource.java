package it.gov.pagopa.swclient.mil.session.resource;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.ws.rs.BeanParam;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.PATCH;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.eclipse.microprofile.rest.client.inject.RestClient;

import io.quarkus.logging.Log;
import io.smallrye.mutiny.Uni;
import it.gov.pagopa.swclient.mil.bean.CommonHeader;
import it.gov.pagopa.swclient.mil.bean.Errors;
import it.gov.pagopa.swclient.mil.session.ErrorCode;
import it.gov.pagopa.swclient.mil.session.bean.SaveCardRequest;
import it.gov.pagopa.swclient.mil.session.bean.InitSessionResponse;
import it.gov.pagopa.swclient.mil.session.bean.SaveNewCardsResponse;
import it.gov.pagopa.swclient.mil.session.bean.InitSessionRequest;
import it.gov.pagopa.swclient.mil.session.client.PMWalletService;
import it.gov.pagopa.swclient.mil.session.client.TermsAndConditionsService;
import it.gov.pagopa.swclient.mil.session.dao.Session;
import it.gov.pagopa.swclient.mil.session.dao.SessionService;

@Path("/sessions")
public class SessionsResource {

	@Inject
	private SessionService sessionService;

	@RestClient
	private TermsAndConditionsService termsAndConsService;
	
	@RestClient
	private PMWalletService pmWalletService;
	
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	public Uni<Response> initSession(@Valid @BeanParam CommonHeader commonHeader, @Valid InitSessionRequest userId) {
		Log.debugf("initSession - Input parameters: %s, %s", commonHeader, userId);
				
		return termsAndConsService.getTCByTaxCode(userId.getTaxCode(), commonHeader)
				.onFailure().transform(t -> {	
					Log.errorf(t, "[%s] Error while retrieving terms and condition", ErrorCode.ERROR_CALLING_TERMS_AND_CONDITIONS_SERVICE);
					return new InternalServerErrorException(Response
						.status(Status.INTERNAL_SERVER_ERROR)
						.entity(new Errors(List.of(ErrorCode.ERROR_CALLING_TERMS_AND_CONDITIONS_SERVICE)))
						.build());
				})
				.map(tc -> {
					InitSessionResponse response = new InitSessionResponse();
					response.setOutcome(tc.getOutcome());
					return response;
				})
				.chain(s -> retrieveSaveNewCardFlag(s, userId.getTaxCode(), userId.getPanToken()!=null))
				.call(s -> storeCard(userId.getTaxCode(), userId.getPanToken()!=null, userId.getPanToken()))
				.chain(s -> storeAndReturnSession(s, userId.getTaxCode()));
		
	}
	
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Path("/{id}")
	public Uni<Response> getSession(@Valid @BeanParam CommonHeader commonHeader, @PathParam(value = "id") String sessionId) {
		Log.debugf("getSession - Input parameters: %s %s", commonHeader, sessionId);

		return retrieveSession(sessionId)
			.map(e -> {
				Log.debugf("getSession - Output parameters: %s", e);
				return Response.ok(e).build();
			});
	}

	
	@PATCH
	@Path("/{id}")
	public Uni<Response> patchSession(@Valid @BeanParam CommonHeader commonHeader, @PathParam(value = "id") String sessionId, Session dataToUpdate) {
		Log.debugf("patchSession - Input parameters: %s %s %s", commonHeader, sessionId, dataToUpdate);

		return retrieveSession(sessionId)
			.chain(s -> updateAndReturnSession(sessionId, s, dataToUpdate));
	}

	@DELETE
	@Path("/{id}")
	public Uni<Response> deleteSession(@Valid @BeanParam CommonHeader commonHeader, @PathParam(value = "id") String sessionId) {
		Log.debugf("deleteSession - Input parameters: %s %s", commonHeader, sessionId);

		return sessionService.getdel(sessionId)
			.onFailure().transform(t -> {
				Log.errorf(t, "[%s] REDIS error while deleting session", ErrorCode.REDIS_ERROR_WHILE_RETRIEVING_SESSION);
				return new InternalServerErrorException(Response
					.status(Status.INTERNAL_SERVER_ERROR)
					.entity(new Errors(List.of(ErrorCode.REDIS_ERROR_WHILE_RETRIEVING_SESSION)))
					.build());
			})
			.onItem().ifNull().failWith(() ->{
					Log.errorf("[%s] REDIS session not found", ErrorCode.REDIS_ERROR_WHILE_RETRIEVING_SESSION);
					return new NotFoundException(Response
						.status(Status.NOT_FOUND)
						.entity(new Errors(List.of(ErrorCode.REDIS_ERROR_WHILE_RETRIEVING_SESSION)))
						.build());
			})
			.map(e -> {
				Log.debugf("deleteSession - Output parameters: %s", e);
				return Response.noContent().build();
			});
	}
	
	private Uni<Session> retrieveSession(String sessionId) {
		return sessionService.get(sessionId)
			.onFailure().transform(t -> {
				Log.errorf(t, "[%s] REDIS error while retrieving session", ErrorCode.REDIS_ERROR_WHILE_RETRIEVING_SESSION);
				return new InternalServerErrorException(Response
					.status(Status.INTERNAL_SERVER_ERROR)
					.entity(new Errors(List.of(ErrorCode.REDIS_ERROR_WHILE_RETRIEVING_SESSION)))
					.build());
			})
			.onItem().ifNull().failWith(() ->{
					Log.errorf("[%s] REDIS session not found", ErrorCode.REDIS_ERROR_WHILE_RETRIEVING_SESSION);
					return new NotFoundException(Response
						.status(Status.NOT_FOUND)
						.entity(new Errors(List.of(ErrorCode.REDIS_ERROR_WHILE_RETRIEVING_SESSION)))
						.build());
			});
	}
	
	private Uni<Response> storeAndReturnSession(InitSessionResponse response, String taxCode) {
		
		String sessionId = UUID.randomUUID().toString();
		
		Session session = new Session();
		session.setTaxCode(taxCode);
		session.setTermsAndConditionAccepted("OK".equals(response.getOutcome()));
		session.setSaveNewCards(response.isSaveNewCards());
		
		return sessionService.set(sessionId, session)
				.onFailure().transform(t -> {
					Log.errorf(t, "[%s] REDIS error saving session in cache", ErrorCode.REDIS_ERROR_WHILE_SAVING_SESSION);
					return new InternalServerErrorException(Response
						.status(Status.INTERNAL_SERVER_ERROR)
						.entity(new Errors(List.of(ErrorCode.REDIS_ERROR_WHILE_SAVING_SESSION)))
						.build());
				})
				.map(c -> {					
					URI location = URI.create("/services/" + sessionId);
					Log.debugf("createSession - Output parameters: %s, %s", location, response);
					return Response.status(Status.CREATED).entity(response).location(location).build();
				});
		
	}
	
	private Uni<Response> updateAndReturnSession(String sessionId, Session currentSession, Session dataToUpdate) {
		
		currentSession.setTermsAndConditionAccepted(dataToUpdate.isTermsAndConditionAccepted());
		currentSession.setSaveNewCards(dataToUpdate.isTermsAndConditionAccepted());
		
		return sessionService.set(sessionId, currentSession)
				.onFailure().transform(t -> {
					Log.errorf(t, "[%s] REDIS error saving session in cache", ErrorCode.REDIS_ERROR_WHILE_SAVING_SESSION);
					return new InternalServerErrorException(Response
						.status(Status.INTERNAL_SERVER_ERROR)
						.entity(new Errors(List.of(ErrorCode.REDIS_ERROR_WHILE_SAVING_SESSION)))
						.build());
				})
				.map(c -> {			
					URI location = URI.create("/services/" + sessionId);
					InitSessionResponse createSessionResponse = new InitSessionResponse();
					createSessionResponse.setOutcome("OK");
					Log.debugf("createSession - Output parameters: %s, %s", location, createSessionResponse);
					return Response.accepted(createSessionResponse).build();
				});
		
	}
	
	private Uni<InitSessionResponse> retrieveSaveNewCardFlag(InitSessionResponse session, String taxCode, boolean hasPanToken) {
		return pmWalletService
				.getSaveNewCards(taxCode)
				.onFailure().recoverWithItem( t -> {
					Log.errorf(t, "[%s] Error while retrieving save new cards flag", ErrorCode.ERROR_CALLING_GET_SAVE_NEW_CARDS_SERVICE);
					return new SaveNewCardsResponse();
				} )
				.map(snc -> {
					if (!hasPanToken) {
						session.setSaveNewCards(snc.isSaveNewCards());
					}
					return session;
				});
	}
	
	
	private Uni<Boolean> storeCard(String taxCode, boolean hasPanToken, String panToken) {
		if (hasPanToken) {
			SaveCardRequest card = new SaveCardRequest();
			card.setPanToken(panToken);
			card.setTaxCode(taxCode);
			
			return pmWalletService.saveCard(card)
					.onFailure().recoverWithNull()
					.onItem().transform(r -> { return Boolean.TRUE; } );
		}
		else {
			return Uni.createFrom().item(() -> Boolean.FALSE);
		}
	}
	
}
