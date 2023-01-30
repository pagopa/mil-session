package it.gov.pagopa.swclient.mil.session.client;

import io.smallrye.mutiny.Uni;
import it.gov.pagopa.swclient.mil.session.bean.pmwallet.RetrieveTaxCodeResponse;
import it.gov.pagopa.swclient.mil.session.bean.pmwallet.PresaveRequest;
import it.gov.pagopa.swclient.mil.session.bean.pmwallet.GetSaveNewCardsFlagRequest;
import org.eclipse.microprofile.rest.client.annotation.ClientHeaderParam;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;

@RegisterRestClient(configKey = "pmwallet-api")
public interface PMWalletService {
	
	@GET
	@Path("/enabledServices/{taxCode}/saveNewCards")
	@ClientHeaderParam(name = "Version", value = "${pmwallet-api.get-savenewcards.version}")
    Uni<GetSaveNewCardsFlagRequest> getSaveNewCardsFlag(@PathParam("taxCode") String taxCode);

	@GET
	@Path("/cards/{panToken}/taxCode")
	@ClientHeaderParam(name = "Version", value = "${pmwallet-api.get-taxcode.version}")
	Uni<RetrieveTaxCodeResponse> retrieveTaxCode(@PathParam("panToken") String panToken);
	
	@POST
	@Path("/cards")
	@ClientHeaderParam(name = "Version", value = "${pmwallet-api.post-cards.version}")
    Uni<Response> presave(PresaveRequest card);

}
