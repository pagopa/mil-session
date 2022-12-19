package it.gov.pagopa.swclient.mil.session.dao;

public class Session {

	private String taxCode;
	private boolean termsAndConditionAccepted;
	private boolean saveNewCards;
	
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
	public boolean isSaveNewCards() {
		return saveNewCards;
	}
	public void setSaveNewCards(boolean saveNewCards) {
		this.saveNewCards = saveNewCards;
	}
	
	@Override
	public String toString() {
		return "Session [taxCode=" + taxCode + ", termsAndConditionAccepted=" + termsAndConditionAccepted
				+ ", saveNewCards=" + saveNewCards + "]";
	}
	
}
