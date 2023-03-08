package it.gov.pagopa.swclient.mil.session.bean;

import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public class UpdateSessionResponse {
	
	private String outcome;

	public String getOutcome() {
		return outcome;
	}

	public void setOutcome(String outcome) {
		this.outcome = outcome;
	}

	@Override
	public String toString() {
		return "PatchSessionResponse [outcome=" + outcome + "]";
	}
	
}
