package mugenglish.whispers.gui;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Query;
import com.google.firebase.database.Transaction;
import com.google.firebase.database.ValueEventListener;

import org.joda.time.DateTime;

import java.util.Random;

import data.ChainManager;
import data.FirebaseDBHeaders;
import data.datawrappers.ChainQueue;
import mugenglish.whispers.R;
import mugenglish.whispers.gui.widgets.ActionAfterStartListener;

public class Teach_Start extends Fragment implements ActionAfterStartListener {
    public static String BUNDLE_LANGUAGE_CODE;
    private FirebaseDatabase database;
    private String languageCode;
    //saving the situation id when creating a new chain
    private String newChainSituationID;
    //saving the id for the chain we grabbed/created.
    //we need to save this for when the user updates the new chain
    //with a recording
    private String chainID;
    //if the user destroys the activity without successfully connecting the chain links,
    //we want to put the chain back into the queue
    private ChainQueue chainQueue = null;
    //cleanly finished activity
    private boolean cleanlyFinished = false;

    private Teach_StartListener listener;

    public interface Teach_StartListener {
        void teachStartToTeachEnd(ChainQueue chainQueue, String chainID);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        database = FirebaseDatabase.getInstance();
        Bundle bundle = getArguments();
        languageCode = bundle.getString(BUNDLE_LANGUAGE_CODE);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState){
        return inflater.inflate(R.layout.fragment_teach_start, container, false);
    }

    @Override
    public void onStart(){
        super.onStart();
        setLayoutBasedOnQueue();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        implementListeners(context);
    }

    //must implement to account for lower APIs
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        implementListeners(activity);
    }

    private void implementListeners(Context context){
        try {
            listener = (Teach_StartListener) context;
        } catch (Exception e){
            throw new ClassCastException(context.toString() + " must implement listener");
        }
    }

    //if there is something in the queue,
    //dequeue and have the user listen to the audio recording.
    //if there isn't anything in the queue,
    // create a chain and have the user read and pronounce the phrase.
    //so, we need two separate UIs to handle these two scenarios
    private void setLayoutBasedOnQueue(){
        //start flow
        dequeue();
    }

    private void dequeue(){
        DatabaseReference transactionReference = database.getReference(
                FirebaseDBHeaders.TO_TEACH_CHAIN_QUEUE + "/" +
                languageCode
        );
        //the way the data is organized now is inefficient.
        //if there are a lot of items in the queue, the user has to
        //download the whole queue
        transactionReference.runTransaction(new Transaction.Handler() {
            private ChainQueue chain = null;
            @Override
            public Transaction.Result doTransaction(MutableData mutableData) {
                MutableData mostRecentChainRef = null;
                ChainQueue mostRecentChain = null;
                DateTime mostRecentDateTime = null;
                for (MutableData childRef : mutableData.getChildren()){
                    ChainQueue childClass = childRef.getValue(ChainQueue.class);
                    DateTime childDateTime = DateTime.parse(childClass.getDateTimeAdded());
                    if (mostRecentChainRef == null){
                        mostRecentChainRef = childRef;
                        mostRecentChain = childClass;
                        mostRecentDateTime = childDateTime;
                    }

                    if (childDateTime.isAfter(mostRecentDateTime)){
                        mostRecentChain = childClass;
                        mostRecentDateTime = childDateTime;
                    }
                }

                if (mostRecentChainRef != null) {
                    //remove
                    mostRecentChainRef.setValue(null);
                    chain = mostRecentChain;
                }

                return Transaction.success(mutableData);
            }

            @Override
            public void onComplete(DatabaseError databaseError, boolean b, DataSnapshot dataSnapshot) {
                //we start a new chain
                if (chain == null){
                    startNewChain();
                } else {
                    linkChain(chain);
                }
            }
        });
    }

    //grab the most sparse phrase and display on screen for the user to pronounce
    private void startNewChain(){
        Log.d(getClass().getSimpleName(), "startNewChain()");
        Query findMostSparseSituation = database.getReference(
                FirebaseDBHeaders.SITUATION_TO_CREATE + "/" +
                languageCode
        ).orderByChild(FirebaseDBHeaders.SITUATION_TO_CREATE_CHAIN_COUNT).limitToFirst(1);
        findMostSparseSituation.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                //still need to get the first child.
                //this will loop, but only one time since we are limiting to the first 1
                for (DataSnapshot first : dataSnapshot.getChildren()) {
                    long phraseCt = (long) first.child(
                            FirebaseDBHeaders.SITUATION_TO_CREATE_PHRASE_COUNT
                    ).getValue();
                    //saving this so when we actually create the new chain,
                    //we don't have to fetch for it again.
                    //it makes more logical sense to store it here than in the phrases
                    newChainSituationID = first.getKey();
                    getRandomPhrase(newChainSituationID, phraseCt);

                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    private void getRandomPhrase(final String situationID, long phraseCt){
        Log.d(getClass().getSimpleName(), "getRandomPhrase()");
        Random random = new Random();
        long randomPhraseIndex = nextLong(random, phraseCt);
        Query randomPhraseQuery = database.getReference(
                FirebaseDBHeaders.PHRASE_TO_CREATE + "/" +
                situationID + "/" +
                languageCode
        ).orderByChild(FirebaseDBHeaders.PHRASE_TO_CREATE_INDEX).
                equalTo(randomPhraseIndex).limitToFirst(1);
        randomPhraseQuery.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                //again, grab first child
                for (DataSnapshot first : dataSnapshot.getChildren()) {
                    String phraseID = (String) first.child(FirebaseDBHeaders.PHRASE_TO_CREATE_ID).getValue();
                    createNewChain(phraseID);
                    getPhrase(situationID, phraseID);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    //long implementation of Random.nextInt(int upperBound)
    private long nextLong(Random rng, long n) {
        // error checking and 2^x checking removed for simplicity.
        long bits, val;
        do {
            bits = (rng.nextLong() << 1) >>> 1;
            val = bits % n;
        } while (bits-val+(n-1) < 0L);
        return val;
    }

    private void createNewChain(String phraseID){
        Log.d(getClass().getSimpleName(), "createNewChain()");
        DatabaseReference chainReference = database.getReference(
                FirebaseDBHeaders.CHAINS
        );

        String key = chainReference.push().getKey();
        chainID = key;
        DatabaseReference newChainRef = chainReference.child(key);
        newChainRef.child(FirebaseDBHeaders.CHAINS_ID_LANGUAGE_CODE).setValue(languageCode);
        newChainRef.child(FirebaseDBHeaders.CHAINS_ID_PHRASE_ID).setValue(phraseID);
        newChainRef.child(FirebaseDBHeaders.CHAINS_ID_SITUATION_ID).setValue(newChainSituationID);
        newChainRef.child(FirebaseDBHeaders.CHAINS_ID_NEXT_LINK_NUMBER).setValue(0);
        //at this point there is no user or recording.
        //we add the first user (this user) on update,
        // which is logically consistent with updating existing chains
        //save so we can access the chain in the next activity


        ChainManager.incrementSituationToCreateChainCt(chainID);
    }

    private void getPhrase(String situationID, String phraseID){
        Log.d(getClass().getSimpleName(), "getPhrase()");
        DatabaseReference phraseRef = database.getReference(
                FirebaseDBHeaders.SITUATIONS + "/" +
                situationID + "/" +
                FirebaseDBHeaders.SITUATIONS_ID_PHRASES + "/" +
                phraseID + "/" +
                languageCode
        );

        phraseRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                String phrase = (String) dataSnapshot.getValue();
                addPronounceNewPhraseUI(phrase);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    private void addPronounceNewPhraseUI(String phrase){
        Log.d(getClass().getSimpleName(), "addPronunciationNewPhraseUI");
        FragmentManager fragmentManager = getChildFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        Fragment phraseDisplay = new PhraseDisplay();
        Bundle bundle = new Bundle();
        bundle.putString(PhraseDisplay.BUNDLE_PHRASE, phrase);
        phraseDisplay.setArguments(bundle);
        fragmentTransaction.add(R.id.fragment_teach_start, phraseDisplay);
        fragmentTransaction.commit();
    }


    private void linkChain(ChainQueue chain){
        chainID = chain.getChainID();
        chainQueue = chain;
        addVoicePlayerUI(chain.getAudioPath());
    }

    private void addVoicePlayerUI(String audioPath){
        FragmentManager fragmentManager = getChildFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        Fragment voicePlayer = new VoicePlayer();
        Bundle bundle = new Bundle();
        bundle.putString(VoicePlayer.BUNDLE_AUDIO_FILE_NAME_KEY, audioPath);
        voicePlayer.setArguments(bundle);
        fragmentTransaction.add(R.id.fragment_teach_start, voicePlayer);
        fragmentTransaction.commit();
    }

    @Override
    public void continueToEnd(){
        //we want to pass the chain id to the next page at least.
        //the chain queue can be null
        listener.teachStartToTeachEnd(chainQueue, chainID);
        //clean up
        cleanlyFinished = true;
        chainQueue = null;
    }

    @Override
    public void onDestroy(){
        //put the chain back into the queue
        // if the user exits without going to the next activity
        putChainBackToQueue();
        super.onDestroy();
    }

    private void putChainBackToQueue(){
        if (chainQueue == null && cleanlyFinished) {
            return;
        }

        //this means that thi is a new chain (we didn't grab a chain queue)
        // and so we should remove the chain
        if (chainQueue == null){ //&& !cleanlyFinished
            ChainManager.removeChain(chainID);
            return;
        }

        //we put the chain back into the queue
        ChainManager.enqueue(chainQueue, languageCode, "", true);

        chainQueue = null;
    }
}
