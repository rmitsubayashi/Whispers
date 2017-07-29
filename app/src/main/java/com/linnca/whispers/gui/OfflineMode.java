package com.linnca.whispers.gui;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.linnca.whispers.data.OfflineModeManager;
import com.linnca.whispers.data.RecordingManager;
import com.linnca.whispers.R;
import com.linnca.whispers.gui.widgets.ActionAfterEndListener;
import com.linnca.whispers.gui.widgets.ActionAfterStartListener;

public class OfflineMode extends Fragment
implements ActionAfterStartListener, ActionAfterEndListener,
        OfflineModeResult.OfflineModeResultListener{
    private final String FRAGMENT_VOICE_PLAYER = "voicePlayer";
    private final String FRAGMENT_FINAL_ANSWER = "finalAnswer";
    private final String FRAGMENT_NO_RECORDINGS = "noRecordings";
    private final String FRAGMENT_RESULTS = "results";
    private String currentFragmentTag;
    private final String SAVED_STATE_CURRENT_FRAGMENT_TAG = "currentFragmentTag";

    private FragmentManager fragmentManager;

    private String response;

    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        fragmentManager = getChildFragmentManager();
        //if there's an orientation change, do nothing
        if (savedInstanceState == null) {
            startOfflineMode();
        } else {
            currentFragmentTag = savedInstanceState.getString(SAVED_STATE_CURRENT_FRAGMENT_TAG);
        }
    }

    @Override
    public void onStart(){
        super.onStart();
        getActivity().setTitle(R.string.toolbar_title_offline_mode);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState){
        return inflater.inflate(R.layout.fragment_offline_mode, container, false);
    }

    @Override
    public void onSaveInstanceState(Bundle outState){
        super.onSaveInstanceState(outState);
        outState.putString(SAVED_STATE_CURRENT_FRAGMENT_TAG, currentFragmentTag);
    }

    public void handleBackPress(){
        //just re-start the flow
        if (currentFragmentTag.equals(FRAGMENT_FINAL_ANSWER))
            startOfflineMode();
        else if (currentFragmentTag.equals(FRAGMENT_RESULTS))
            addFinalAnswerFragment();
    }

    //true means this fragment should handle the back press
    public boolean shouldHandleBackPress(){
        return currentFragmentTag.equals(FRAGMENT_FINAL_ANSWER) || currentFragmentTag.equals(FRAGMENT_RESULTS);
    }

    private void startOfflineMode(){
        String fileName = OfflineModeManager.getNextRecordingFileName(getContext());
        if (fileName.equals(OfflineModeManager.NO_RECORDINGS)){
            addNoRecordingsFragment();
        } else {
            addVoicePlayerFragment(fileName);
        }
    }

    //handle back stack management in the main activity because
    //nested fragments can't be stored in the back stack
    private void addVoicePlayerFragment(String fileName){
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        Fragment voicePlayer = new VoicePlayer();
        Bundle bundle = new Bundle();
        bundle.putString(VoicePlayer.BUNDLE_AUDIO_FILE_PATH_KEY,
                RecordingManager.getInternalStorageFilePath(getContext(), fileName));
        voicePlayer.setArguments(bundle);
        fragmentTransaction.replace(R.id.fragment_offline_mode, voicePlayer, FRAGMENT_VOICE_PLAYER);
        fragmentTransaction.commit();
        currentFragmentTag =FRAGMENT_VOICE_PLAYER;
    }

    private void addFinalAnswerFragment(){
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        Fragment finalAnswer = new FinalAnswer();
        fragmentTransaction.replace(R.id.fragment_offline_mode, finalAnswer, FRAGMENT_FINAL_ANSWER);
        fragmentTransaction.commit();
        currentFragmentTag = FRAGMENT_FINAL_ANSWER;
    }

    private void addNoRecordingsFragment(){
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        Fragment noRecordings = new OfflineModeNoRecordings();
        fragmentTransaction.replace(R.id.fragment_offline_mode, noRecordings, FRAGMENT_NO_RECORDINGS);
        fragmentTransaction.commit();
        currentFragmentTag = FRAGMENT_NO_RECORDINGS;
    }

    private void addResultFragment(String response){
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        Fragment results = new OfflineModeResult();
        Bundle bundle = new Bundle();
        bundle.putString(OfflineModeResult.BUNDLE_RESPONSE, response);
        results.setArguments(bundle);
        fragmentTransaction.replace(R.id.fragment_offline_mode, results, FRAGMENT_RESULTS);
        fragmentTransaction.commit();
        currentFragmentTag = FRAGMENT_RESULTS;
    }

    @Override
    public void continueToEnd(){
        addFinalAnswerFragment();
    }

    @Override
    public void saveData(String audioFile, String answer){
        //audio file is null since we only take written responses for offline mode
        this.response = answer;
    }

    @Override
    public void redirectUser(){
        addResultFragment(response);
    }

    @Override
    public void toNextRecording(){
        if (OfflineModeManager.removeNextRecordingFile(getContext())){
            Log.d("OfflineMode", "successfully removed recording file");
        }
        if (OfflineModeManager.removeNextAnswerFile(getContext())){
            Log.d("OfflineMode","successfully removed answer file");
        }
        startOfflineMode();
    }
}
