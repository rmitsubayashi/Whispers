package data;

public class FirebaseDBHeaders {
    public static String STORAGE_RECORDINGS = "recordings";

    public static String TO_TEACH_CHAIN_QUEUE = "toTeachChainQueue";
    public static String TO_LEARN_CHAIN_QUEUE = "toLearnChainQueue";

    public static String CHAINS = "chains";
    public static String CHAINS_ID_SITUATION_ID = "situationID";
    public static String CHAINS_ID_PHRASE_ID = "phraseID";
    public static String CHAINS_ID_USERS = "userIDs";
    public static String CHAINS_ID_USERS_USER_ID = "userID";
    public static String CHAINS_ID_USERS_NOTIFICATION_TYPE = "notificationType";
    public static String CHAINS_ID_USERS_CHAT_ID = "chatID";

    public static String CHAINS_ID_CHAT_MESSAGES = "chatMessages";
    public static String CHAINS_ID_LINKS = "links";
    public static String CHAINS_ID_NEXT_LINK_NUMBER = "nextLinkNumber";
    public static String CHAINS_ID_LANGUAGE_CODE = "languageCode";

    public static String USER = "user";
    public static String USER_ID_MINIMUM_CHAINS = "minimumChains";
    //make sure the strings are compatible with the minimal chain class variables
    public static String USER_ID_MINIMUM_CHAINS_CHAIN_ID = "chainID";
    public static String USER_ID_MINIMUM_CHAINS_NEXT_LINK_NUMBER = "nextLinkNumber";
    public static String USER_ID_MINIMUM_CHAINS_NEW_NOTIFICATION = "newNotification";
    public static String USER_ID_TO_LEARN_LANGUAGE_CODE = "toLearnLanguageCode";
    public static String USER_ID_TO_TEACH_LANGUAGE_CODE = "toTeachLanguageCode";
    //chat id (one signal id) different from FireBase id
    public static String USER_ID_CHAT_ID = "chatID";
    public static String USER_ID_NOTIFICATION_TYPE = "notificationType";
    public static String USER_ID_CREDITS = "credits";
    public static String USER_ID_NEW_NOTIFICATION = "newNotification";

    public static String SITUATIONS = "situation";
    public static String SITUATIONS_ID_TITLE = "title";
    public static String SITUATIONS_ID_IMAGE = "image";
    public static String SITUATIONS_ID_PHRASES = "phrases";

    public static String SITUATION_TO_CREATE = "situationToCreate";
    public static String SITUATION_TO_CREATE_CHAIN_COUNT = "chainCount";
    public static String SITUATION_TO_CREATE_PHRASE_COUNT = "phraseCount";

    public static String PHRASE_TO_CREATE = "phraseToCreate";
    public static String PHRASE_TO_CREATE_INDEX = "phraseIndex";
    public static String PHRASE_TO_CREATE_ID = "phraseID";
}
