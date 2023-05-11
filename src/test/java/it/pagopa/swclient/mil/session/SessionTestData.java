package it.pagopa.swclient.mil.session;

public final class SessionTestData {


    public final static String CF_MARIO_ROSSI = "RSSMRA80A01H501U"; // accepted, save cards
    public final static String CF_MARIA_ROSSI = "RSSMRA80A41H501Y"; // accepted, save cards
    public final static String CF_MARTA_ROSSI = "RSSMRT80A41H501R"; // accepted, save cards
    public final static String CF_LUIGI_ROSSI = "RSSLGU80A01H501U"; // accepted, not save cards
    public final static String CF_ALESSANDRO_ROSSI = "RSSLSN80A01H501K"; // accepted, getSaveNewCardsFlag:404
    public final static String CF_GIOVANNI_ROSSI = "RSSGNN80A01H501N"; // accepted, getSaveNewCardsFlag:500
    public final static String CF_GIOVANNA_ROSSI = "RSSGNN80A41H501R"; // accepted, getSaveNewCardsFlag:timeout
    public final static String CF_MARIO_VERDI = "VRDMRA80A01H501Q"; // not accepted
    public final static String CF_LUIGI_VERDI = "VRDLGU80A01H501Q"; // tcCheck:404 (accepted but expired)
    public final static String CF_MARIO_BIANCHI = "BNCMRA80A01H501A"; // tcCheck:500
    public final static String CF_MARIA_BIANCHI = "BNCMRA80A41H501E"; // tcCheck:timeout
    public final static String CF_FRANCO_ROSSI	= "RSSFNC80A01H501B";
    public final static String CF_FRANCO_VERDI = "VRDMNC80A01H501X";


    public final static String PAN_MARIO_ROSSI = "a5069caab6a149008426508e1a966eeb"; // accepted, save cards, presave:204
    public final static String PAN_MARIA_ROSSI = "a5430e624c4a46c2a0953c770019b97e"; // accepted, save cards, presave:500
    public final static String PAN_MARTA_ROSSI = "aa35a707e2684045bd439e7d11a8956d"; // accepted, save cards, presave:timeout
    public final static String PAN_LUIGI_ROSSI = "a45491567e11431daec441e4f4a3c157"; // accepted, not save cards
    public final static String PAN_ALESSANDRO_ROSSI = "a4db9382379b4a799e26c01916b90932"; // accepted, getSaveNewCardsFlag:404
    public final static String PAN_GIOVANNI_ROSSI = "a6ea9a71c04c459694a1b9d239fa59fc"; // accepted, getSaveNewCardsFlag:500
    public final static String PAN_GIOVANNA_ROSSI = "a2723e115cd6496e83ab2e053856b0de"; // accepted, getSaveNewCardsFlag:timeout
    public final static String PAN_MARIO_VERDI = "ab64da842a334d8e9e7c6c7d2fd706e3"; // not accepted
    public final static String PAN_LUIGI_VERDI = "a358c16d12114ae89f4c504818185c2a"; // tcCheck:404 (accepted but expired)
    public final static String PAN_MARIO_BIANCHI = "a2c52680f72745978ed2e991a86d86dd"; // tcCheck:500
    public final static String PAN_MARIA_BIANCHI = "a7e0eef15c3b445385ea531d66bde493"; // tcCheck:500
    public final static String PAN_FRANCO_ROSSI = "a72a0cf5f141481aa33112adef17aaf8";
    public final static String PAN_FRANCO_VERDI = "a10521013606448a8bc3fcc131726562";


    private SessionTestData() {
    }
}
