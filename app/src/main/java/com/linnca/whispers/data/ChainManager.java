package com.linnca.whispers.data;

import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.GenericTypeIndicator;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Query;
import com.google.firebase.database.Transaction;
import com.google.firebase.database.ValueEventListener;

import org.joda.time.DateTime;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.linnca.whispers.data.datawrappers.ChainLink;
import com.linnca.whispers.data.datawrappers.ChainQueue;
import com.linnca.whispers.data.datawrappers.LinkHistory;
import com.linnca.whispers.data.datawrappers.MinimalChain;
import com.linnca.whispers.data.datawrappers.UserAddedToChain;

public class ChainManager {
    private static String TAG = "ChainManager";
    //using longs so we can easily store in FireBase (FireBase stores longs)
    //none is if the user has removed the minimal chain
    public static final long NOTIFICATION_TYPE_NONE = 0;
    //no push still allows notification icons to appear
    public static final long NOTIFICATION_TYPE_CHAT_ONLY = 1;
    //both push and icon
    public static final long NOTIFICATION_TYPE_ALL = 2;

    public static final long MINIMUM_CHAIN_VISIBILITY_VISIBLE = 0;
    public static final long MINIMUM_CHAIN_VISIBILITY_HIDDEN = 1;
    public static final long MINIMUM_CHAIN_VISIBILITY_REMOVED = 2;

    public static final long MINIMUM_CHAIN_UPDATE_NO_NEW_LINK = -1;


    public static void updateChain(final String chainID, final DatabaseReference previousQueueReference, final String fileName, final String answer){
        //two scenarios
        // 1. put it into the queue to continue the chain
        // 2. finish the chain.

        //either way, we have to remove the queue if it came from a queue
        if (previousQueueReference != null){
            previousQueueReference.removeValue();
        }

        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference chainReference = database.getReference(
                FirebaseDBHeaders.CHAINS + "/" + chainID
        );

        final String userID = FirebaseAuth.getInstance().getCurrentUser().getUid();

        //all values that need previous values to update
        chainReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                long currentLinkNumber = dataSnapshot.child(
                        FirebaseDBHeaders.CHAINS_ID_NEXT_LINK_NUMBER
                ).getValue(long.class);
                long newLinkNumber = currentLinkNumber + 1;
                updateChainLink(chainID, new ChainLink(userID, currentLinkNumber, fileName, answer));
                updateChainLinkNumber(chainID, newLinkNumber);

                //if this is not the end of the chain
                // (which should mean the recording is not null),
                // we should put it back into the queue
                if (newLinkNumber != 5 && fileName != null) {
                    ChainQueue chainQueue = new ChainQueue(chainID, fileName,
                             ChainQueue.IN_QUEUE);
                    //note that enqueue() updates the time to the current time.
                    //no need to get the time here
                    enqueue(chainQueue);
                }
                //else, we should check to see whether the answer is right,
                //and if it is, add credits to all users.
                //if it's correct, add it to the list of available offline audio.
                //also decrement the situation to create count
                else {
                    String situationID = dataSnapshot.child(
                            FirebaseDBHeaders.CHAINS_ID_SITUATION_ID
                    ).getValue(String.class);
                    String phraseID = dataSnapshot.child(
                            FirebaseDBHeaders.CHAINS_ID_PHRASE_ID
                    ).getValue(String.class);
                    String languageCode = dataSnapshot.child(
                            FirebaseDBHeaders.CHAINS_ID_LANGUAGE_CODE
                    ).getValue(String.class);
                    checkAnswer(chainID, situationID, phraseID, languageCode, answer);

                    decrementSituationToCreateChainCt(chainID);
                }

                //update each previous user's minimal chain.
                //note that the first person is still not added here
                DataSnapshot users = dataSnapshot.child(
                        FirebaseDBHeaders.CHAINS_ID_USERS
                );

                //we can safely cast the long to an int because we know there will never
                //be a number bigger than the max int value (0~5).
                //this has all the chat ids so we can notify them
                List<String> usersToPushNotify = new ArrayList<>((int)users.getChildrenCount());
                //this has all the user ids so we can set the notification indicator for
                // each user's bottom navigation view
                List<String> usersToChangeIcon = new ArrayList<>((int)users.getChildrenCount());
                for (DataSnapshot userSnapshot : users.getChildren()){
                    UserAddedToChain userAddedToChain = userSnapshot.getValue(UserAddedToChain.class);
                    //this handles everything for the minimal chain,
                    //from new notification icon to showing hidden minimal chains
                    updateMinimalChain(chainID, userAddedToChain.getUserID(), newLinkNumber);
                    if (userAddedToChain.getVisibility() == MINIMUM_CHAIN_VISIBILITY_VISIBLE ||
                            userAddedToChain.getVisibility() == MINIMUM_CHAIN_VISIBILITY_HIDDEN){
                        usersToChangeIcon.add(userAddedToChain.getUserID());
                    }

                    //whether we should send a push notification
                    if (userAddedToChain.getNotificationType() == NOTIFICATION_TYPE_ALL &&
                            userAddedToChain.getVisibility() != MINIMUM_CHAIN_VISIBILITY_REMOVED) {
                        usersToPushNotify.add(userAddedToChain.getChatID());
                    }
                }

                //this might be the first chain link so no user to notify
                if (usersToPushNotify.size() > 0) {
                    String[] usersToPushNotifyArray = usersToPushNotify.toArray(new String[usersToPushNotify.size()]);
                    //notify users that the chain link has been updated
                    ChatManager.sendNotification(
                            ChatManager.NOTIFICATION_IDENTIFIER_CHAIN_UPDATE, null, usersToPushNotifyArray
                    );
                    String[] usersToChangeIconArray = usersToChangeIcon.toArray(new String[usersToChangeIcon.size()]);
                    //this tells the bottom navigation view to show a notification icon
                    UserManager.notifyUserBottomNavigation(usersToChangeIconArray);
                }
                //after we update all previous users' chains,
                //we add the new user to the chain.
                //if the user is a teacher,
                //we shouldn't send him notifications
                boolean teacher = false;
                if (currentLinkNumber % 2 == 0){
                    teacher = true;
                }
                addUserToChain(chainID, userID, teacher);
                //also set up a minimal chain for the new user
                String situationID = dataSnapshot.child(
                        FirebaseDBHeaders.CHAINS_ID_SITUATION_ID
                ).getValue(String.class);
                addMinimalChain(chainID, userID, currentLinkNumber, newLinkNumber, situationID);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });


    }

    private static void checkAnswer(final String chainID, String situationID, String phraseID, final String languageCode, final String answer){
        DatabaseReference phraseRef = FirebaseDatabase.getInstance().getReference(
                FirebaseDBHeaders.SITUATIONS + "/" +
                situationID + "/" +
                FirebaseDBHeaders.SITUATIONS_ID_PHRASES + "/" +
                phraseID + "/" +
                languageCode
        );

        phraseRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                String answerToCheck = dataSnapshot.getValue(String.class);
                if (checkAnswers(answerToCheck, answer, languageCode)){
                    rewardCredits(chainID);
                    OfflineModeManager.uploadOfflineRecording(chainID, answer, languageCode);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    public static boolean checkAnswers(String answer1, String answer2, String languageCode){
        answer1 = cleanAnswer(answer1, languageCode);
        answer2 = cleanAnswer(answer2, languageCode);

        return answer1.equals(answer2);
    }

    //remove last punctuation and upper case
    //this works for English. not sure about other languages
    private static String cleanAnswer(String answer, String languageCode){
        Pattern pattern;
        Matcher matcher;
        switch (languageCode){
            case LanguageIDs.ENGLISH :
                answer = answer.toLowerCase();
                pattern = Pattern.compile("[?.!,;]?$");
                matcher = pattern.matcher(answer);
                return matcher.replaceAll("");
            case LanguageIDs.JAPANESE :
                pattern = Pattern.compile("[。、?!？！]?$");
                matcher = pattern.matcher(answer);
                return matcher.replaceAll("");
            default :
                return answer;
        }
    }

    private static void rewardCredits(String chainID){
        DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference(
                FirebaseDBHeaders.CHAINS + "/" +
                chainID + "/" +
                FirebaseDBHeaders.CHAINS_ID_USERS
        );

        usersRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (DataSnapshot snapshot : dataSnapshot.getChildren()){
                    String userID = snapshot.child(FirebaseDBHeaders.CHAINS_ID_USERS_USER_ID).getValue(String.class);
                    UserManager.addCredit(
                            userID, UserManager.CREDIT_REWARD_FOR_CORRECT_ANSWER, LinkHistory.TRANSACTION_TYPE_SUCCESSFUL_CHAIN
                    );
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    private static void changeChainNextNotificationToNone(final String chainID, String userID){
        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference(
                FirebaseDBHeaders.CHAINS + "/" +
                chainID + "/" +
                FirebaseDBHeaders.CHAINS_ID_USERS
        );

        Query userQuery = userRef.orderByChild(FirebaseDBHeaders.CHAINS_ID_USERS_USER_ID)
                .equalTo(userID).limitToFirst(1);
        userQuery.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                //only one will be returned
                for (DataSnapshot snapshot : dataSnapshot.getChildren()){
                    String userAddedID = snapshot.getKey();
                    DatabaseReference userAddedRef = FirebaseDatabase.getInstance().getReference(
                            FirebaseDBHeaders.CHAINS + "/" +
                            chainID + "/" +
                            FirebaseDBHeaders.CHAINS_ID_USERS + "/" +
                            userAddedID
                    );

                    userAddedRef.child(FirebaseDBHeaders.CHAINS_ID_USERS_NOTIFICATION_TYPE)
                            .setValue(NOTIFICATION_TYPE_NONE);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    //we need the previous value to update this
    private static void updateChainLinkNumber(String chainID, long newLinkNumber){
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference ref = database.getReference(
                FirebaseDBHeaders.CHAINS + "/" +
                chainID + "/" +
                FirebaseDBHeaders.CHAINS_ID_NEXT_LINK_NUMBER
        );
        ref.setValue(newLinkNumber);
    }

    //if the user is a teacher, he should not be notified when a new chain has been updated?
    private static void addUserToChain(final String chainID, final String userID, final boolean teacher){
        //grab the chat id from OneSignal and update the user info on the chain
        // (user ID(FireBase DB), datetime added, and user ID(OneSignal)
        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference(
                FirebaseDBHeaders.USER + "/" +
                userID
        );

        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                String chatID = dataSnapshot.child(
                        FirebaseDBHeaders.USER_ID_CHAT_ID
                ).getValue(String.class);
                long notificationType;
                if (teacher){
                    //we still want the teacher to receive push notifications when there is a chat message
                    notificationType = NOTIFICATION_TYPE_CHAT_ONLY;
                } else {
                    //right now there isn't a user setting to change this
                    //notificationType = dataSnapshot.child(FirebaseDBHeaders.USER_ID_NOTIFICATION_TYPE).getValue(long.class);
                    notificationType = NOTIFICATION_TYPE_ALL;
                }
                String dateTime = DateTime.now().toString();
                UserAddedToChain newUserAddedToChain = new UserAddedToChain(userID, dateTime, chatID, notificationType, MINIMUM_CHAIN_VISIBILITY_VISIBLE);
                FirebaseDatabase database = FirebaseDatabase.getInstance();
                DatabaseReference ref = database.getReference(
                        FirebaseDBHeaders.CHAINS + "/" +
                                chainID + "/" +
                                FirebaseDBHeaders.CHAINS_ID_USERS
                );

                ref.push().setValue(newUserAddedToChain);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

    }

    private static void addMinimalChain(final String chainID, final String userID, final long currentLinkNumber, final long nextLinkNumber, final String situationID){
        //get the user's language code so we can set the title of the minimal chain
        //to the user's preferred language.
        //I think we can use the user's preferences to do this,
        // but there might be cases where we create another user's minimal chain?
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference userLanguageRef = database.getReference(
                FirebaseDBHeaders.USER + "/"  +
                userID + "/" +
                FirebaseDBHeaders.USER_ID_TO_TEACH_LANGUAGE_CODE
        );

        userLanguageRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                String languageCode = dataSnapshot.getValue(String.class);
                continueAddMinimalChain(chainID, userID, currentLinkNumber, nextLinkNumber, situationID, languageCode);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    //continue add minimal chain.
    //to many nests so separate for readability purposes
    private static void continueAddMinimalChain(final String chainID, final String userID, final long currentLinkNumber, final long nextLinkNumber, String situationID, String languageCode){
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference situationNameRef = database.getReference(
                FirebaseDBHeaders.SITUATIONS + "/" +
                situationID + "/" +
                FirebaseDBHeaders.SITUATIONS_ID_TITLE + "/" +
                languageCode
        );

        situationNameRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                String name = dataSnapshot.getValue(String.class);
                endAddMinimalChain(chainID, userID, currentLinkNumber, nextLinkNumber, name);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    //next nest is adding a minimal chain
    private static void endAddMinimalChain(final String chainID, String userID, final long currentLinkNumber, final long nextLinkNumber, final String name){
        //we can either create a new minimal chain or append to an existing one
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        final DatabaseReference minimalChainRef = database.getReference(
                FirebaseDBHeaders.MINIMUM_CHAINS + "/" +
                userID
        );

        //shouldn't have to limit to 1 since there will be only one minimum chain per chain,
        // but just in case
        Query sameMinimalChainQuery = minimalChainRef.orderByChild(
                FirebaseDBHeaders.MINIMUM_CHAINS_USER_ID_CHAIN_ID
        ).equalTo(chainID).limitToFirst(1);

        sameMinimalChainQuery.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                String dateTime = DateTime.now().toString();
                if (dataSnapshot.getChildrenCount() == 0L){
                    String key = minimalChainRef.push().getKey();
                    List<Long> linkedLinkNumbers = new ArrayList<>(1);
                    linkedLinkNumbers.add(currentLinkNumber);
                    MinimalChain minimalChain = new MinimalChain(key, chainID, linkedLinkNumbers, nextLinkNumber, name, dateTime);
                    minimalChainRef.child(key).setValue(minimalChain);
                } else {
                    for (DataSnapshot child : dataSnapshot.getChildren()) {
                        String key = child.getKey();
                        //update
                        GenericTypeIndicator<List<Long>> type = new GenericTypeIndicator<List<Long>>() {
                        };
                        List<Long> linkedLinkNumbers = child.child(
                                FirebaseDBHeaders.MINIMUM_CHAINS_USER_ID_LINKED_LINK_NUMBERS
                        ).getValue(type);
                        linkedLinkNumbers.add(currentLinkNumber);
                        minimalChainRef.child(key).child(
                                FirebaseDBHeaders.MINIMUM_CHAINS_USER_ID_LINKED_LINK_NUMBERS
                        ).setValue(linkedLinkNumbers);
                        minimalChainRef.child(key).child(
                                FirebaseDBHeaders.MINIMUM_CHAINS_USER_ID_DATETIME_LINKED
                        ).setValue(dateTime);
                        minimalChainRef.child(key).child(
                                FirebaseDBHeaders.MINIMUM_CHAINS_USER_ID_NEXT_LINK_NUMBER
                        ).setValue(nextLinkNumber);
                        minimalChainRef.child(key).child(
                                FirebaseDBHeaders.MINIMUM_CHAINS_USER_ID_VISIBILITY
                        ).setValue(MINIMUM_CHAIN_VISIBILITY_VISIBLE);
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });


    }

    private static void updateChainLink(String chainID, ChainLink link){
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference ref = database.getReference(
                FirebaseDBHeaders.CHAINS + "/" +
                        chainID + "/" +
                        FirebaseDBHeaders.CHAINS_ID_LINKS
        );

        ref.push().setValue(link);
    }

    //we can get the chain from the queue's chain id,
    //and get the appropriate com.linnca.whispers.data,
    //but starting another thread to fetch the com.linnca.whispers.data never gets called
    //before destroying the activity.
    //(when the user destroys an activity during learning/teaching)
    public static void enqueue(final ChainQueue chainQueue, String languageCode,
                               String situationID, boolean toTeachQueue){
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference queueReference;
        if (toTeachQueue){
            queueReference = database.getReference(
                    FirebaseDBHeaders.TO_TEACH_CHAIN_QUEUE + "/" +
                            languageCode
            );
        } else {
            queueReference = database.getReference(
                    FirebaseDBHeaders.TO_LEARN_CHAIN_QUEUE + "/" +
                            languageCode + "/" +
                            situationID
            );
        }

        queueReference.push().setValue(chainQueue);
    }

    //this is just enqueueing where we grab the info from the chain
    public static void enqueue(final ChainQueue chainQueue){
        final FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference chainRef = database.getReference(FirebaseDBHeaders.CHAINS + "/" +
        chainQueue.getChainID());
        chainRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                String languageCode = dataSnapshot.child(
                        FirebaseDBHeaders.CHAINS_ID_LANGUAGE_CODE
                ).getValue(String.class);
                //identify which queue the chain should go to
                long nextLinkNumber = dataSnapshot.child(
                        FirebaseDBHeaders.CHAINS_ID_NEXT_LINK_NUMBER
                ).getValue(long.class);
                DatabaseReference queueReference;
                if (nextLinkNumber % 2 == 0){
                    queueReference = database.getReference(
                            FirebaseDBHeaders.TO_TEACH_CHAIN_QUEUE + "/" +
                                    languageCode
                    );
                } else {
                    String situationID = (String)dataSnapshot.child(
                            FirebaseDBHeaders.CHAINS_ID_SITUATION_ID
                    ).getValue();
                    queueReference = database.getReference(
                            FirebaseDBHeaders.TO_LEARN_CHAIN_QUEUE + "/" +
                                    languageCode + "/" +
                                    situationID
                    );
                }

                queueReference.push().setValue(chainQueue);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    public static void putChainBackIntoQueue(DatabaseReference chainQueueRef){
        //update all values atomically
        Map<String, Object> updateChain = new HashMap<>();
        updateChain.put(FirebaseDBHeaders.CHAIN_QUEUE_IN_QUEUE,
                ChainQueue.IN_QUEUE
        );
        chainQueueRef.updateChildren(updateChain);
    }

    //mainly used to remove a newly made chain before the user has a chance
    // to insert into the queue
    public static void removeChain(String chainID){
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference chainRef = database.getReference(
                FirebaseDBHeaders.CHAINS + "/" +
                chainID
        );

        chainRef.removeValue();
    }

    private static void decrementSituationToCreateChainCt(String chainID){
        final FirebaseDatabase database = FirebaseDatabase.getInstance();

        DatabaseReference chainRef = database.getReference(
                FirebaseDBHeaders.CHAINS + "/" +
                        chainID
        );
        chainRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.getValue() == null){
                    return;
                }
                String languageCode = (String)dataSnapshot.child(
                        FirebaseDBHeaders.CHAINS_ID_LANGUAGE_CODE
                ).getValue();
                String situationID = (String)dataSnapshot.child(
                        FirebaseDBHeaders.CHAINS_ID_SITUATION_ID
                ).getValue();
                DatabaseReference situationToCreateRef = database.getReference(
                        FirebaseDBHeaders.SITUATION_TO_CREATE + "/" +
                                languageCode + "/" +
                                situationID + "/" +
                                FirebaseDBHeaders.SITUATION_TO_CREATE_CHAIN_COUNT
                );
                situationToCreateRef.runTransaction(new Transaction.Handler() {
                    @Override
                    public Transaction.Result doTransaction(MutableData mutableData) {
                        if (mutableData.getValue() == null)
                            return Transaction.success(mutableData);
                        int currentCt = (int)mutableData.getValue();
                        int newCt;
                        if (currentCt == 0) //shouldn't happen
                            newCt = 0;
                        else
                            newCt = currentCt - 1;
                        mutableData.setValue(newCt);
                        return Transaction.success(mutableData);
                    }

                    @Override
                    public void onComplete(DatabaseError databaseError, boolean b, DataSnapshot dataSnapshot) {

                    }
                });
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

    }

    public static void incrementSituationToCreateChainCt(String chainID){
        final FirebaseDatabase database = FirebaseDatabase.getInstance();

        DatabaseReference chainRef = database.getReference(
                FirebaseDBHeaders.CHAINS + "/" +
                chainID
        );
        chainRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.getValue() == null){
                    return;
                }
                String languageCode = (String)dataSnapshot.child(
                        FirebaseDBHeaders.CHAINS_ID_LANGUAGE_CODE
                ).getValue();
                String situationID = (String)dataSnapshot.child(
                        FirebaseDBHeaders.CHAINS_ID_SITUATION_ID
                ).getValue();
                DatabaseReference situationToCreateRef = database.getReference(
                        FirebaseDBHeaders.SITUATION_TO_CREATE + "/" +
                                languageCode + "/" +
                                situationID + "/" +
                                FirebaseDBHeaders.SITUATION_TO_CREATE_CHAIN_COUNT
                );
                situationToCreateRef.runTransaction(new Transaction.Handler() {
                    @Override
                    public Transaction.Result doTransaction(MutableData mutableData) {
                        if (mutableData.getValue() == null)
                            return Transaction.success(mutableData);

                        long currentCt = (long)mutableData.getValue();
                        long newCt = currentCt + 1;
                        mutableData.setValue(newCt);
                        return Transaction.success(mutableData);
                    }

                    @Override
                    public void onComplete(DatabaseError databaseError, boolean b, DataSnapshot dataSnapshot) {

                    }
                });
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });

    }

    //dequeue should be handled in the client UI code since FireBase does not allow
    // values to be returned..

    //updates the minimal chain (link number, new notification, show if hidden).
    //also handles cases when we want to send the minimal chain a notification but not update a link number
    // (when the user sends a chat message)
    public static void updateMinimalChain(String chainID, String userToUpdate, final long newLinkNumber){
        final FirebaseDatabase database = FirebaseDatabase.getInstance();
        final DatabaseReference chainReference = database.getReference(
                FirebaseDBHeaders.MINIMUM_CHAINS + "/" +
                userToUpdate
        );

        Query chainQuery = chainReference.orderByChild(FirebaseDBHeaders.MINIMUM_CHAINS_USER_ID_CHAIN_ID).equalTo(chainID).limitToFirst(1);
        //change certain fields of the minimalChain class
        chainQuery.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                //we can't directly change the values of a query result
                //so find the same com.linnca.whispers.data again and set it there.
                //Only one iteration since the query is limited to 1
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    String minimumChainID = snapshot.getKey();
                    if (newLinkNumber != MINIMUM_CHAIN_UPDATE_NO_NEW_LINK) {
                        DatabaseReference linkNumberRef =
                                chainReference.child(minimumChainID + "/" +
                                        FirebaseDBHeaders.MINIMUM_CHAINS_USER_ID_NEXT_LINK_NUMBER);
                        linkNumberRef.setValue(newLinkNumber);
                    }
                    DatabaseReference notificationRef =
                            chainReference.child(minimumChainID + "/" +
                                    FirebaseDBHeaders.MINIMUM_CHAINS_USER_ID_NEW_NOTIFICATION);
                    notificationRef.setValue(true);
                    long visibility = snapshot.child(FirebaseDBHeaders.MINIMUM_CHAINS_USER_ID_VISIBILITY).getValue(long.class);
                    DatabaseReference visibilityRef =
                            chainReference.child(minimumChainID + "/" +
                                    FirebaseDBHeaders.MINIMUM_CHAINS_USER_ID_VISIBILITY);
                    if (visibility == MINIMUM_CHAIN_VISIBILITY_HIDDEN)
                        visibilityRef.setValue(MINIMUM_CHAIN_VISIBILITY_VISIBLE);

                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    public static void removeMinimalChainNewNotification(String minimumChainID, String userID){
        DatabaseReference notificationRef = FirebaseDatabase.getInstance().getReference(
                FirebaseDBHeaders.MINIMUM_CHAINS + "/" +
                userID + "/" +
                minimumChainID + "/" +
                FirebaseDBHeaders.MINIMUM_CHAINS_USER_ID_NEW_NOTIFICATION
        );
        notificationRef.setValue(false);
    }
}
