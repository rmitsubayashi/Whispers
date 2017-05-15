package data;

import android.provider.ContactsContract;
import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Query;
import com.google.firebase.database.Transaction;
import com.google.firebase.database.ValueEventListener;

import org.joda.time.DateTime;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import data.datawrappers.ChainLink;
import data.datawrappers.ChainQueue;
import data.datawrappers.MinimalChain;
import data.datawrappers.UserAddedToChain;

public class ChainManager {
    private static String TAG = "ChainManager";
    //using longs so we can easily store in FireBase (FireBase stores longs)
    public static long NOTIFICATION_TYPE_NONE = 0;
    public static long NOTIFICATION_TYPE_ONLY_NEXT = 1;
    public static long NOTIFICATION_TYPE_ALL = 2;

    public static void updateChain(final String chainID, final ChainLink chainLink){
        //two scenarios
        // 1. put it into the queue to continue the chain
        // 2. finish the chain
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
                updateChainLinkNumber(chainID, newLinkNumber);

                //if this is not the end of the chain
                // (which should mean the recording is not null),
                // we should put it back into the queue
                if (newLinkNumber != 5 && chainLink.getAudioFileName() != null) {
                    ChainQueue chainQueue = new ChainQueue(chainID, chainLink.getAudioFileName(),
                            "");
                    //note that enqueue() updates the time to the current time.
                    //no need to get teh time here
                    enqueue(chainQueue);
                }
                //else, we should check to see whether the answer is right,
                //and if it is, add credits to all users
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
                    String answer = chainLink.getAnswer();
                    checkAnswer(chainID, situationID, phraseID, languageCode, answer);
                }

                //update each previous user's minimal chain
                DataSnapshot users = dataSnapshot.child(
                        FirebaseDBHeaders.CHAINS_ID_USERS
                );

                //we can safely cast the long to an int because we know there will never
                //be a number bigger than the max int value (0~5).
                //this has all the chat ids so we can notify them
                List<String> usersToNotify = new ArrayList<>((int)users.getChildrenCount());
                //this has all the user ids so we can set the notification indicator for
                // each user's bottom navigation view
                List<String> usersToChangeIcon = new ArrayList<>((int)users.getChildrenCount());
                for (DataSnapshot userSnapshot : users.getChildren()){
                    UserAddedToChain userAddedToChain = userSnapshot.getValue(UserAddedToChain.class);
                    updateMinimalChain(chainID, userAddedToChain.getUserID(), newLinkNumber);
                    if (userAddedToChain.getNotificationType() != NOTIFICATION_TYPE_NONE) {
                        usersToNotify.add(userAddedToChain.getChatID());
                        usersToChangeIcon.add(userAddedToChain.getUserID());
                    }
                    //we can set this to none now since we've alerted this user's next
                    if (userAddedToChain.getNotificationType() == NOTIFICATION_TYPE_ONLY_NEXT) {
                        changeChainNextNotificationToNone(chainID, userID);
                        usersToChangeIcon.add(userAddedToChain.getUserID());
                    }
                }

                //this might be the first chain link so no user to notify
                if (usersToNotify.size() > 0) {
                    String[] usersToNotifyArray = usersToNotify.toArray(new String[usersToNotify.size()]);
                    //notify users that the chain link has been updated
                    ChatManager.sendNotification(
                            "Your chain has been updated", usersToNotifyArray
                    );
                    Log.d("Chain Manager", "sending notification");
                    String[] usersToChangeIconArray = usersToChangeIcon.toArray(new String[usersToChangeIcon.size()]);
                    //this tells the bottom navigation view to show a notification icon
                    UserManager.notifyUser(usersToChangeIconArray);
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

        //we add this link.
        //this can be asynchronous
        updateChainLink(chainID, chainLink);

    }

    private static void checkAnswer(final String chainID, String situationID, String phraseID, String languageCode, final String answer){
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
                if (checkAnswers(answerToCheck, answer)){
                    rewardCredits(chainID);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    public static boolean checkAnswers(String answer1, String answer2){
        answer1 = cleanAnswer(answer1);
        answer2 = cleanAnswer(answer2);

        return answer1.equals(answer2);
    }

    //remove last punctuation and upper case
    //this works for English. not sure about other languages
    private static String cleanAnswer(String answer){
        answer = answer.toLowerCase();
        Pattern pattern = Pattern.compile("[?.!,;]?$");
        Matcher m = pattern.matcher(answer);
        return m.replaceAll("");
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
                    UserManager.addCredit(userID, UserManager.CREDIT_REWARD_FOR_CORRECT_ANSWER);
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

        Query userQuery = userRef.orderByChild(FirebaseDBHeaders.CHAINS_ID_USERS_USER_ID).equalTo(chainID).limitToFirst(1);
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
                String chatID = dataSnapshot.child(FirebaseDBHeaders.USER_ID_CHAT_ID).getValue(String.class);
                long notificationType;
                if (teacher){
                    notificationType = NOTIFICATION_TYPE_NONE;
                } else {
                    notificationType = dataSnapshot.child(FirebaseDBHeaders.USER_ID_NOTIFICATION_TYPE).getValue(long.class);
                }
                String dateTime = DateTime.now().toString();
                UserAddedToChain newUserAddedToChain = new UserAddedToChain(userID, dateTime, chatID, notificationType);
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
    private static void endAddMinimalChain(String chainID, String userID, long currentLinkNumber, long nextLinkNumber, String name){
        String dateTime = DateTime.now().toString();
        //no need to notify a new chain when creating the user's own minimal chain.
        //we will need to if it's not the user who's creating it.
        MinimalChain minimalChain = new MinimalChain(chainID, currentLinkNumber, nextLinkNumber, name, dateTime, false);
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference minimalChainRef = database.getReference(
                FirebaseDBHeaders.USER + "/" +
                userID + "/" +
                FirebaseDBHeaders.USER_ID_MINIMUM_CHAINS
        );
        String key = minimalChainRef.push().getKey();
        minimalChainRef.child(key).setValue(minimalChain);
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
    //but starting another thread to fetch the data never gets called
    //before destroying the activity.
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

        Log.d(TAG, queueReference.toString());

        queueReference.push().setValue(chainQueue);
    }
    public static void enqueue(final ChainQueue chainQueue){
        //update the time this queue will be added
        String currentDateTime = DateTime.now().toString();
        chainQueue.setDateTimeAdded(currentDateTime);

        final FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference chainRef = database.getReference(FirebaseDBHeaders.CHAINS + "/" +
        chainQueue.getChainID());
        Log.d(TAG,"chain queue ref is :" + chainRef.toString());
        chainRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Log.d(TAG,dataSnapshot.getValue().toString());
                String languageCode = dataSnapshot.child(
                        FirebaseDBHeaders.CHAINS_ID_LANGUAGE_CODE
                ).getValue(String.class);
                Log.d(TAG,languageCode);
                //identify which queue the chain should go to
                long nextLinkNumber = dataSnapshot.child(
                        FirebaseDBHeaders.CHAINS_ID_NEXT_LINK_NUMBER
                ).getValue(long.class);
                Log.d(TAG,"num:"+nextLinkNumber);
                DatabaseReference queueReference;
                Log.d(TAG, "is divisible by 2:" + (nextLinkNumber % 2));
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

                Log.d(TAG, queueReference.toString());

                queueReference.push().setValue(chainQueue);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
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
                    Log.d(TAG, "decrementSituationToCreateChainCt() returned null");
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
                    Log.d(TAG, "incrementSituationToCreateChainCt() returned null");
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

    private static void updateMinimalChain(String chainID, String userToUpdate, final long newLinkNumber){
        final FirebaseDatabase database = FirebaseDatabase.getInstance();
        final DatabaseReference chainReference = database.getReference(
                FirebaseDBHeaders.USER + "/" +
                userToUpdate + "/" +
                FirebaseDBHeaders.USER_ID_MINIMUM_CHAINS
        );

        Query chainQuery = chainReference.orderByChild(FirebaseDBHeaders.USER_ID_MINIMUM_CHAINS_CHAIN_ID).equalTo(chainID).limitToFirst(1);
        //change certain fields of the minimalChain class
        chainQuery.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                //we can't directly change the values of a query result
                //so find the same data again and set it there.
                //Only one iteration since the query is limited to 1
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    String minimumChainID = snapshot.getKey();
                    DatabaseReference linkNumberRef =
                            chainReference.child(minimumChainID + "/" +
                                    FirebaseDBHeaders.USER_ID_MINIMUM_CHAINS_NEXT_LINK_NUMBER);
                    linkNumberRef.setValue(newLinkNumber);
                    DatabaseReference notificationRef =
                            chainReference.child(minimumChainID + "/" +
                                    FirebaseDBHeaders.USER_ID_MINIMUM_CHAINS_NEW_NOTIFICATION);
                    notificationRef.setValue(true);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }
}
