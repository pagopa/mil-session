package it.gov.pagopa.swclient.mil.session.bean;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

public class GetSessionResponse {
	
	private String outcome;

	private String taxCode;

	@JsonInclude(Include.NON_NULL)
	private Boolean saveNewCards;

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

	public String getTaxCode() {
		return taxCode;
	}

	public void setTaxCode(String taxCode) {
		this.taxCode = taxCode;
	}

	@Override
	public String toString() {
		return "GetSessionResponse [outcome=" + outcome + ", taxCode=" + taxCode + ", saveNewCards=" + saveNewCards
				+ "]";
	}

	
}
