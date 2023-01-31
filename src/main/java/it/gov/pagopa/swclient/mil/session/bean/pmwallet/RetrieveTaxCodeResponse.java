package it.gov.pagopa.swclient.mil.session.bean.pmwallet;

public class RetrieveTaxCodeResponse {

	private String taxCode;

	public String getTaxCode() {
		return taxCode;
	}

	public void setTaxCode(String taxCode) {
		this.taxCode = taxCode;
	}

	@Override
	public String toString() {
		return "RetrieveTaxCodeResponse [taxCode=" + taxCode + "]";
	}
	
}
