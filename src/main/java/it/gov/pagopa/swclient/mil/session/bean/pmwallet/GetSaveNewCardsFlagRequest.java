package it.gov.pagopa.swclient.mil.session.bean.pmwallet;

public class GetSaveNewCardsFlagRequest {

	private boolean saveNewCards;

	public boolean isSaveNewCards() {
		return saveNewCards;
	}

	public void setSaveNewCards(boolean saveNewCards) {
		this.saveNewCards = saveNewCards;
	}

	@Override
	public String toString() {
		return "GetSaveNewCardsFlagRequest [saveNewCards=" + saveNewCards + "]";
	}
	
}
