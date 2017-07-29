package com.linnca.whispers.data;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.util.Log;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.onesignal.OneSignal;

import org.joda.time.DateTime;
import org.joda.time.Days;

import java.util.Locale;

import com.linnca.whispers.data.datawrappers.LinkHistory;
import com.linnca.whispers.R;

public class UserManager {
    public static long CREDIT_REWARD_FOR_CORRECT_ANSWER = 25;
    public static long CREDIT_REWARD_FOR_TEACHING = 75;
    public static long CREDIT_NEEDED_FOR_LEARNING = 100;
    public static long CREDIT_LOGIN_REWARD = 100;
    public static long STARTING_CREDITS = 500;

    public interface OnRegisterUserListener {
        void onRegisterUser();
    }

    public interface OnLoginRewardListener {
        void onLoginReward();
    }

    public static void addCredit(final String userID, final long value, final int transactionType){
        final DatabaseReference creditRef = FirebaseDatabase.getInstance().getReference(
                FirebaseDBHeaders.LINKS + "/" +
                userID
        );

        creditRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                long creditValue = dataSnapshot.getValue(long.class);
                long newCreditValue = value + creditValue;
                creditRef.setValue(newCreditValue);
                LinkHistory history = new LinkHistory(DateTime.now().toString(), value, transactionType);
                DatabaseReference historyRef = FirebaseDatabase.getInstance().getReference(
                        FirebaseDBHeaders.LINK_HISTORY + "/" +
                        userID
                );
                historyRef.push().setValue(history);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    public static void removeCredit(final String userID, long value, final int transactionType){
        //just in case we input a negative value (which makes kinda sense)
        value = Math.abs(value);
        final DatabaseReference creditRef = FirebaseDatabase.getInstance().getReference(
                FirebaseDBHeaders.LINKS + "/" +
                userID
        );

        final long finalValue = value;
        creditRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                long creditValue = dataSnapshot.getValue(long.class);
                //no negative values
                long newCreditValue = (creditValue - finalValue > 0) ? creditValue - finalValue : 0;
                creditRef.setValue(newCreditValue);

                LinkHistory history = new LinkHistory(DateTime.now().toString(), (finalValue * (-1)), transactionType);
                DatabaseReference historyRef = FirebaseDatabase.getInstance().getReference(
                        FirebaseDBHeaders.LINK_HISTORY + "/" +
                                userID
                );
                historyRef.push().setValue(history);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    //this can be called when there is a new chat
    //or a progression in the chain.
    //there is a different 'new notification' indicator
    //for each minimum chain of the user
    public static void notifyUserBottomNavigation(String... userIDs){
        if (userIDs.length == 0)
            return;

        for (String userID : userIDs) {
            DatabaseReference notificationRef = FirebaseDatabase.getInstance().getReference(
                    FirebaseDBHeaders.NEW_NOTIFICATION + "/" +
                    userID
            );
            //the value doesn't matter, as long as it's true
            notificationRef.setValue(true);

            showMinimalChain(userID);
        }
    }

    private static void showMinimalChain(String userID){

    }

    public static String getToTeachLanguage(){
        String language = Locale.getDefault().getLanguage();
        switch (language){
            case "en" :
                return LanguageIDs.ENGLISH;
            case "ja" :
                return LanguageIDs.JAPANESE;
            default:
                //language not supported
                return null;
        }
    }

    public static void registerUser(final OnRegisterUserListener listener, final String languageToLearn, final Context context){
        Log.d("tutorial","Registering user...");
        if (FirebaseAuth.getInstance().getCurrentUser() == null){
            FirebaseAuth.getInstance().signInAnonymously().addOnSuccessListener(
                    new OnSuccessListener<AuthResult>() {
                        @Override
                        public void onSuccess(AuthResult authResult) {
                            String newUserID = authResult.getUser().getUid();
                            final DatabaseReference chatIDRef = FirebaseDatabase.getInstance().getReference(
                                    FirebaseDBHeaders.USER + "/" +
                                            newUserID + "/" +
                                            FirebaseDBHeaders.USER_ID_CHAT_ID
                            );

                            OneSignal.idsAvailable(new OneSignal.IdsAvailableHandler() {
                                @Override
                                public void idsAvailable(String userId, String registrationId) {
                                    chatIDRef.setValue(userId);
                                }
                            });

                            DatabaseReference userRef = FirebaseDatabase.getInstance().getReference(
                                    FirebaseDBHeaders.USER + "/" +
                                            newUserID
                            );

                            DatabaseReference toLearnRef = userRef.child(
                                    FirebaseDBHeaders.USER_ID_TO_LEARN_LANGUAGE_CODE
                            );
                            toLearnRef.setValue(languageToLearn);

                            String languageToTeach = UserManager.getToTeachLanguage();
                            DatabaseReference toTeachRef = userRef.child(
                                    FirebaseDBHeaders.USER_ID_TO_TEACH_LANGUAGE_CODE
                            );
                            toTeachRef.setValue(languageToTeach);

                            DatabaseReference creditsRef = FirebaseDatabase.getInstance().getReference(
                                    FirebaseDBHeaders.LINKS + "/" +
                                    newUserID
                            );
                            creditsRef.setValue(UserManager.STARTING_CREDITS);

                            DatabaseReference notificationTypeRef = userRef.child(
                                    FirebaseDBHeaders.USER_ID_NOTIFICATION_TYPE
                            );
                            //default is all?
                            notificationTypeRef.setValue(ChainManager.NOTIFICATION_TYPE_ALL);

                            //also set the preferences
                            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
                            SharedPreferences.Editor editor = preferences.edit();
                            editor.putString(
                                    context.getString(R.string.preferences_to_learn_language_key),
                                    languageToLearn
                            );
                            editor.putString(
                                    context.getString(R.string.preferences_to_teach_language_key),
                                    languageToTeach
                            );
                            //make sure this won't get called again
                            editor.putBoolean(context.getString(R.string.preferences_first_time_key), false);
                            //we commit instead of applying so
                            //the next page will guarantee access to the language
                            boolean suppressWarning = editor.commit();
                            if (suppressWarning)
                                listener.onRegisterUser();
                            else
                                Log.d("user registration","Could not commit the language preferences");

                        }
                    }
            ).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    e.printStackTrace();
                }
            });
        } else {
            Log.d("user registration","Could not register user because the user was already registered");
        }

    }

    public static void checkLastLogin(final String userID, final OnLoginRewardListener listener){
        final DatabaseReference loginRef = FirebaseDatabase.getInstance().getReference(
                FirebaseDBHeaders.LAST_LOGIN + "/" +
                userID
        );

        loginRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                //this should be the user's first time
                if (dataSnapshot.getValue() == null){
                    loginReward(userID, listener);
                } else {
                    String lastLogin = dataSnapshot.getValue(String.class);
                    DateTime lastLoginDate = DateTime.parse(lastLogin);
                    Days daysPassed = Days.daysBetween(lastLoginDate.toLocalDate(), DateTime.now().toLocalDate());
                    if (daysPassed.getDays() > 0) {
                        loginReward(userID, listener);
                    }
                }
                //save the current time
                loginRef.setValue(DateTime.now().toString());
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    private static void loginReward(String userID, OnLoginRewardListener listener){
        UserManager.addCredit(userID, CREDIT_LOGIN_REWARD, LinkHistory.TRANSACTION_TYPE_LOGIN_REWARD);
        listener.onLoginReward();
    }


}
