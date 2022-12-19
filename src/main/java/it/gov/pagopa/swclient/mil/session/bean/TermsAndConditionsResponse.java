package it.gov.pagopa.swclient.mil.session.bean;

public class TermsAndConditionsResponse {

	private String outcome;
	
	public String getOutcome() {
		return outcome;
	}

	public void setOutcome(String outcome) {
		this.outcome = outcome;
	}

	@Override
	public String toString() {
		return "TermsAndCons [outcome=" + outcome + "]";
	}
	
}
