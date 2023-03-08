package it.gov.pagopa.swclient.mil.session.bean.termsandconds;

public class CheckResponse {

	private String outcome;
	
	public String getOutcome() {
		return outcome;
	}

	public void setOutcome(String outcome) {
		this.outcome = outcome;
	}

	@Override
	public String toString() {
		return "CheckResponse [outcome=" + outcome + "]";
	}
	
}
