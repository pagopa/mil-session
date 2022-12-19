package it.gov.pagopa.swclient.mil.session;


public final class ErrorCode {
	public static final String MODULE_ID = "002";

	public static final String PAN_TOKEN_MUST_MATCH_REGEXP = MODULE_ID + "000001";
	public static final String TAX_CODE_MUST_MATCH_REGEXP = MODULE_ID + "000002";

	public static final String PAN_TOKEN_TAX_CODE_BOTH_NULL = MODULE_ID + "000003";
	public static final String PAN_TOKEN_TAX_CODE_BOTH_NOT_NULL = MODULE_ID + "000004";
	
	public static final String ERROR_CALLING_TERMS_AND_CONDITIONS_SERVICE = MODULE_ID + "000005";
	
	public static final String ERROR_CALLING_GET_SAVE_NEW_CARDS_SERVICE = MODULE_ID + "000006";
	
	public static final String ERROR_CALLING_SAVE_CARD_SERVICE = MODULE_ID + "000007";
	
	public static final String REDIS_ERROR_WHILE_SAVING_SESSION = MODULE_ID + "000008";
	public static final String REDIS_ERROR_WHILE_RETRIEVING_SESSION = MODULE_ID + "000008";

	
	private ErrorCode() {
	}
}
