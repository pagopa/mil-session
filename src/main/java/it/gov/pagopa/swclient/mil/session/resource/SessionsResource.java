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

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.resteasy.reactive.ClientWebApplicationException;

import io.quarkus.logging.Log;
import io.smallrye.mutiny.ItemWithContext;
import io.smallrye.mutiny.Uni;
import it.gov.pagopa.swclient.mil.bean.CommonHeader;
import it.gov.pagopa.swclient.mil.bean.Errors;
import it.gov.pagopa.swclient.mil.session.ErrorCode;
import it.gov.pagopa.swclient.mil.session.bean.CreateSessionRequest;
import it.gov.pagopa.swclient.mil.session.bean.CreateSessionResponse;
import it.gov.pagopa.swclient.mil.session.bean.GetSessionResponse;
import it.gov.pagopa.swclient.mil.session.bean.Outcome;
import it.gov.pagopa.swclient.mil.session.bean.TaxCodeSource;
import it.gov.pagopa.swclient.mil.session.bean.UpdateSessionRequest;
import it.gov.pagopa.swclient.mil.session.bean.UpdateSessionResponse;
import it.gov.pagopa.swclient.mil.session.bean.pmwallet.GetSaveNewCardsFlagRequest;
import it.gov.pagopa.swclient.mil.session.bean.pmwallet.PresaveRequest;
import it.gov.pagopa.swclient.mil.session.bean.pmwallet.RetrieveTaxCodeResponse;
import it.gov.pagopa.swclient.mil.session.bean.termsandconds.CheckResponse;
import it.gov.pagopa.swclient.mil.session.client.PMWalletService;
import it.gov.pagopa.swclient.mil.session.client.TermsAndConditionsService;
import it.gov.pagopa.swclient.mil.session.dao.Session;
import it.gov.pagopa.swclient.mil.session.dao.SessionService;

@Path("/sessions")
public class SessionsResource {

	private static final String CONTEXT_TAX_CODE 		= "TAX_CODE";
	private static final String CONTEXT_SAVE_NEW_CARDS 	= "SAVE_NEW_CARDS";
	private static final String CONTEXT_SOURCE			= "SOURCE";

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
	 * @param createSessionRequest the request containing the tax code or the tokenized pan of the card
	 * @return an {@link CreateSessionResponse} instance containing the outcome of session initialization
	 */
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Uni<Response> createSession(@Valid @BeanParam CommonHeader commonHeader, @Valid CreateSessionRequest createSessionRequest) {
		Log.debugf("createSession - Input parameters: %s, %s", commonHeader, createSessionRequest);

		return Uni.createFrom()
		         .item(createSessionRequest.getTaxCode())
		         .attachContext()
		         .chain(ctxTaxCode -> {
		            if (ctxTaxCode.get() != null) {
		               return Uni.createFrom().item(ctxTaxCode);
		            }
		            else {
		               // if the tokenized pan is passed in request, retrieve tax code from PM wallet
		               Log.debugf("Calling PMWallet - retrieve Tax Code by PAN token - Input parameters: panToken=%s", createSessionRequest.getPanToken());
		               return pmWalletService.retrieveTaxCode(createSessionRequest.getPanToken())
		                     .onFailure(t -> (t instanceof ClientWebApplicationException exc) && exc.getResponse().getStatus() == 404).recoverWithItem(new RetrieveTaxCodeResponse())
		                     .onFailure().transform(t -> {
		                        Log.errorf(t, "[%s] Error while retrieving taxCode from PM wallet", ErrorCode.ERROR_CALLING_GET_TAX_CODE_SERVICE);
		                        return new InternalServerErrorException(Response
		                              .status(Status.INTERNAL_SERVER_ERROR)
		                              .entity(new Errors(List.of(ErrorCode.ERROR_CALLING_GET_TAX_CODE_SERVICE)))
		                              .build());
		                     })
		                     .map(res ->  {
		                    	 ItemWithContext<String> walletCtxTaxCode = new ItemWithContext<>(ctxTaxCode.context(), res.getTaxCode());
		                    	 if (res.getSource() != null) {
		                    		 walletCtxTaxCode.context().put(CONTEXT_SOURCE, res.getSource());
		                    	 }
		                    	 return walletCtxTaxCode;
		                     });
		            }
		         })
		         .chain(ctxTaxCode -> {
		            if (ctxTaxCode.get() == null) {
		               // if was not possible to retrieve the tax code, generates a pairing token
		               // to try to identify the user with IO
		               String pairingToken = String.format("%05d", new SecureRandom().nextInt(9999 + 1));

		               CreateSessionResponse initSessionResponse = new CreateSessionResponse();
		               initSessionResponse.setOutcome(Outcome.PAIR_WITH_IO.toString());
		               initSessionResponse.setPairingToken(pairingToken);

		               URI location = URI.create("/sessions?pairingToken=" + pairingToken);
		               Log.debugf("createSession - Output parameters: %s, %s", location, initSessionResponse);
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
		               return createSessionWithTaxCode(ctxTaxCode, commonHeader, createSessionRequest);
		            }

		         });

		}


	/**
	 * Branch of the createSession Uni that initialize a session using the tax code (that can be the one received in request or recovered from the PM Wallet)
	 * @param ctxTaxCode the tax code for which to initialize the session wrapped in a context
	 * @param commonHeader a set of mandatory headers
	 * @param createSessionRequest the original request containing the tax code or the tokenized pan of the card
	 * @return an {@link CreateSessionResponse} instance containing the outcome of session initialization
	 */
	private Uni<Response> createSessionWithTaxCode(ItemWithContext<String> ctxTaxCode, CommonHeader commonHeader, CreateSessionRequest createSessionRequest) {

		return Uni.createFrom().item(ctxTaxCode)
				.map(cTaxCode -> {
					// create response and attach the context to it
					// the resulting object will be passed and returned from all the Uni in the chain
					// the context will contain the tax code and save new card flag that otherwise will be lost
					cTaxCode.context().put(CONTEXT_TAX_CODE, cTaxCode.get());
					return new ItemWithContext<>(cTaxCode.context(), new CreateSessionResponse());
				})
				.chain(cResponse -> {
					// retrieve terms and condition acceptance status from termsandconds service
					Log.debugf("Calling MIL - retrieve terms and condition acceptance - Input parameters: taxCode=%s", cResponse.context().get(CONTEXT_TAX_CODE).toString());
					return termsAndConsService.check(cResponse.context().get(CONTEXT_TAX_CODE), commonHeader)
							.onFailure(t -> (t instanceof ClientWebApplicationException exc) && exc.getResponse().getStatus() == 404).recoverWithItem(() -> {
								// if 404 recover with TERMS_AND_CONDITIONS_NOT_YET_ACCEPTED
								Log.debugf("Calling MIL - retrieve terms and condition acceptance returned 404, recovering with %s", Outcome.TERMS_AND_CONDITIONS_NOT_YET_ACCEPTED.toString());
								CheckResponse tcCheckResponse = new CheckResponse();
								tcCheckResponse.setOutcome(Outcome.TERMS_AND_CONDITIONS_NOT_YET_ACCEPTED.toString());
								return tcCheckResponse;
							})
							.onFailure().transform(t -> {
								Log.errorf(t, "[%s] Error while retrieving terms and condition", ErrorCode.ERROR_CALLING_TERMS_AND_CONDITIONS_SERVICE);
								return new InternalServerErrorException(Response
										.status(Status.INTERNAL_SERVER_ERROR)
										.entity(new Errors(List.of(ErrorCode.ERROR_CALLING_TERMS_AND_CONDITIONS_SERVICE)))
										.build());
							})
							.map(tc -> {
								Log.debugf("Calling MIL - retrieve terms and condition acceptance - Output %s", tc);
								cResponse.get().setOutcome(tc.getOutcome());
								return cResponse;
							});
				})
				.chain(ctxResponse -> retrieveSettingAndStoreCard(createSessionRequest, ctxResponse))
				.chain(cResponse -> {
					// generate the sessionId and store it in the redis cache
					// then return the response to the caller
					String sessionId = UUID.randomUUID().toString();

					Session session = new Session();
					session.setTaxCode(cResponse.context().get(CONTEXT_TAX_CODE));
					boolean hasAcceptedTermsAndConditions = Outcome.OK.toString().equals(cResponse.get().getOutcome());
					session.setTermsAndConditionAccepted(hasAcceptedTermsAndConditions);
					if (hasAcceptedTermsAndConditions && cResponse.context().contains(CONTEXT_SAVE_NEW_CARDS)) {
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
								Log.debugf("createSession - Output parameters: %s, %s", location, cResponse.get());
								return Response.status(Status.CREATED).entity(cResponse.get()).location(location).build();
							});
				});
	}
	

	/**
	 * Branch of the createSession Uni that, if the user already accepted the terms and condition, retrieves the saveNewCards settings from the PM wallet.
	 * If the tax code was passed in request returns the setting in the response, otherwise if the setting is true, try to save the card in the wallet
	 * @param createSessionRequest the original request containing the tax code or the tokenized pan of the card
	 * @param createSessionResponse the response to update and return to the upstream Uni
	 * @return a new {@link Uni} that emits the updated createSessionResponse
	 */
	private Uni<ItemWithContext<CreateSessionResponse>>  retrieveSettingAndStoreCard(CreateSessionRequest createSessionRequest, ItemWithContext<CreateSessionResponse> createSessionResponse) {

		String source = null;
		if (createSessionResponse.context().contains(CONTEXT_SOURCE)) {
			source = createSessionResponse.context().get(CONTEXT_SOURCE);
		}
		
		if ((source == null ||
				 source.equals(TaxCodeSource.EXTERNAL.name()))
				&&
				Outcome.OK.toString().equals(createSessionResponse.get().getOutcome())) {
			return Uni.createFrom().item(createSessionResponse)
					.chain(cResponse -> {
						// retrieve saveNewCards flag from the PM wallet
						Log.debugf("Calling PMWallet - retrieve saveNewCard flag - Input parameters: taxCode=%s", cResponse.context().get(CONTEXT_TAX_CODE).toString());
						return pmWalletService
								.getSaveNewCardsFlag(cResponse.context().get(CONTEXT_TAX_CODE))
								.onFailure().recoverWithItem(t -> {
									Log.errorf(t, "[%s] Error while retrieving save new cards flag", ErrorCode.ERROR_CALLING_GET_SAVE_NEW_CARDS_SERVICE);
									// if there is an error in the integration with the service we don't block
									// and return the saveNewCard flag to false
									return new GetSaveNewCardsFlagRequest();
								})
								.map(snc -> {
									// store the saveNewCard flag in the context
									// this is needed because if the pan was passed it is not returned in the response
									// so we need an object to store it
									Log.debugf("Calling PMWallet - retrieve saveNewCard flag - Output %s", snc);
									cResponse.context().put(CONTEXT_SAVE_NEW_CARDS, snc.isSaveNewCards());
									if (createSessionRequest.getPanToken() == null) {
										cResponse.get().setSaveNewCards(snc.isSaveNewCards());
									}
									return cResponse;
								});
					})
					.call(cResponse -> {
						// asynch save of the card on the pm wallet
						// this operation is only done if the pan was passed in request and the saveNewCard flag is configured to true
						// we use call instead of chain because we don't
						
						if (createSessionRequest.getPanToken() != null && 
								cResponse.context().getOrElse(CONTEXT_SAVE_NEW_CARDS, () -> Boolean.FALSE)) {
							PresaveRequest card = new PresaveRequest();
							card.setPanToken(createSessionRequest.getPanToken());
							card.setTaxCode(createSessionResponse.context().get(CONTEXT_TAX_CODE));
							Log.debugf("Calling PMWallet - saving new card - Input parameters: %s", card);
							return pmWalletService.presave(card)
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
			return Uni.createFrom().item(createSessionResponse);
		}
	}

	/**
	 * Retrieve a session by its session id
	 * @param commonHeader a set of mandatory headers
	 * @param sessionId the identifier of the session
	 * @return a {@link Session} containing the session information or 404 if not found
	 */
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Path("/{id}")
	public Uni<Response> getSessionById(@Valid @BeanParam CommonHeader commonHeader,
									@Pattern(regexp = SESSION_ID_REGEX, message = "[" + ErrorCode.SESSION_ID_MUST_MATCH_REGEXP + "] session id must match \"{regexp}\"")
									@PathParam(value = "id") String sessionId) {
		Log.debugf("getSessionById - Input parameters: %s sessionId [%s]", commonHeader, sessionId);

		return retrieveSession(sessionId)
			.map(e -> {
				GetSessionResponse response = new GetSessionResponse();
				response.setOutcome(e.isTermsAndConditionAccepted() ? Outcome.OK.toString() :
						Outcome.TERMS_AND_CONDITIONS_NOT_YET_ACCEPTED.toString());
				response.setTaxCode(e.getTaxCode());
				response.setSaveNewCards(e.isSaveNewCards());
				Log.debugf("getSessionById - Output parameters: %s", response);
				return Response.ok(response).build();
			});
	}

	/**
	 * Updates the termsAndCondsAccepted and saveNewCards value of an existing session
	 * @param commonHeader a set of mandatory headers
	 * @param sessionId the identifier of the session
	 * @param updateSessionRequest
	 * @return a {@link UpdateSessionResponse} containing the outcome of the successful update or 404 if not found
	 */
	@PATCH
	@Path("/{id}")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Uni<Response> updateSessionById(@Valid @BeanParam CommonHeader commonHeader,
									  @Pattern(regexp = SESSION_ID_REGEX, message = "[" + ErrorCode.SESSION_ID_MUST_MATCH_REGEXP + "] session id must match \"{regexp}\"")
									  @PathParam(value = "id") String sessionId,
									  UpdateSessionRequest updateSessionRequest) {
		Log.debugf("updateSessionById - Input parameters: %s sessionId [%s] %s", commonHeader, sessionId, updateSessionRequest);

		return retrieveSession(sessionId)
			.chain(currentSession -> {
				currentSession.setTermsAndConditionAccepted(updateSessionRequest.isTermsAndCondsAccepted());
				currentSession.setSaveNewCards(updateSessionRequest.isSaveNewCards());
				return sessionService.set(sessionId, currentSession)
						.onFailure().transform(t -> {
							Log.errorf(t, "[%s] REDIS error saving session in cache", ErrorCode.REDIS_ERROR_WHILE_SAVING_SESSION);
							return new InternalServerErrorException(Response
									.status(Status.INTERNAL_SERVER_ERROR)
									.entity(new Errors(List.of(ErrorCode.REDIS_ERROR_WHILE_SAVING_SESSION)))
									.build());
						})
						.map(c -> {
							UpdateSessionResponse updateSessionResponse = new UpdateSessionResponse();
							updateSessionResponse.setOutcome("ACCEPTED");
							Log.debugf("updateSessionById - Output parameters: %s", updateSessionResponse);
							return Response.accepted(updateSessionResponse).build();
						});
			});
	}

	/**
	 * Delete an existing session by its session id
	 * @param commonHeader a set of mandatory headers
	 * @param sessionId the identifier of the session
	 * @return
	 */
	@DELETE
	@Path("/{id}")
	@Produces(MediaType.APPLICATION_JSON)
	public Uni<Response> deleteSessionById(@Valid @BeanParam CommonHeader commonHeader,
									   @Pattern(regexp = SESSION_ID_REGEX, message = "[" + ErrorCode.SESSION_ID_MUST_MATCH_REGEXP + "] session id must match \"{regexp}\"")
									   @PathParam(value = "id") String sessionId) {
		Log.debugf("deleteSessionById - Input parameters: %s sessionId [%s]", commonHeader, sessionId);

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
				Log.debugf("deleteSessionById - Output parameters: %s", e);
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
