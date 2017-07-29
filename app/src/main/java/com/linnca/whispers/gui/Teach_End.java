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

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import com.linnca.whispers.data.ChainManager;
import com.linnca.whispers.data.FirebaseDBHeaders;
import com.linnca.whispers.R;
import com.linnca.whispers.gui.widgets.ActionAfterEndListener;

public class Teach_End extends Fragment implements ActionAfterEndListener,
        TimeExpired.TimeExpiredListener,
        YouAreOffline.YouAreOfflineListener{
    private final String TAG = "Teach_End";
    public static String BUNDLE_CHAIN_ID = "chainID";
    public static String BUNDLE_CHAIN_QUEUE_KEY = "chainQueueKey";
    public static String BUNDLE_LANGUAGE_CODE = "languageCode";
    //only if the user has to pronounce a phrase
    public static String BUNDLE_PHRASE = "phrase";

    private final String FRAGMENT_TIME_EXPIRED = "timeExpired";
    private final String FRAGMENT_FINAL_ANSWER = "finalAnswer";
    private final String FRAGMENT_RECORDER = "recorder";
    private final String FRAGMENT_OFFLINE = "offline";

    public static final String SAVED_STATE_LANGUAGE_CODE = "languageCode";
    public static final String SAVED_STATE_CHAIN_ID = "chainID";
    public static final String SAVED_STATE_CHAIN_QUEUE_KEY = "chainQueueKey";
    public static final String SAVED_STATE_PHRASE = "phrase";
    public static final String SAVED_STATE_STOPPED = "stopped";

    //whether the fragment stopped.
    //we release the chain queue on stop,
    //so we need to make the user go back and try again
    private boolean stopped = false;

    private String chainID;
    //if the user exists without linking hte chain,
    //put this back into the queue
    private String chainQueueKey = null;
    private String languageCode;
    private boolean cleanlyFinished = false;
    private String phrase = null;

    private DatabaseReference onlineRef;
    private ValueEventListener onlineListener;

    private FirebaseDatabase database;

    //for deciding the right layout to show
    private boolean firstLink;
    private boolean finalLink;

    private Teach_EndListener listener;

    public interface Teach_EndListener{
        void teachEndToTeachStart(boolean completed);
        void teachEndSaveData(String audioPath, String answer, String chainID, String chainQueueKey);
        void toOfflineMode();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //orientation change
        if (savedInstanceState != null){
            //this helps when the user exits (should show time expired),
            // changes orientation after app closes (should change orientation),
            // and then opens app (if we don't save the stopped state,
            // the screen will not go to the time expired)
            stopped = savedInstanceState.getBoolean(SAVED_STATE_STOPPED);
            languageCode = savedInstanceState.getString(SAVED_STATE_LANGUAGE_CODE);
            chainID = savedInstanceState.getString(SAVED_STATE_CHAIN_ID);
            chainQueueKey = savedInstanceState.getString(SAVED_STATE_CHAIN_QUEUE_KEY);
            phrase = savedInstanceState.getString(SAVED_STATE_PHRASE);
            if (stopped) {
                cleanlyFinished = true;
            }
        }

        if (!stopped) {
            Bundle bundle = getArguments();
            chainID = bundle.getString(BUNDLE_CHAIN_ID);

            //now this activity is responsible for putting the chain back into the queue
            // if the user never finishes.
            //this can be empty
            if (bundle.containsKey(BUNDLE_CHAIN_QUEUE_KEY)) {
                chainQueueKey = bundle.getString(BUNDLE_CHAIN_QUEUE_KEY);
            }

            if (bundle.containsKey(BUNDLE_LANGUAGE_CODE)) {
                languageCode = bundle.getString(BUNDLE_LANGUAGE_CODE);
            }

            if (bundle.containsKey(BUNDLE_PHRASE)) {
                phrase = bundle.getString(BUNDLE_PHRASE);
            }
        } else {
            cleanlyFinished = true;
        }

        database = FirebaseDatabase.getInstance();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState){
        getActivity().setTitle(R.string.toolbar_title_teach);
        return inflater.inflate(R.layout.fragment_wrapper, container, false);
    }

    @Override
    public void onStart(){
        super.onStart();
        FragmentManager fragmentManager = getChildFragmentManager();
        if (stopped && fragmentManager.findFragmentByTag(FRAGMENT_OFFLINE) == null && fragmentManager.findFragmentByTag(FRAGMENT_TIME_EXPIRED) == null){
            addTimeExpiredFragment();
        } else {
            onlineRef = database.getReference(".info/connected");
            onlineListener = new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    boolean connected = dataSnapshot.getValue(Boolean.class);
                    if (connected) {
                        FragmentManager fragmentManager = getChildFragmentManager();
                        if (fragmentManager.findFragmentByTag(FRAGMENT_RECORDER) == null &&
                                fragmentManager.findFragmentByTag(FRAGMENT_FINAL_ANSWER) == null &&
                                fragmentManager.findFragmentByTag(FRAGMENT_TIME_EXPIRED) == null &&
                                fragmentManager.findFragmentByTag(FRAGMENT_OFFLINE) == null
                                ) {
                            setLayoutBasedOnChain();
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

                ;

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }

            };
            onlineRef.addValueEventListener(onlineListener);
        }
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
            listener = (Teach_EndListener) context;
        } catch (Exception e){
            throw new ClassCastException(context.toString() + " must implement listener");
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState){
        super.onSaveInstanceState(outState);
        if (!getActivity().isChangingConfigurations() || stopped) {
            outState.putBoolean(SAVED_STATE_STOPPED, true);
        } else {
            if (languageCode != null)
                outState.putString(SAVED_STATE_LANGUAGE_CODE, languageCode);

            if (chainID != null)
                outState.putString(SAVED_STATE_CHAIN_ID, chainID);

            if (chainQueueKey != null)
                outState.putString(SAVED_STATE_CHAIN_QUEUE_KEY, chainQueueKey);

            if (phrase != null)
                outState.putString(SAVED_STATE_PHRASE, phrase);
        }


    }

    @Override
    public void onStop(){
        if (!getActivity().isChangingConfigurations() ){
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
        //the variable is persisted if the fragment never finishes
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
        if (stopped){
            return;
        }
        //this means the user created a chain but never
        //put anything into the chain queue
        if (chainQueueKey == null && !cleanlyFinished){
            ChainManager.removeChain(chainID);
            return;
        }

        if (chainQueueKey == null){ //&& cleanlyFinished)
            return;
        }
        //put the chain back into the queue if
        // the user exits the application without submitting
        //ChainManager.enqueue(chainQueue, languageCode,"",true);
        DatabaseReference chainQueueRef = database.getReference(
                FirebaseDBHeaders.TO_TEACH_CHAIN_QUEUE + "/" +
                        languageCode + "/" +
                        chainQueueKey
        );
        ChainManager.putChainBackIntoQueue(chainQueueRef);
        chainQueueKey = null;
    }


    //stores the chain info and rewards user
    @Override
    public void saveData(final String recordingPath, final String answer){
        if (chainQueueKey == null){
            //newly created chain
            database.getReference(
                    FirebaseDBHeaders.CHAINS + "/" +
                    chainID
            ).onDisconnect().cancel();
        } else {
            //chain already in queue
            database.getReference(
                    FirebaseDBHeaders.TO_TEACH_CHAIN_QUEUE + "/" +
                    languageCode + "/" +
                    chainQueueKey + "/" +
                    FirebaseDBHeaders.CHAIN_QUEUE_IN_QUEUE
            ).onDisconnect().cancel();
        }

        listener.teachEndSaveData(recordingPath, answer, chainID, chainQueueKey);
        chainQueueKey = null;
    }

    @Override
    public void redirectUser(){
        Log.d(TAG, "Redirecting user back to teach_start");
        chainQueueKey = null;
        cleanlyFinished = true;
        listener.teachEndToTeachStart(true);
    }

    private void setLayoutBasedOnChain(){
        DatabaseReference chainRef = database.getReference(
                FirebaseDBHeaders.CHAINS + "/" +
                chainID + "/" +
                FirebaseDBHeaders.CHAINS_ID_NEXT_LINK_NUMBER
        );
        chainRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                long nextLinkNumber = (long)dataSnapshot.getValue();
                if (nextLinkNumber == 0L) {
                    firstLink = true;
                    finalLink = false;
                }
                else if (nextLinkNumber == 2L){
                    firstLink = false;
                    finalLink = false;
                } else if (nextLinkNumber == 4L){
                    firstLink = false;
                    finalLink = true;
                }
                addFragmentUI();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    private void addFragmentUI(){
        //if the user stopped the activity in the middle of fetching the com.linnca.whispers.data
        if (stopped){
            return;
        }

        if (finalLink)
            addFinalAnswerUI();
        else
            addVoiceRecorderUI();
    }

    private void addVoiceRecorderUI(){
        FragmentManager fragmentManager = getChildFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        Fragment voiceRecorder = new VoiceRecorder();
        Bundle bundle = new Bundle();
        int showUserInput;
        String userInputPrompt;
        if (firstLink) {
            showUserInput = VoiceRecorder.USER_INPUT_GONE;
            userInputPrompt = phrase;
        }
        else {
            showUserInput = VoiceRecorder.USER_INPUT_REQUIRED;
            userInputPrompt = getResources().getString(R.string.voice_recorder_prompt_required);
        }
        bundle.putInt(VoiceRecorder.USER_INPUT_BUNDLE, showUserInput);
        bundle.putString(VoiceRecorder.USER_INPUT_PROMPT_BUNDLE, userInputPrompt);
        voiceRecorder.setArguments(bundle);
        fragmentTransaction.replace(R.id.fragment_layout, voiceRecorder, FRAGMENT_RECORDER);
        fragmentTransaction.commit();
    }

    private void addFinalAnswerUI(){
        FragmentManager fragmentManager = getChildFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        Fragment finalAnswer = new FinalAnswer();
        fragmentTransaction.replace(R.id.fragment_layout, finalAnswer, FRAGMENT_FINAL_ANSWER);
        fragmentTransaction.commit();
    }

    private void addTimeExpiredFragment(){
        FragmentManager fragmentManager = getChildFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        Fragment timeExpired = new TimeExpired();
        fragmentTransaction.replace(R.id.fragment_layout, timeExpired, FRAGMENT_TIME_EXPIRED);
        fragmentTransaction.commit();
    }

    private void addNoConnectionFragment(){
        FragmentManager fragmentManager = getChildFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        Fragment youAreOffline = new YouAreOffline();
        fragmentTransaction.replace(R.id.fragment_layout, youAreOffline, FRAGMENT_OFFLINE);
        fragmentTransaction.commit();
    }

    @Override
    public void backToStart(){
        listener.teachEndToTeachStart(false);
    }

    @Override
    public void toOfflineMode(){listener.toOfflineMode();}
}
