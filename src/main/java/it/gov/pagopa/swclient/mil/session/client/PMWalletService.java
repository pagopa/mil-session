package it.gov.pagopa.swclient.mil.session.client;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;

import it.gov.pagopa.swclient.mil.session.bean.GetTaxCodeResponse;
import org.eclipse.microprofile.rest.client.annotation.ClientHeaderParam;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import io.smallrye.mutiny.Uni;
import it.gov.pagopa.swclient.mil.session.bean.SaveCardRequest;
import it.gov.pagopa.swclient.mil.session.bean.SaveNewCardsResponse;

@RegisterRestClient(configKey = "pmwallet-api")
public interface PMWalletService {
	
	@GET
	@Path("/enabledServices/{taxCode}/saveNewCards")
	@ClientHeaderParam(name = "Version", value = "${pmwallet-api.get-savenewcards.version}")
    Uni<SaveNewCardsResponse> getSaveNewCards(@PathParam("taxCode") String taxCode);

	@GET
	@Path("/cards/{panToken}/taxCode")
	@ClientHeaderParam(name = "Version", value = "${pmwallet-api.post-cards.version}")
	Uni<GetTaxCodeResponse> getTaxCode(@PathParam("panToken") String panToken);
	
	@POST
	@Path("/cards")
	@ClientHeaderParam(name = "Version", value = "${pmwallet-api.cards.version}") 
    Uni<Response> saveCard(SaveCardRequest card);

}
