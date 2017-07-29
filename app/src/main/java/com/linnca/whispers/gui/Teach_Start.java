package com.linnca.whispers.gui;

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
import android.widget.ProgressBar;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Query;
import com.google.firebase.database.Transaction;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import com.linnca.whispers.data.ChainManager;
import com.linnca.whispers.data.FirebaseDBHeaders;
import com.linnca.whispers.data.RecordingManager;
import com.linnca.whispers.data.datawrappers.ChainQueue;
import com.linnca.whispers.R;
import com.linnca.whispers.gui.widgets.ActionAfterStartListener;

public class Teach_Start extends Fragment implements ActionAfterStartListener,
        TimeExpired.TimeExpiredListener,
        YouAreOffline.YouAreOfflineListener{
    private final String TAG = "Teach_Start";
    public static final String BUNDLE_LANGUAGE_CODE = "languageCode";
    public static final String SAVED_STATE_LANGUAGE_CODE = "languageCode";
    public static final String SAVED_STATE_CHAIN_ID = "chainID";
    public static final String SAVED_STATE_CHAIN_QUEUE_KEY = "chainQueueKey";
    public static final String SAVED_STATE_PHRASE = "phrase";
    public static final String SAVED_STATE_FILE_NAME = "fileName";
    //we have a local variable for when the activity is stopped,
    //and we have the saved instance variable for when the user changes orientation
    //while on the time expired screen
    public static final String SAVED_STATE_STOPPED = "stopped";

    private final String fragment_layout = "teachStart";
    private final String FRAGMENT_OFFLINE = "offline";
    private final String FRAGMENT_PHRASE_DISPLAY = "phraseDisplay";
    private final String FRAGMENT_VOICE_PLAYER = "voicePlayer";
    private final String FRAGMENT_TIME_EXPIRED = "timeExpired";
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
    private String chainQueueKey;
    //only used if the user is teaching a new phrase
    String phrase = null;
    //only used if user is listening to a phrase
    String fileName = null;
    //cleanly finished activity
    private boolean cleanlyFinished = false;
    //whether the fragment has bee stopped (we should show the time expired fragment)
    private boolean stopped = false;
    private DatabaseReference onlineRef;
    private ValueEventListener onlineListener;


    private ProgressBar progressBar;

    private Teach_StartListener listener;

    public interface Teach_StartListener {
        void teachStartToTeachEnd(String chainQueueKey, String chainID, String phrase);
        void teachStartToTeachStart();
        void toOfflineMode();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null){
            stopped = savedInstanceState.getBoolean(SAVED_STATE_STOPPED);
            languageCode = savedInstanceState.getString(SAVED_STATE_LANGUAGE_CODE);
            chainID = savedInstanceState.getString(SAVED_STATE_CHAIN_ID);
            chainQueueKey = savedInstanceState.getString(SAVED_STATE_CHAIN_QUEUE_KEY);
            phrase = savedInstanceState.getString(SAVED_STATE_PHRASE);
            fileName = savedInstanceState.getString(SAVED_STATE_FILE_NAME);

            if (stopped){
                cleanlyFinished = true;
            }

        }

        database = FirebaseDatabase.getInstance();
        Bundle bundle = getArguments();
        languageCode = bundle.getString(BUNDLE_LANGUAGE_CODE);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState){
        View view = inflater.inflate(R.layout.fragment_wrapper, container, false);
        progressBar = (ProgressBar)view.findViewById(R.id.fragment_progress_bar);
        return view;
    }

    @Override
    public void onStart(){
        super.onStart();
        getActivity().setTitle(R.string.toolbar_title_teach);
        FragmentManager fragmentManager = getChildFragmentManager();
        if (stopped && fragmentManager.findFragmentByTag(FRAGMENT_OFFLINE) == null && fragmentManager.findFragmentByTag(FRAGMENT_TIME_EXPIRED) == null){
            addTimeExpiredFragment();
        }
        else {
            onlineRef = database.getReference(".info/connected");
            onlineListener = new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    boolean connected = dataSnapshot.getValue(Boolean.class);
                    if (connected) {
                        FragmentManager fragmentManager = getChildFragmentManager();
                        if (fragmentManager.findFragmentByTag(FRAGMENT_VOICE_PLAYER) == null &&
                                fragmentManager.findFragmentByTag(FRAGMENT_PHRASE_DISPLAY) == null &&
                                fragmentManager.findFragmentByTag(FRAGMENT_TIME_EXPIRED) == null &&
                                fragmentManager.findFragmentByTag(FRAGMENT_OFFLINE) == null) {
                            setLayoutBasedOnQueue();
                        }
                    } else {
                        //!stopped prevents attempting to add the fragment when we press the
                        //home button
                        if (!stopped) {
                            addNoConnectionFragment();
                        }
                        onlineRef.removeEventListener(onlineListener);
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }

            };
            onlineRef.addValueEventListener(onlineListener);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState){
        if (!getActivity().isChangingConfigurations() || stopped) {
            savedInstanceState.putBoolean(SAVED_STATE_STOPPED, true);
        } else {
            if (languageCode != null)
                savedInstanceState.putString(SAVED_STATE_LANGUAGE_CODE, languageCode);

            if (chainID != null)
                savedInstanceState.putString(SAVED_STATE_CHAIN_ID, chainID);

            if (chainQueueKey != null)
                savedInstanceState.putString(SAVED_STATE_CHAIN_QUEUE_KEY, chainQueueKey);

            if (phrase != null)
                savedInstanceState.putString(SAVED_STATE_PHRASE, phrase);
            if (fileName != null){
                savedInstanceState.putString(SAVED_STATE_FILE_NAME, fileName);
            }
        }




        super.onSaveInstanceState(savedInstanceState);
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

    private void addTimeExpiredFragment(){
        progressBar.setVisibility(View.GONE);
        FragmentManager fragmentManager = getChildFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        Fragment timeExpired = new TimeExpired();
        fragmentTransaction.replace(R.id.fragment_layout, timeExpired, fragment_layout);
        fragmentTransaction.commit();
    }

    private void addNoConnectionFragment(){
        FragmentManager fragmentManager = getChildFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        Fragment youAreOffline = new YouAreOffline();
        fragmentTransaction.replace(R.id.fragment_layout, youAreOffline, FRAGMENT_OFFLINE);
        fragmentTransaction.commit();
    }

    //if there is something in the queue,
    //dequeue and have the user listen to the audio recording.
    //if there isn't anything in the queue,
    // create a chain and have the user read and pronounce the phrase.
    //so, we need two separate UIs to handle these two scenarios
    private void setLayoutBasedOnQueue(){
        progressBar.setVisibility(View.VISIBLE);
        //start flow
        dequeue();
    }

    private void dequeue(){
        DatabaseReference queueReference = database.getReference(
                FirebaseDBHeaders.TO_TEACH_CHAIN_QUEUE + "/" +
                languageCode
        );

        Query queueQuery = queueReference.orderByChild(FirebaseDBHeaders.CHAIN_QUEUE_IN_QUEUE).limitToFirst(1);
        dequeueQuery(queueQuery, 0);
    }

    //this can loop over and over until there is a chain available that no one is using.
    //if it loops too many times, stop
    private void dequeueQuery(final Query query, final int loopCt){
        //max loop count
        if (loopCt == 10){
            startNewChain();
            return;
        }

        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                //there is nothing in the queue so make a new chain
                if (dataSnapshot.getChildrenCount() == 0){
                    startNewChain();
                    return;
                }

                //only one item looped since we are limiting it to 1
                for (DataSnapshot snapshot : dataSnapshot.getChildren()){
                    //get key
                    final String key = snapshot.getKey();
                    final DatabaseReference transactionRef =
                            database.getReference(
                                    FirebaseDBHeaders.TO_TEACH_CHAIN_QUEUE + "/" +
                                    languageCode + "/" +
                                    key + "/" +
                                    FirebaseDBHeaders.CHAIN_QUEUE_IN_QUEUE
                            );

                    transactionRef.runTransaction(new Transaction.Handler() {
                        @Override
                        public Transaction.Result doTransaction(MutableData mutableData) {
                            //check_me that this chain queue hasn't been remove already
                            long inQueue = mutableData.getValue(long.class);
                            if (inQueue == ChainQueue.WITH_USER){
                                return Transaction.abort();
                            } else if (inQueue == ChainQueue.IN_QUEUE){
                                //update values so we know that the user has this
                                mutableData.setValue(ChainQueue.WITH_USER);

                                chainQueueKey = key;
                                return Transaction.success(mutableData);
                            } else {
                                //should't reach this
                                return Transaction.abort();
                            }

                        }

                        @Override
                        public void onComplete(DatabaseError databaseError, boolean committed, DataSnapshot dataSnapshot) {
                            if (committed && chainQueueKey != null){
                                transactionRef.onDisconnect().setValue(ChainQueue.IN_QUEUE);
                                linkChain(key);
                            } else {
                                //try again if it fails
                                dequeueQuery(query, loopCt+1);
                            }
                        }
                    });
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    //grab the most sparse phrase and display on screen for the user to pronounce
    private void startNewChain(){

        //if the user exits before loading, stop the rest of the flow.
        //nothing needs to be updated in the database
        if (stopped)
            return;

        Query findMostSparseSituation = database.getReference(
                FirebaseDBHeaders.SITUATION_TO_CREATE + "/" +
                languageCode
        ).orderByChild(FirebaseDBHeaders.SITUATION_TO_CREATE_CHAIN_COUNT).limitToFirst(1);
        findMostSparseSituation.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                //still need to get the first child.
                //this will loop, but only one time since we are limiting to 1
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
        //if the user exits before loading, stop the rest of the flow.
        //nothing needs to be updated in the database
        if (stopped)
            return;
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
                    createNewChain(situationID, phraseID);
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

    private void createNewChain(String situationID, String phraseID){
        //still nothing needs to be updated on the database
        if (stopped)
            return;
        DatabaseReference chainReference = database.getReference(
                FirebaseDBHeaders.CHAINS
        );

        String key = chainReference.push().getKey();
        chainID = key;
        DatabaseReference newChainRef = chainReference.child(key);
        Map updateMap = new HashMap();
        //no need to check assignment?
        updateMap.put(FirebaseDBHeaders.CHAINS_ID_LANGUAGE_CODE, languageCode);
        updateMap.put(FirebaseDBHeaders.CHAINS_ID_PHRASE_ID, phraseID);
        updateMap.put(FirebaseDBHeaders.CHAINS_ID_SITUATION_ID, newChainSituationID);
        updateMap.put(FirebaseDBHeaders.CHAINS_ID_NEXT_LINK_NUMBER, 0);
        newChainRef.updateChildren(updateMap);
        //at this point there is no user or recording.
        //we add the first user (this user) on update,
        // which is logically consistent with updating existing chains
        //save so we can access the chain in the next activity
        newChainRef.onDisconnect().removeValue();

        getPhrase(situationID, phraseID);
    }

    private void getPhrase(String situationID, String phraseID){
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
                phrase = (String) dataSnapshot.getValue();
                //we have to check_me if the activity has been stopped here
                //or when we try to add the fragment, the activity doesn't exist and will
                // crash the app
                if (!stopped) {
                    addPronounceNewPhraseUI();
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    private void addPronounceNewPhraseUI(){
        progressBar.setVisibility(View.GONE);
        //shouldn't happen
        if (phrase == null)
            return;
        FragmentManager fragmentManager = getChildFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        Fragment phraseDisplay = new PhraseDisplay();
        Bundle bundle = new Bundle();
        bundle.putString(PhraseDisplay.BUNDLE_PHRASE, phrase);
        phraseDisplay.setArguments(bundle);
        fragmentTransaction.replace(R.id.fragment_layout, phraseDisplay, FRAGMENT_PHRASE_DISPLAY);
        fragmentTransaction.commit();
    }


    private void linkChain(String chainKey){
        if (stopped){
            return;
        }

        DatabaseReference chainRef = database.getReference(
                FirebaseDBHeaders.TO_TEACH_CHAIN_QUEUE + "/" +
                languageCode + "/" +
                chainKey
        );
        chainRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                ChainQueue queue = dataSnapshot.getValue(ChainQueue.class);
                chainID = queue.getChainID();
                if (stopped) {
                    return;
                }
                fileName = queue.getAudioPath();
                loadAudioFile();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.d(TAG, databaseError.getMessage());
            }
        });

    }

    private void loadAudioFile(){
        RecordingManager.OnSaveRecordingListener listener = new RecordingManager.OnSaveRecordingListener() {
            @Override
            public void onSaveRecording() {
                addVoicePlayerUI(RecordingManager.getInternalStorageFilePath(getContext(), fileName));
            }
        };
        RecordingManager.saveRecordingToInternalStorage(getContext(), fileName, fileName, listener);
    }

    private void addVoicePlayerUI(String audioPath){
        progressBar.setVisibility(View.GONE);
        FragmentManager fragmentManager = getChildFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        Fragment voicePlayer = new VoicePlayer();
        Bundle bundle = new Bundle();
        bundle.putString(VoicePlayer.BUNDLE_AUDIO_FILE_PATH_KEY, audioPath);
        voicePlayer.setArguments(bundle);
        fragmentTransaction.replace(R.id.fragment_layout, voicePlayer, FRAGMENT_VOICE_PLAYER);
        fragmentTransaction.commit();
    }

    @Override
    public void continueToEnd(){
        //we want to pass the chain id to the next page at least.
        //the chain queue can be null
        //clean up
        cleanlyFinished = true;
        listener.teachStartToTeachEnd(chainQueueKey, chainID, phrase);
        chainQueueKey = null;

    }

    @Override
    public void onStop(){
        //put the chain back into the queue
        // if the user exits without going to the next activity.
        //don't want to do this if the user is just changing orientation
        if (!getActivity().isChangingConfigurations()) {
            if (!cleanlyFinished) {
                if (chainQueueKey == null) {
                    database.getReference(
                            FirebaseDBHeaders.CHAINS + "/" +
                                    chainID
                    ).onDisconnect().cancel();
                } else {
                    database.getReference(
                            FirebaseDBHeaders.TO_TEACH_CHAIN_QUEUE + "/" +
                                    languageCode + "/" +
                                    chainQueueKey + "/" +
                                    FirebaseDBHeaders.CHAIN_QUEUE_IN_QUEUE
                    ).onDisconnect().cancel();
                }
                putChainBackToQueue();

            }
            if (fileName != null){
                RecordingManager.removeRecordingFromInternalStorage(getContext(), fileName);
            }
        }
        stopped = true;

        super.onStop();
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        if (onlineRef != null && onlineListener != null){
            onlineRef.removeEventListener(onlineListener);
        }
    }

    private void putChainBackToQueue(){
        if (chainQueueKey == null && cleanlyFinished) {
            return;
        }

        if (stopped){
            return;
        }

        //this means that this is a new chain (we didn't grab a chain queue)
        // and so we should remove the chain
        if (chainQueueKey == null){ //&& !cleanlyFinished
            ChainManager.removeChain(chainID);
            return;
        }

        //we put the chain back into the queue
        DatabaseReference chainQueueRef = database.getReference(
            FirebaseDBHeaders.TO_TEACH_CHAIN_QUEUE + "/" +
            languageCode + "/" +
            chainQueueKey
        );
        ChainManager.putChainBackIntoQueue(chainQueueRef);

        chainQueueKey = null;
    }

    @Override
    public void backToStart(){
        listener.teachStartToTeachStart();
    }

    @Override
    public void toOfflineMode(){listener.toOfflineMode();}

}
