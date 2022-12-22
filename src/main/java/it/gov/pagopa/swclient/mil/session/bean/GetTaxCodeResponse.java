package it.gov.pagopa.swclient.mil.session.bean;

public class GetTaxCodeResponse {

	private String taxCode;

	public String getTaxCode() {
		return taxCode;
	}

	public void setTaxCode(String taxCode) {
		this.taxCode = taxCode;
	}

	@Override
	public String toString() {
		return "GetTaxCodeResponse [taxCode=" + taxCode + "]";
	}
	
}
