package it.gov.pagopa.swclient.mil.session.resource;

import java.net.URI;
import java.security.SecureRandom;
import java.util.List;
import java.util.UUID;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.Pattern;
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

import io.smallrye.mutiny.ItemWithContext;
import it.gov.pagopa.swclient.mil.session.bean.GetSessionResponse;
import it.gov.pagopa.swclient.mil.session.bean.GetTaxCodeResponse;
import it.gov.pagopa.swclient.mil.session.bean.Outcome;
import it.gov.pagopa.swclient.mil.session.bean.PatchSessionResponse;
import it.gov.pagopa.swclient.mil.session.bean.TermsAndConditionsResponse;
import org.eclipse.microprofile.config.inject.ConfigProperty;
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

	private static final String CONTEXT_TAX_CODE = "TAX_CODE";
	private static final String CONTEXT_SAVE_NEW_CARDS = "SAVE_NEW_CARDS";

	private static final String SESSION_ID_REGEX = "^[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12}$";

	@Inject
	SessionService sessionService;

	@RestClient
	TermsAndConditionsService termsAndConsService;
	
	@RestClient
	PMWalletService pmWalletService;

	@ConfigProperty(name = "io.pairing.max-retry", defaultValue = "3")
	int ioPairingMaxRetry;

	@ConfigProperty(name = "io.pairing.retry-after", defaultValue = "60")
	int ioPairingRetryAfter;

	/**
	 * API to initialize a session with the multilayer integration channel, from one of the enabled channels.
	 * It accepts in input the tax code of the user or a tokenized pan of the user card.
	 * If the tokenized pan is passed in request, it will try to recover the tax code from the PM wallet of PagoPA.
	 * If no tax code can be retrieved it will return a pairing token to try to identify the user through the IO app.
	 * @param commonHeader a set of mandatory headers
	 * @param initSessionRequest the request containing the tax code or the tokenized pan of the card
	 * @return an {@link InitSessionResponse} instance containing the outcome of session initialization
	 */
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Uni<Response> initSession(@Valid @BeanParam CommonHeader commonHeader, @Valid InitSessionRequest initSessionRequest) {
		Log.debugf("initSession - Input parameters: %s, %s", commonHeader, initSessionRequest);

		return Uni.createFrom()
				.item(initSessionRequest.getTaxCode())
				.onItem().ifNull().switchTo(() -> {
					// if the tokenized pan is passed in request, retrieve tax code from PM wallet
					Log.debugf("Calling PMWallet - retrieve Tax Code by PAN token - Input parameters: %s", initSessionRequest.getPanToken());
					return pmWalletService.getTaxCode(initSessionRequest.getPanToken())
							.onFailure(NotFoundException.class::isInstance).recoverWithItem(new GetTaxCodeResponse())
							.onFailure().transform(t -> {
								Log.errorf(t, "[%s] Error while retrieving taxCode from PM wallet", ErrorCode.ERROR_CALLING_GET_TAX_CODE_SERVICE);
								return new InternalServerErrorException(Response
										.status(Status.INTERNAL_SERVER_ERROR)
										.entity(new Errors(List.of(ErrorCode.ERROR_CALLING_GET_TAX_CODE_SERVICE)))
										.build());
							})
							.map(GetTaxCodeResponse::getTaxCode);
				})
				.chain(taxCode -> {
					if (taxCode == null) {
						// if was not possible to retrieve the tax code, generates a pairing token
						// to try to identify the user with IO
						String pairingToken = String.format("%05d", new SecureRandom().nextInt(9999 + 1));

						InitSessionResponse initSessionResponse = new InitSessionResponse();
						initSessionResponse.setOutcome(Outcome.PAIR_WITH_IO.toString());
						initSessionResponse.setPairingToken(pairingToken);

						URI location = URI.create("/sessions?pairingToken=" + pairingToken);
						Log.debugf("initSession - Output parameters: %s, %s", location, initSessionResponse);
						return Uni.createFrom().item(
								Response
										.status(Status.ACCEPTED)
										.entity(initSessionResponse)
										.location(location)
										.header("Retry-After", ioPairingRetryAfter)
										.header("Max-Retries", ioPairingMaxRetry)
										.build());
					}
					else {
						// use the tax code to retrieve the tc acceptance and save new card flags
						return initSessionWithTaxCode(taxCode, commonHeader, initSessionRequest);
					}

				});

	}

	/**
	 * Branch of the initSession Uni that initialize a session using the tax code (that can be the one received in request or recovered from the PM Wallet
	 * @param taxCode the tax code for which to initialize the session
	 * @param commonHeader a set of mandatory headers
	 * @param initSessionRequest the original request containing the tax code or the tokenized pan of the card
	 * @return an {@link InitSessionResponse} instance containing the outcome of session initialization
	 */
	private Uni<Response> initSessionWithTaxCode(String taxCode, CommonHeader commonHeader, InitSessionRequest initSessionRequest) {

		return Uni.createFrom().item(taxCode)
			.attachContext()
				.map(cTaxCode -> {
					// create response and attach the context to it
					// the resulting object will be passed and returned from all the Uni in the chain
					// the context will contain the tax code and save new card flag that otherwise will be lost
					cTaxCode.context().put(CONTEXT_TAX_CODE, cTaxCode.get());
					return new ItemWithContext<>(cTaxCode.context(), new InitSessionResponse());
				})
				.chain(cResponse -> {
					// retrieve terms and condition acceptance status from termsandconds service
					Log.debugf("Calling MIL - retrieve terms and condition acceptance - Input parameters: %s", cResponse.context().get(CONTEXT_TAX_CODE).toString());
					return termsAndConsService.getTCByTaxCode(cResponse.context().get(CONTEXT_TAX_CODE), commonHeader)
							.onFailure(NotFoundException.class::isInstance).recoverWithItem(() -> {
								// if 404 recover with TERMS_AND_CONDITIONS_NOT_YET_ACCEPTED
								TermsAndConditionsResponse termsAndConditionsResponse = new TermsAndConditionsResponse();
								termsAndConditionsResponse.setOutcome(Outcome.TERMS_AND_CONDITIONS_NOT_YET_ACCEPTED.toString());
								return termsAndConditionsResponse;
							})
							.onFailure().transform(t -> {
								Log.errorf(t, "[%s] Error while retrieving terms and condition", ErrorCode.ERROR_CALLING_TERMS_AND_CONDITIONS_SERVICE);
								return new InternalServerErrorException(Response
										.status(Status.INTERNAL_SERVER_ERROR)
										.entity(new Errors(List.of(ErrorCode.ERROR_CALLING_TERMS_AND_CONDITIONS_SERVICE)))
										.build());
							})
							.map(tc -> {
								cResponse.get().setOutcome(tc.getOutcome());
								return cResponse;
							});
				})
				.chain(cResponse -> retrieveSettingAndStoreCard(initSessionRequest, cResponse))
				.chain(cResponse -> {
					// generate the sessionId and store it in the redis cache
					// then return the response to the caller
					String sessionId = UUID.randomUUID().toString();

					Session session = new Session();
					session.setTaxCode(cResponse.context().get(CONTEXT_TAX_CODE));
					boolean hasAcceptedTermsAndConditions = Outcome.OK.toString().equals(cResponse.get().getOutcome());
					session.setTermsAndConditionAccepted(hasAcceptedTermsAndConditions);
					if (hasAcceptedTermsAndConditions) {
						session.setSaveNewCards(cResponse.context().get(CONTEXT_SAVE_NEW_CARDS));
					}

					return sessionService.set(sessionId, session)
							.onFailure().transform(t -> {
								Log.errorf(t, "[%s] REDIS error saving session in cache", ErrorCode.REDIS_ERROR_WHILE_SAVING_SESSION);
								return new InternalServerErrorException(Response
										.status(Status.INTERNAL_SERVER_ERROR)
										.entity(new Errors(List.of(ErrorCode.REDIS_ERROR_WHILE_SAVING_SESSION)))
										.build());
							})
							.map(c -> {
								URI location = URI.create("/sessions/" + sessionId);
								Log.debugf("initSession - Output parameters: %s, %s", location, cResponse.get());
								return Response.status(Status.CREATED).entity(cResponse.get()).location(location).build();
							});
				});
	}

	/**
	 * Branch of the initSession Uni that, if the user already accepted the terms and condition, retrieves the saveNewCards settings from the PM wallet.
	 * If the tax code was passed in request returns the setting in the response, otherwise if the setting is true, try to save the card in the wallet
	 * @param initSessionRequest the original request containing the tax code or the tokenized pan of the card
	 * @param initSessionResponse the response to update and return to the upstream Uni
	 * @return a new {@link Uni} that emits the updated initSessionResponse
	 */
	private Uni<ItemWithContext<InitSessionResponse>> retrieveSettingAndStoreCard(InitSessionRequest initSessionRequest, ItemWithContext<InitSessionResponse> initSessionResponse) {

		if (Outcome.OK.toString().equals(initSessionResponse.get().getOutcome())) {
			return Uni.createFrom().item(initSessionResponse)
					.chain(cResponse -> {
						// retrieve saveNewCards flag from the PM wallet
						Log.debugf("retrieve saveNewCard flag - Input parameters: %s", cResponse.context().get(CONTEXT_TAX_CODE).toString());
						return pmWalletService
								.getSaveNewCards(cResponse.context().get(CONTEXT_TAX_CODE))
								.onFailure().recoverWithItem(t -> {
									Log.errorf(t, "[%s] Error while retrieving save new cards flag", ErrorCode.ERROR_CALLING_GET_SAVE_NEW_CARDS_SERVICE);
									// if there is an error in the integration with the service we don't block
									// and return the saveNewCard flag to false
									return new SaveNewCardsResponse();
								})
								.map(snc -> {
									// store the saveNewCard flag in the context
									// this is needed because if the pan was passed it is not returned in the response
									// so we need an object to store it
									cResponse.context().put(CONTEXT_SAVE_NEW_CARDS, snc.isSaveNewCards());
									if (initSessionRequest.getPanToken() == null) {
										cResponse.get().setSaveNewCards(snc.isSaveNewCards());
									}
									return cResponse;
								});
					})
					.call(cResponse -> {
						// asynch save of the card on the pm wallet
						// this operation is only done if the pan was passed in request and the saveNewCard flag is configured to true
						// we use call instead of chain because we don't
						if (initSessionRequest.getPanToken() != null &&
								(Boolean) cResponse.context().get(CONTEXT_SAVE_NEW_CARDS)) {
							Log.debugf("saving new card - Input parameters: %s", initSessionRequest.getPanToken());
							SaveCardRequest card = new SaveCardRequest();
							card.setPanToken(initSessionRequest.getPanToken());
							card.setTaxCode(cResponse.context().get(CONTEXT_TAX_CODE));

							return pmWalletService.saveCard(card)
									.onFailure().recoverWithItem(t -> {
										Log.errorf(t, "[%s] Error while saving card in wallet", ErrorCode.ERROR_CALLING_SAVE_CARD_SERVICE);
										return Response.ok().build();
									})
									.onItem().transform(r -> Boolean.TRUE);
						} else {
							return Uni.createFrom().item(() -> Boolean.FALSE);
						}
					});
		}
		else {
			return Uni.createFrom().item(initSessionResponse);
		}
	}

	/**
	 *
	 * @param commonHeader a set of mandatory headers
	 * @param sessionId the identifier of the session
	 * @return
	 */
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Path("/{id}")
	public Uni<Response> getSession(@Valid @BeanParam CommonHeader commonHeader,
									@Pattern(regexp = SESSION_ID_REGEX, message = "[" + ErrorCode.SESSION_ID_MUST_MATCH_REGEXP + "] session id must match \"{regexp}\"")
									@PathParam(value = "id") String sessionId) {
		Log.debugf("getSession - Input parameters: %s sessionId [%s]", commonHeader, sessionId);

		return retrieveSession(sessionId)
			.map(e -> {
				GetSessionResponse response = new GetSessionResponse();
				response.setOutcome(e.isTermsAndConditionAccepted() ? Outcome.OK.toString() :
						Outcome.TERMS_AND_CONDITIONS_NOT_YET_ACCEPTED.toString());
				response.setTaxCode(e.getTaxCode());
				response.setSaveNewCards(e.isSaveNewCards());
				Log.debugf("getSession - Output parameters: %s", response);
				return Response.ok(response).build();
			});
	}

	/**
	 *
	 * @param commonHeader a set of mandatory headers
	 * @param sessionId the identifier of the session
	 * @param sessionDataToUpdate
	 * @return
	 */
	@PATCH
	@Path("/{id}")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Uni<Response> patchSession(@Valid @BeanParam CommonHeader commonHeader,
									  @Pattern(regexp = SESSION_ID_REGEX, message = "[" + ErrorCode.SESSION_ID_MUST_MATCH_REGEXP + "] session id must match \"{regexp}\"")
									  @PathParam(value = "id") String sessionId,
									  Session sessionDataToUpdate) {
		Log.debugf("patchSession - Input parameters: %s sessionId [%s] %s", commonHeader, sessionId, sessionDataToUpdate);

		return retrieveSession(sessionId)
			.chain(currentSession -> {
				currentSession.setTermsAndConditionAccepted(sessionDataToUpdate.isTermsAndConditionAccepted());
				currentSession.setSaveNewCards(sessionDataToUpdate.isTermsAndConditionAccepted());
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
							PatchSessionResponse patchSessionResponse = new PatchSessionResponse();
							patchSessionResponse.setOutcome("ACCEPTED");
							Log.debugf("patchSession - Output parameters: %s, %s", location, patchSessionResponse);
							return Response.accepted(patchSessionResponse).build();
						});
			});
	}

	/**
	 *
	 * @param commonHeader a set of mandatory headers
	 * @param sessionId the identifier of the session
	 * @return
	 */
	@DELETE
	@Path("/{id}")
	@Produces(MediaType.APPLICATION_JSON)
	public Uni<Response> deleteSession(@Valid @BeanParam CommonHeader commonHeader,
									   @Pattern(regexp = SESSION_ID_REGEX, message = "[" + ErrorCode.SESSION_ID_MUST_MATCH_REGEXP + "] session id must match \"{regexp}\"")
									   @PathParam(value = "id") String sessionId) {
		Log.debugf("deleteSession - Input parameters: %s sessionId [%s]", commonHeader, sessionId);

		return sessionService.getdel(sessionId)
			.onFailure().transform(t -> {
				Log.errorf(t, "[%s] REDIS error while deleting session", ErrorCode.REDIS_ERROR_WHILE_DELETING_SESSION);
				return new InternalServerErrorException(Response
					.status(Status.INTERNAL_SERVER_ERROR)
					.entity(new Errors(List.of(ErrorCode.REDIS_ERROR_WHILE_DELETING_SESSION)))
					.build());
			})
			.onItem().ifNull().failWith(() ->{
					Log.errorf("[%s] REDIS session not found", ErrorCode.REDIS_ERROR_SESSION_NOT_FOUND);
					return new NotFoundException(Response
						.status(Status.NOT_FOUND)
						.entity(new Errors(List.of(ErrorCode.REDIS_ERROR_SESSION_NOT_FOUND)))
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
					Log.errorf("[%s] REDIS session not found", ErrorCode.REDIS_ERROR_SESSION_NOT_FOUND);
					return new NotFoundException(Response
						.status(Status.NOT_FOUND)
						.entity(new Errors(List.of(ErrorCode.REDIS_ERROR_SESSION_NOT_FOUND)))
						.build());
			});
	}

}
