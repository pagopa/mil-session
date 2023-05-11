package it.pagopa.swclient.mil.session.client;

import org.eclipse.microprofile.rest.client.annotation.ClientHeaderParam;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import io.smallrye.mutiny.Uni;
import it.pagopa.swclient.mil.bean.CommonHeader;
import it.pagopa.swclient.mil.session.bean.termsandconds.CheckResponse;
import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;

@Path("/acceptedTermsConds")
@RegisterRestClient(configKey = "termsandconditions-api")
public interface TermsAndConditionsService {
	
	@GET
	@Path("/{taxCode}")
	@ClientHeaderParam(name = "Ocp-Apim-Subscription-Key", value = "${mil.apim-subscription-key}")
    Uni<CheckResponse> check(@PathParam("taxCode") String taxCode, @BeanParam CommonHeader commonHeader);

}
