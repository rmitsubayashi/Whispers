package com.linnca.whispers.gui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import com.linnca.whispers.data.ChainManager;
import com.linnca.whispers.data.FirebaseDBHeaders;
import com.linnca.whispers.R;
import com.linnca.whispers.gui.widgets.ActionAfterEndListener;

public class Learn_End extends Fragment implements ActionAfterEndListener,
        TimeExpired.TimeExpiredListener,
        YouAreOffline.YouAreOfflineListener{
    private final String TAG = "Learn_End";
    private final String FRAGMENT_VOICE_RECORDER = "fragmentVoiceRecorder";
    private final String FRAGMENT_TIME_EXPIRED = "fragmentTimeExpired";
    private final String FRAGMENT_OFFLINE = "fragmentOffline";
    public static String BUNDLE_CHAIN_QUEUE_KEY = "chainQueueKey";
    public static String BUNDLE_CHAIN_ID = "chainID";
    public static String BUNDLE_LANGUAGE_CODE = "languageCode";
    public static String BUNDLE_SITUATION_ID = "situationID";

    private final String SAVED_STATE_STOPPED = "stopped";
    //variables are persisted with onStop() so
    //no need to save it in the instance state
    private boolean stopped = false;

    private String chainID;
    private String chainQueueKey;
    private FirebaseDatabase database;
    private String userID;
    //we need these to return the chain queue back into the queue
    private String languageCode;
    private String situationID;
    private boolean cleanlyFinished = false;
    private DatabaseReference onlineRef;
    private ValueEventListener onlineListener;

    private Learn_EndListener listener;

    public interface Learn_EndListener {
        void learnEndToSituationList(boolean completed);
        void learnEndSaveData(String audioPath, String answer, String chainID, String chainQueueKey, String situationID);
        void toOfflineMode();
        //only used to set it null
        void setSituationID(String situationID);
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.d(TAG, "onCreate called");

        if (savedInstanceState != null){
            stopped = savedInstanceState.getBoolean(SAVED_STATE_STOPPED);
        }

        //whether the fragment is restored or initially created, the arguments
        //are the same, and they are the only variables we need.
        //no need to save extra information in the saved instance

        if (!stopped) {
            Bundle bundle = getArguments();
            chainQueueKey = bundle.getString(BUNDLE_CHAIN_QUEUE_KEY);
            chainID = bundle.getString(BUNDLE_CHAIN_ID);
            //note we can also get this by looking at the user's preferences
            languageCode = bundle.getString(BUNDLE_LANGUAGE_CODE);
            situationID = bundle.getString(BUNDLE_SITUATION_ID);

            userID = FirebaseAuth.getInstance().getCurrentUser().getUid();
        } else {
            //prevent putChainBackIntoQueue
            cleanlyFinished = true;
        }

        database = FirebaseDatabase.getInstance();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState){
        return inflater.inflate(R.layout.fragment_wrapper, container, false);
    }

    @Override
    public void onStart(){
        super.onStart();
        getActivity().setTitle(R.string.toolbar_title_learn);
        FragmentManager fragmentManager = getChildFragmentManager();
        if (stopped && fragmentManager.findFragmentByTag(FRAGMENT_VOICE_RECORDER) != null) {
            addTimeExpiredFragment();
        } else {
            onlineRef = database.getReference(".info/connected");
            onlineListener = new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    boolean connected = dataSnapshot.getValue(Boolean.class);
                    if (connected){
                        FragmentManager fragmentManager = getChildFragmentManager();
                        if (fragmentManager.findFragmentByTag(FRAGMENT_VOICE_RECORDER) == null &&
                                fragmentManager.findFragmentByTag(FRAGMENT_TIME_EXPIRED) == null &&
                                fragmentManager.findFragmentByTag(FRAGMENT_OFFLINE) == null
                                ) {
                            addVoiceRecorderFragment();
                        }
                    } else {
                        //!stopped prevents attempting to add the fragment when we press the
                        //home button
                        if (!stopped)
                            addNoConnectionFragment();
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
            listener = (Learn_EndListener) context;
        } catch (Exception e){
            throw new ClassCastException(context.toString() + " must implement listener");
        }
    }

    @Override
    public void saveData(String audioFileName, String answer){
        if (isFirstTime()){
            showIntroductionDialog(audioFileName, answer);
        } else {
            actuallySaveData(audioFileName, answer);
        }
    }

    private void actuallySaveData(final String audioFileName, final String answer){
        //this is done in the main activity so we don't time out before the fragment is closed
        listener.learnEndSaveData(audioFileName, answer, chainID, chainQueueKey, situationID);
        database.getReference(
                FirebaseDBHeaders.TO_LEARN_CHAIN_QUEUE + "/" +
                        languageCode + "/" +
                        situationID + "/" +
                        chainQueueKey + "/" +
                        FirebaseDBHeaders.CHAIN_QUEUE_IN_QUEUE
        ).onDisconnect().cancel();
        chainQueueKey = null;
        cleanlyFinished = true;
    }

    @Override
    public void redirectUser(){
        if (!isFirstTime()) {
            listener.learnEndToSituationList(true);
        }
        //if it is the first time, redirect after the user has dismissed the dialog
    }

    private boolean isFirstTime(){
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        return preferences.getBoolean(
                getString(R.string.preferences_learn_end_first_time_key),true);
    }

    private void showIntroductionDialog(final String audioFileName, final String answer){
        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(getActivity());
        alertDialogBuilder.setTitle(R.string.learn_end_first_time_title);
        alertDialogBuilder.setMessage(R.string.learn_end_first_time_explanation);
        alertDialogBuilder.setPositiveButton(R.string.learn_end_first_time_explanation_confirm, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                actuallySaveData(audioFileName, answer);
                SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getContext());
                SharedPreferences.Editor editor = preferences.edit();
                editor.putBoolean(getString(R.string.preferences_learn_end_first_time_key), false);
                editor.apply();
                redirectUser();
            }
        });

        alertDialogBuilder.show();
    }


    @Override
    public void onStop(){
        //put the chain back into the queue
        // if the user exits without going to the next activity
        if (!getActivity().isChangingConfigurations()) {
            if (!cleanlyFinished){
                database.getReference(
                        FirebaseDBHeaders.TO_LEARN_CHAIN_QUEUE + "/" +
                                languageCode + "/" +
                                situationID + "/" +
                                chainQueueKey + "/" +
                                FirebaseDBHeaders.CHAIN_QUEUE_IN_QUEUE
                ).onDisconnect().cancel();
                putChainBackToQueue();
                listener.setSituationID(null);
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

    @Override
    public void onSaveInstanceState(Bundle outState){
        super.onSaveInstanceState(outState);
        //activity stopped and not just a change in orientation
        if (!getActivity().isChangingConfigurations() || stopped){
            outState.putBoolean(SAVED_STATE_STOPPED, true);
        }
    }


    private void putChainBackToQueue(){
        if (!cleanlyFinished) {
            //put the chain back into queue
            DatabaseReference chainQueueRef = database.getReference(
                    FirebaseDBHeaders.TO_LEARN_CHAIN_QUEUE + "/" +
                            languageCode + "/" +
                            situationID + "/" +
                            chainQueueKey
            );
            ChainManager.putChainBackIntoQueue(chainQueueRef);
            cleanlyFinished = true;
        }
    }

    private void addVoiceRecorderFragment(){
        FragmentManager fragmentManager = getChildFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        Fragment voiceRecorder = new VoiceRecorder();
        Bundle bundle = new Bundle();
        bundle.putString(VoiceRecorder.USER_INPUT_PROMPT_BUNDLE,
                getResources().getString(R.string.voice_recorder_prompt_optional));
        bundle.putInt(VoiceRecorder.USER_INPUT_BUNDLE, VoiceRecorder.USER_INPUT_OPTIONAL);
        voiceRecorder.setArguments(bundle);
        fragmentTransaction.replace(R.id.fragment_layout, voiceRecorder, FRAGMENT_VOICE_RECORDER);
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
        listener.learnEndToSituationList(false);
    }

    @Override
    public void toOfflineMode(){listener.toOfflineMode();}
}
