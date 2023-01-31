package it.gov.pagopa.swclient.mil.session.client;

import io.smallrye.mutiny.Uni;
import it.gov.pagopa.swclient.mil.bean.CommonHeader;
import it.gov.pagopa.swclient.mil.session.bean.termsandconds.CheckResponse;
import org.eclipse.microprofile.rest.client.annotation.ClientHeaderParam;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import javax.ws.rs.BeanParam;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

@Path("/acceptedTermsConds")
@RegisterRestClient(configKey = "termsandconditions-api")
public interface TermsAndConditionsService {
	
	@GET
	@Path("/{taxCode}")
	@ClientHeaderParam(name = "Ocp-Apim-Subscription-Key", value = "${mil.apim-subscription-key}")
    Uni<CheckResponse> check(@PathParam("taxCode") String taxCode, @BeanParam CommonHeader commonHeader);

}
