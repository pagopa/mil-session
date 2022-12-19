package it.gov.pagopa.swclient.mil.session.client;

import javax.ws.rs.BeanParam;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import io.smallrye.mutiny.Uni;
import it.gov.pagopa.swclient.mil.bean.CommonHeader;
import it.gov.pagopa.swclient.mil.session.bean.TermsAndConditionsResponse;

@Path("/acceptedTermsConds")
@RegisterRestClient(configKey = "termsandconditions-api")
public interface TermsAndConditionsService {
	
	@GET
	@Path("/{taxCode}")
    Uni<TermsAndConditionsResponse> getTCByTaxCode(@PathParam("taxCode") String taxCode, @BeanParam CommonHeader commonHeader);

}
