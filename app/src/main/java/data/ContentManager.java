package data;

import android.util.Log;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Transaction;

import java.util.ArrayList;
import java.util.List;

import data.datawrappers.LanguagePhrasePair;

/*
 * We will import this into a different project and have the FirebaseDBHeaders
 * as a jar file in both.
 * For now, let's use the same project
 *
 */

//handles all content
// (new situations, new phrases, editing phrases, etc.)
public class ContentManager {
    public static void run(){
        Log.d("ContentManager","Running...");
        //
        List<LanguagePhrasePair> situation = new ArrayList<>();
        situation.add(new LanguagePhrasePair(LanguageIDs.ENGLISH ,"A night at a hotel"));
        situation.add(new LanguagePhrasePair(LanguageIDs.JAPANESE,"ホテルで一泊"));
        String situationID = createSituation("image path",situation);

        List<LanguagePhrasePair> phrase1_1 = new ArrayList<>();
        phrase1_1.add(new LanguagePhrasePair(LanguageIDs.ENGLISH, "Is there a room available?"));
        phrase1_1.add(new LanguagePhrasePair(LanguageIDs.JAPANESE,"部屋は空いていますか。"));
        createPhrase(situationID, phrase1_1);

        List<LanguagePhrasePair> phrase1_2 = new ArrayList<>();
        phrase1_2.add(new LanguagePhrasePair(LanguageIDs.ENGLISH, "Is there a non-smoking room available?"));
        phrase1_2.add(new LanguagePhrasePair(LanguageIDs.JAPANESE,"禁煙部屋は空いていますか。"));
        createPhrase(situationID, phrase1_2);

        List<LanguagePhrasePair> phrase1_3 = new ArrayList<>();
        phrase1_3.add(new LanguagePhrasePair(LanguageIDs.ENGLISH, "Where is the nearest restaurant?"));
        phrase1_3.add(new LanguagePhrasePair(LanguageIDs.JAPANESE,"一番近いレストランはどこですか。"));
        createPhrase(situationID, phrase1_3);

        List<LanguagePhrasePair> phrase1_4 = new ArrayList<>();
        phrase1_4.add(new LanguagePhrasePair(LanguageIDs.ENGLISH, "Will there be internet in my room?"));
        phrase1_4.add(new LanguagePhrasePair(LanguageIDs.JAPANESE,"客室はインターネットが繋がっていますか。"));
        createPhrase(situationID, phrase1_4);

        List<LanguagePhrasePair> phrase1_5 = new ArrayList<>();
        phrase1_5.add(new LanguagePhrasePair(LanguageIDs.ENGLISH, "Your room will be on the third floor"));
        phrase1_5.add(new LanguagePhrasePair(LanguageIDs.JAPANESE,"お部屋は３階になります。"));
        createPhrase(situationID, phrase1_5);

        List<LanguagePhrasePair> phrase1_6 = new ArrayList<>();
        phrase1_6.add(new LanguagePhrasePair(LanguageIDs.ENGLISH, "I have a reservation."));
        phrase1_6.add(new LanguagePhrasePair(LanguageIDs.JAPANESE,"予約があります。"));
        createPhrase(situationID, phrase1_6);



        //
        List<LanguagePhrasePair> situation2 = new ArrayList<>();
        situation2.add(new LanguagePhrasePair(LanguageIDs.ENGLISH ,"Ordering at a restaurant"));
        situation2.add(new LanguagePhrasePair(LanguageIDs.JAPANESE,"レストランで注文"));
        String situation2ID = createSituation("image path",situation2);

        List<LanguagePhrasePair> phrase2_1 = new ArrayList<>();
        phrase2_1.add(new LanguagePhrasePair(LanguageIDs.ENGLISH, "What are the specials tonight?"));
        phrase2_1.add(new LanguagePhrasePair(LanguageIDs.JAPANESE,"今夜のおすすめは何ですか。"));
        createPhrase(situation2ID, phrase2_1);

        List<LanguagePhrasePair> phrase2_2 = new ArrayList<>();
        phrase2_2.add(new LanguagePhrasePair(LanguageIDs.ENGLISH, "Could I have my steak medium-rare?"));
        phrase2_2.add(new LanguagePhrasePair(LanguageIDs.JAPANESE,"私のステーキはミディアムレアでお願いできますか。"));
        createPhrase(situation2ID, phrase2_2);

        List<LanguagePhrasePair> phrase2_3 = new ArrayList<>();
        phrase2_3.add(new LanguagePhrasePair(LanguageIDs.ENGLISH, "What are you going to get?"));
        phrase2_3.add(new LanguagePhrasePair(LanguageIDs.JAPANESE,"何にする？"));
        createPhrase(situation2ID, phrase2_3);

        List<LanguagePhrasePair> phrase2_4 = new ArrayList<>();
        phrase2_4.add(new LanguagePhrasePair(LanguageIDs.ENGLISH, "I'll have the salmon."));
        phrase2_4.add(new LanguagePhrasePair(LanguageIDs.JAPANESE,"私はサーモンにします。"));
        createPhrase(situation2ID, phrase2_4);

        List<LanguagePhrasePair> phrase2_5 = new ArrayList<>();
        phrase2_5.add(new LanguagePhrasePair(LanguageIDs.ENGLISH, "Does this contain peanuts?"));
        phrase2_5.add(new LanguagePhrasePair(LanguageIDs.JAPANESE,"これはピーナッツが入っていますか。"));
        createPhrase(situation2ID, phrase2_5);

        List<LanguagePhrasePair> phrase2_6 = new ArrayList<>();
        phrase2_6.add(new LanguagePhrasePair(LanguageIDs.ENGLISH, "How would you like your eggs?"));
        phrase2_6.add(new LanguagePhrasePair(LanguageIDs.JAPANESE,"卵はどのように召し上がりますか。"));
        createPhrase(situation2ID, phrase2_6);

        List<LanguagePhrasePair> phrase2_7 = new ArrayList<>();
        phrase2_7.add(new LanguagePhrasePair(LanguageIDs.ENGLISH, "Would you like something to drink?"));
        phrase2_7.add(new LanguagePhrasePair(LanguageIDs.JAPANESE,"飲み物はいかがでしょうか。"));
        createPhrase(situation2ID, phrase2_7);

        List<LanguagePhrasePair> phrase2_8 = new ArrayList<>();
        phrase2_8.add(new LanguagePhrasePair(LanguageIDs.ENGLISH, "There's hair in my soup!"));
        phrase2_8.add(new LanguagePhrasePair(LanguageIDs.JAPANESE,"スープの中に髪の毛が入ってるんですけど！"));
        createPhrase(situation2ID, phrase2_8);

        List<LanguagePhrasePair> phrase2_9 = new ArrayList<>();
        phrase2_9.add(new LanguagePhrasePair(LanguageIDs.ENGLISH, "How is everything?"));
        phrase2_9.add(new LanguagePhrasePair(LanguageIDs.JAPANESE,"食事はいかがですか。"));
        createPhrase(situation2ID, phrase2_9);

        //
        List<LanguagePhrasePair> situation3 = new ArrayList<>();
        situation3.add(new LanguagePhrasePair(LanguageIDs.ENGLISH ,"Watching a movie"));
        situation3.add(new LanguagePhrasePair(LanguageIDs.JAPANESE,"映画を鑑賞"));
        String situation3ID = createSituation("image path",situation3);

        List<LanguagePhrasePair> phrase3_1 = new ArrayList<>();
        phrase3_1.add(new LanguagePhrasePair(LanguageIDs.ENGLISH, "Let's get some popcorn."));
        phrase3_1.add(new LanguagePhrasePair(LanguageIDs.JAPANESE,"ポップコーンを買おう。"));
        createPhrase(situation3ID, phrase3_1);

        List<LanguagePhrasePair> phrase3_2 = new ArrayList<>();
        phrase3_2.add(new LanguagePhrasePair(LanguageIDs.ENGLISH, "Which movie do you want to watch?"));
        phrase3_2.add(new LanguagePhrasePair(LanguageIDs.JAPANESE,"どの映画を観たい。"));
        createPhrase(situation3ID, phrase3_2);

        List<LanguagePhrasePair> phrase3_3 = new ArrayList<>();
        phrase3_3.add(new LanguagePhrasePair(LanguageIDs.ENGLISH, "I want to watch an action movie."));
        phrase3_3.add(new LanguagePhrasePair(LanguageIDs.JAPANESE,"アクション映画を観たい。"));
        createPhrase(situation3ID, phrase3_3);

        List<LanguagePhrasePair> phrase3_4 = new ArrayList<>();
        phrase3_4.add(new LanguagePhrasePair(LanguageIDs.ENGLISH, "Save a seat for me."));
        phrase3_4.add(new LanguagePhrasePair(LanguageIDs.JAPANESE,"席を取っておいて。"));
        createPhrase(situation3ID, phrase3_4);

        List<LanguagePhrasePair> phrase3_5 = new ArrayList<>();
        phrase3_5.add(new LanguagePhrasePair(LanguageIDs.ENGLISH, "Stop making out!"));
        phrase3_5.add(new LanguagePhrasePair(LanguageIDs.JAPANESE,"キスするのやめろよ!"));
        createPhrase(situation3ID, phrase3_5);

        Log.d("ContentManager","Finished running");




    }


    //return the new situation id so it's easier to immediately add phrases
    private static String createSituation(String imagePath, List<LanguagePhrasePair> titlesInLanguage){
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference situationsRef = database.getReference(FirebaseDBHeaders.SITUATIONS);
        String situationID = situationsRef.push().getKey();
        DatabaseReference situationRef = situationsRef.child(situationID);
        situationRef.child(FirebaseDBHeaders.SITUATIONS_ID_IMAGE).setValue(imagePath);
        DatabaseReference situationTitleRef = situationRef.child(FirebaseDBHeaders.SITUATIONS_ID_TITLE);
        for (LanguagePhrasePair pair : titlesInLanguage){
            String languageCode = pair.getLanguageCode();
            String title = pair.getPhrase();
            situationTitleRef.child(languageCode).setValue(title);
        }

        return situationID;

    }

    //we are adding a phrase
    //with all the translations in a list
    private static void createPhrase(String situationID, List<LanguagePhrasePair> phrases){
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        //first update the phrases we use to fetch phrase translations
        DatabaseReference phrasesRef = database.getReference(
                FirebaseDBHeaders.SITUATIONS + "/" +
                situationID + "/" +
                FirebaseDBHeaders.SITUATIONS_ID_PHRASES
        );
        final String phraseID = phrasesRef.push().getKey();
        DatabaseReference phraseRef = phrasesRef.child(phraseID);
        for (LanguagePhrasePair pair : phrases){
            String languageCode = pair.getLanguageCode();
            String phrase = pair.getPhrase();
            phraseRef.child(languageCode).setValue(phrase);
        }

        //next, increment situation to create phrase counter.
        //we use this number to pick a random phrase for a teacher to create
        DatabaseReference situationToCreateRef = database.getReference(
                FirebaseDBHeaders.SITUATION_TO_CREATE
        );

        for (LanguagePhrasePair pair : phrases){
            String languageCode = pair.getLanguageCode();
            final String phrase = pair.getPhrase();
            DatabaseReference phraseCtRef =
                    situationToCreateRef.child(languageCode)
                            .child(situationID)
                            .child(FirebaseDBHeaders.SITUATION_TO_CREATE_PHRASE_COUNT);
            phraseCtRef.runTransaction(new Transaction.Handler() {
                @Override
                public Transaction.Result doTransaction(MutableData mutableData) {
                    long currentValue;
                    if (mutableData.getValue() == null)
                        currentValue = 0;
                    else
                        currentValue = (long)mutableData.getValue();
                    mutableData.setValue(currentValue + 1);
                    return Transaction.success(mutableData);
                }

                @Override
                public void onComplete(DatabaseError databaseError, boolean b, DataSnapshot dataSnapshot) {
                    Log.d("ContentManager",phrase + ":" + dataSnapshot.getValue());
                }
            });
        }

        //add chain ct (which should be 0)
        for (LanguagePhrasePair pair : phrases) {
            String languageCode = pair.getLanguageCode();
            DatabaseReference phraseCtRef =
                    situationToCreateRef.child(languageCode)
                            .child(situationID)
                            .child(FirebaseDBHeaders.SITUATION_TO_CREATE_CHAIN_COUNT);
            phraseCtRef.runTransaction(new Transaction.Handler() {
                @Override
                public Transaction.Result doTransaction(MutableData mutableData) {
                    if (mutableData.getValue() == null){
                        mutableData.setValue(0L);
                    }
                    return Transaction.success(mutableData);
                }

                @Override
                public void onComplete(DatabaseError databaseError, boolean b, DataSnapshot dataSnapshot) {

                }
            });
        }

        //lastly update the phrases that are used for getting a random phrase.
        DatabaseReference phrasesToCreateRef = database.getReference(
                FirebaseDBHeaders.PHRASE_TO_CREATE + "/" +
                situationID
        );
        for (LanguagePhrasePair pair : phrases){
            String languageCode = pair.getLanguageCode();

            DatabaseReference phrasesToCreateLanguageRef = phrasesToCreateRef.child(languageCode);
            final String phraseToCreateKey = phrasesToCreateLanguageRef.push().getKey();
            phrasesToCreateLanguageRef.runTransaction(new Transaction.Handler() {
                @Override
                public Transaction.Result doTransaction(MutableData mutableData) {
                    long childCt = mutableData.getChildrenCount();
                    mutableData.child(phraseToCreateKey)
                            .child(FirebaseDBHeaders.PHRASE_TO_CREATE_INDEX)
                            .setValue(childCt);
                    mutableData.child(phraseToCreateKey)
                            .child(FirebaseDBHeaders.PHRASE_TO_CREATE_ID)
                            .setValue(phraseID);
                    return Transaction.success(mutableData);
                }

                @Override
                public void onComplete(DatabaseError databaseError, boolean b, DataSnapshot dataSnapshot) {

                }
            });
        }
    }
}
