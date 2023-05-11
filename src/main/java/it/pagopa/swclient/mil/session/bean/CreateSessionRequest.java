package it.pagopa.swclient.mil.session.bean;

import it.pagopa.swclient.mil.session.ErrorCode;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Pattern;

public class CreateSessionRequest {

	@Pattern(regexp = "^(?:([A-Z]{6}\\d{2}[A-Z]\\d{2}[A-Z]\\d{3}[A-Z])|(\\d{11}))$", message = "[" + ErrorCode.TAX_CODE_MUST_MATCH_REGEXP + "] taxCode must match \"{regexp}\"")
	private String taxCode;

	@Pattern(regexp = "^[a-zA-Z0-9]{1,32}$", message = "[" + ErrorCode.PAN_TOKEN_MUST_MATCH_REGEXP + "] panToken must match \"{regexp}\"")
	private String panToken;

	@AssertTrue(message = "[" + ErrorCode.PAN_TOKEN_TAX_CODE_BOTH_NULL + "] tax code or pan token must not be null")
    private boolean areBothNull() {
        return taxCode == null && panToken == null;
    }
	
	@AssertTrue(message = "[" + ErrorCode.PAN_TOKEN_TAX_CODE_BOTH_NOT_NULL + "] tax code and pan token must not be both passed in request")
    private boolean areBothValorized() {
        return taxCode != null && panToken != null;
    }
	
	public String getTaxCode() {
		return taxCode;
	}

	public void setTaxCode(String taxCode) {
		this.taxCode = taxCode;
	}

	public String getPanToken() {
		return panToken;
	}

	public void setPanToken(String panToken) {
		this.panToken = panToken;
	}

	@Override
	public String toString() {
		return "CreateSessionRequest [taxCode=" + taxCode + ", panToken=" + panToken + "]";
	}

}
