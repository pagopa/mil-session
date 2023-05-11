package it.pagopa.swclient.mil.session.bean.pmwallet;

public class PresaveRequest {
	
	private String panToken;
	private String taxCode;
	
	public String getPanToken() {
		return panToken;
	}
	public void setPanToken(String panToken) {
		this.panToken = panToken;
	}
	public String getTaxCode() {
		return taxCode;
	}
	public void setTaxCode(String taxCode) {
		this.taxCode = taxCode;
	}
	
	@Override
	public String toString() {
		return "PresaveRequest [panToken=" + panToken + ", taxCode=" + taxCode + "]";
	}
	
}
