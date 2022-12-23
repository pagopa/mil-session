package it.gov.pagopa.swclient.mil.session.bean;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

public class InitSessionResponse {
	
	private String outcome;
	
	@JsonInclude(Include.NON_NULL)
	private Boolean saveNewCards;
	
	@JsonInclude(Include.NON_NULL)
	private String pairingToken;
	
	public String getOutcome() {
		return outcome;
	}

	public void setOutcome(String outcome) {
		this.outcome = outcome;
	}

	public Boolean isSaveNewCards() {
		return saveNewCards;
	}

	public void setSaveNewCards(Boolean saveNewCards) {
		this.saveNewCards = saveNewCards;
	}

	public String getPairingToken() {
		return pairingToken;
	}

	public void setPairingToken(String pairingToken) {
		this.pairingToken = pairingToken;
	}

	@Override
	public String toString() {
		return "InitSessionResponse [outcome=" + outcome + ", saveNewCards=" + saveNewCards + ", pairingToken="
				+ pairingToken + "]";
	}
	
}
