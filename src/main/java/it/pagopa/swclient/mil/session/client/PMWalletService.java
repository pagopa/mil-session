package it.pagopa.swclient.mil.session.client;

import org.eclipse.microprofile.rest.client.annotation.ClientHeaderParam;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import io.smallrye.mutiny.Uni;
import it.pagopa.swclient.mil.session.bean.pmwallet.GetSaveNewCardsFlagRequest;
import it.pagopa.swclient.mil.session.bean.pmwallet.PresaveRequest;
import it.pagopa.swclient.mil.session.bean.pmwallet.RetrieveTaxCodeResponse;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.Response;

@RegisterRestClient(configKey = "pmwallet-api")
public interface PMWalletService {
	
	@GET
	@Path("/enabledServices/{taxCode}/saveNewCards")
	@ClientHeaderParam(name = "Version", value = "${pmwallet-api.get-savenewcards.version}")
	@ClientHeaderParam(name = "Ocp-Apim-Subscription-Key", value = "${pmwallet-api.apim-subscription-key}")
    Uni<GetSaveNewCardsFlagRequest> getSaveNewCardsFlag(@PathParam("taxCode") String taxCode);

	@GET
	@Path("/cards/{panToken}/taxCode")
	@ClientHeaderParam(name = "Version", value = "${pmwallet-api.get-taxcode.version}")
	@ClientHeaderParam(name = "Ocp-Apim-Subscription-Key", value = "${pmwallet-api.apim-subscription-key}")
	Uni<RetrieveTaxCodeResponse> retrieveTaxCode(@PathParam("panToken") String panToken);
	
	@POST
	@Path("/cards")
	@ClientHeaderParam(name = "Version", value = "${pmwallet-api.post-cards.version}")
	@ClientHeaderParam(name = "Ocp-Apim-Subscription-Key", value = "${pmwallet-api.apim-subscription-key}")
    Uni<Response> presave(PresaveRequest card);

}
