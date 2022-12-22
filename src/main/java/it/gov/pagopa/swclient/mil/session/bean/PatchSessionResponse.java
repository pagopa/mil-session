package it.gov.pagopa.swclient.mil.session.bean;

public class PatchSessionResponse {
	
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
