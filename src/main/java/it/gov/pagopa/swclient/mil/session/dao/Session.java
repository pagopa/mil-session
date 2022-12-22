package it.gov.pagopa.swclient.mil.session.dao;

public class Session {

	private String taxCode;
	private boolean termsAndConditionAccepted;
	private Boolean saveNewCards;
	
	public String getTaxCode() {
		return taxCode;
	}
	public void setTaxCode(String taxCode) {
		this.taxCode = taxCode;
	}
	public boolean isTermsAndConditionAccepted() {
		return termsAndConditionAccepted;
	}
	public void setTermsAndConditionAccepted(boolean termsAndConditionAccepted) {
		this.termsAndConditionAccepted = termsAndConditionAccepted;
	}
	public Boolean isSaveNewCards() {
		return saveNewCards;
	}
	public void setSaveNewCards(Boolean saveNewCards) {
		this.saveNewCards = saveNewCards;
	}
	
	@Override
	public String toString() {
		return "Session [taxCode=" + taxCode + ", termsAndConditionAccepted=" + termsAndConditionAccepted
				+ ", saveNewCards=" + saveNewCards + "]";
	}
	
}
