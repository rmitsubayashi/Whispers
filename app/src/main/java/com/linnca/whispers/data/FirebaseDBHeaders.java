package com.linnca.whispers.data;

public class FirebaseDBHeaders {
    public static final String STORAGE_RECORDINGS = "recordings";
    public static final String STORAGE_TUTORIAL = "tutorial";

    public static final String OFFLINE_RECORDINGS = "offlineRecordings";
    public static final String OFFLINE_RECORDINGS_LANGUAGE_RECORDING_FILE_NAME = "fileName";
    public static final String OFFLINE_RECORDINGS_LANGUAGE_RECORDING_ANSWER = "answer";
    public static final String OFFLINE_RECORDINGS_LANGUAGE_RECORDING_RANDOM_SEQUENCE = "randomSequence";

    public static final String TO_TEACH_CHAIN_QUEUE = "toTeachChainQueue";
    public static final String TO_LEARN_CHAIN_QUEUE = "toLearnChainQueue";
    //make sure the strings are compatible with the chain queue class variables
    public static final String CHAIN_QUEUE_IN_QUEUE = "inQueue";

    public static final String CHAINS = "chains";
    public static final String CHAINS_ID_SITUATION_ID = "situationID";
    public static final String CHAINS_ID_PHRASE_ID = "phraseID";
    public static final String CHAINS_ID_USERS = "userIDs";
    public static final String CHAINS_ID_USERS_USER_ID = "userID";
    public static final String CHAINS_ID_USERS_NOTIFICATION_TYPE = "notificationType";
    public static final String CHAINS_ID_USERS_CHAT_ID = "chatID";
    public static final String CHAINS_ID_USERS_VISIBILITY = "visibility";

    public static final String CHAINS_ID_CHAT_MESSAGES = "chatMessages";
    public static final String CHAINS_ID_LINKS = "links";
    public static final String CHAINS_ID_NEXT_LINK_NUMBER = "nextLinkNumber";
    public static final String CHAINS_ID_LANGUAGE_CODE = "languageCode";

    public static final String MINIMUM_CHAINS = "minimumChains";
    //make sure the strings are compatible with the minimal chain class variables
    public static final String MINIMUM_CHAINS_USER_ID_CHAIN_ID = "chainID";
    public static final String MINIMUM_CHAINS_USER_ID_NEXT_LINK_NUMBER = "nextLinkNumber";
    public static final String MINIMUM_CHAINS_USER_ID_NEW_NOTIFICATION = "newNotification";
    public static final String MINIMUM_CHAINS_USER_ID_VISIBILITY = "visibility";
    public static final String MINIMUM_CHAINS_USER_ID_NOTIFICATION_TYPE = "notificationType";
    public static final String MINIMUM_CHAINS_USER_ID_LINKED_LINK_NUMBERS = "linkedLinkNumbers";
    public static final String MINIMUM_CHAINS_USER_ID_DATETIME_LINKED = "dateTimeLinked";
    
    public static final String USER = "user";
    public static final String USER_ID_TO_LEARN_LANGUAGE_CODE = "toLearnLanguageCode";
    public static final String USER_ID_TO_TEACH_LANGUAGE_CODE = "toTeachLanguageCode";
    //chat id (one signal id) different from FireBase id
    public static final String USER_ID_CHAT_ID = "chatID";
    public static final String USER_ID_NOTIFICATION_TYPE = "notificationType";

    public static final String LINKS = "links";

    public static final String LAST_LOGIN = "lastLogin";

    public static final String NEW_NOTIFICATION = "newNotification";
    
    public static final String LINK_HISTORY = "linkHistory";

    public static final String SITUATIONS = "situation";
    public static final String SITUATIONS_ID_TITLE = "title";
    public static final String SITUATIONS_ID_PHRASES = "phrases";

    public static final String SITUATION_TO_CREATE = "situationToCreate";
    public static final String SITUATION_TO_CREATE_CHAIN_COUNT = "chainCount";
    public static final String SITUATION_TO_CREATE_PHRASE_COUNT = "phraseCount";

    public static final String PHRASE_TO_CREATE = "phraseToCreate";
    public static final String PHRASE_TO_CREATE_INDEX = "phraseIndex";
    public static final String PHRASE_TO_CREATE_ID = "phraseID";

}
