package it.pagopa.swclient.mil.session;


public final class ErrorCode {
	public static final String MODULE_ID = "002";

	public static final String PAN_TOKEN_MUST_MATCH_REGEXP 					= MODULE_ID + "000001";
	public static final String TAX_CODE_MUST_MATCH_REGEXP 					= MODULE_ID + "000002";
	public static final String SESSION_ID_MUST_MATCH_REGEXP 				= MODULE_ID + "000003";

	public static final String PAN_TOKEN_TAX_CODE_BOTH_NULL 				= MODULE_ID + "000004";
	public static final String PAN_TOKEN_TAX_CODE_BOTH_NOT_NULL 			= MODULE_ID + "000005";

	public static final String ERROR_CALLING_GET_TAX_CODE_SERVICE 			= MODULE_ID + "000006";
	public static final String ERROR_CALLING_TERMS_AND_CONDITIONS_SERVICE	= MODULE_ID + "000007";
	
	public static final String ERROR_CALLING_GET_SAVE_NEW_CARDS_SERVICE 	= MODULE_ID + "000008";
	
	public static final String ERROR_CALLING_SAVE_CARD_SERVICE 				= MODULE_ID + "000009";
	
	public static final String REDIS_ERROR_WHILE_SAVING_SESSION 			= MODULE_ID + "00000A";
	public static final String REDIS_ERROR_WHILE_RETRIEVING_SESSION 		= MODULE_ID + "00000B";
	public static final String REDIS_ERROR_WHILE_DELETING_SESSION 			= MODULE_ID + "00000C";
	public static final String REDIS_ERROR_SESSION_NOT_FOUND 				= MODULE_ID + "00000D";

	public static final String TERMS_AND_CONDS_ACCEPTED_NULL 				= MODULE_ID + "00000E";
	public static final String SAVE_NEW_CARD_NULL 							= MODULE_ID + "00000F";
	
	public static final String PAIRING_TOKEN_MUST_NOT_BE_NULL				= MODULE_ID + "000010";
	public static final String PAIRING_TOKEN_MUST_MATCH_REGEXP				= MODULE_ID + "000011";

	public static final String TAX_CODE_MUST_NOT_BE_NULL					= MODULE_ID + "000012";
	
	public static final String REDIS_ERROR_WHILE_SAVING_PAIRING 			= MODULE_ID + "000013";
	
	
	private ErrorCode() {
	}
}