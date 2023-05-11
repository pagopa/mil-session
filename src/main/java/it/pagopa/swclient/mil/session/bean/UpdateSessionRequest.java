package it.pagopa.swclient.mil.session.bean;

import it.pagopa.swclient.mil.session.ErrorCode;
import jakarta.validation.constraints.NotNull;

public class UpdateSessionRequest {

	@NotNull(message = "[" + ErrorCode.TERMS_AND_CONDS_ACCEPTED_NULL + "] termsAndCondsAccepted must not be null")
	private Boolean termsAndCondsAccepted;

	@NotNull(message = "[" + ErrorCode.SAVE_NEW_CARD_NULL + "] saveNewCards must not be null")
	private Boolean saveNewCards;

	public boolean isTermsAndCondsAccepted() {
		return termsAndCondsAccepted;
	}

	public void setTermsAndCondsAccepted(boolean termsAndCondsAccepted) {
		this.termsAndCondsAccepted = termsAndCondsAccepted;
	}

	public boolean isSaveNewCards() {
		return saveNewCards;
	}

	public void setSaveNewCards(boolean saveNewCards) {
		this.saveNewCards = saveNewCards;
	}

	@Override
	public String toString() {
		return "UpdateSessionRequest [termsAndCondsAccepted=" + termsAndCondsAccepted + ", " +
				"saveNewCards=" + saveNewCards + "]";
	}

}
