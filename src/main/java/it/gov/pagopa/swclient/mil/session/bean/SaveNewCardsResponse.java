package it.gov.pagopa.swclient.mil.session.bean;

public class SaveNewCardsResponse {

	private boolean saveNewCards;

	public boolean isSaveNewCards() {
		return saveNewCards;
	}

	public void setSaveNewCards(boolean saveNewCards) {
		this.saveNewCards = saveNewCards;
	}

	@Override
	public String toString() {
		return "SaveNewCardsResponse [saveNewCards=" + saveNewCards + "]";
	}
	
}
