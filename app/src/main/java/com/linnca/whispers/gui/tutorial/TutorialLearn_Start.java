package com.linnca.whispers.gui.tutorial;

import android.app.Activity;
import android.content.Context;
import android.media.AudioManager;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;

import com.linnca.whispers.data.FirebaseDBHeaders;
import com.linnca.whispers.R;
import com.linnca.whispers.gui.VoicePlayer;
import com.linnca.whispers.gui.widgets.ActionAfterStartListener;

public class TutorialLearn_Start extends Fragment implements ActionAfterStartListener {
    public static String BUNDLE_LANGUAGE_CODE = "languageCode";
    private String languageCode;

    private String audioPath;

    private TutorialLearn_StartListener listener;

    interface TutorialLearn_StartListener{
        void tutorialLearnStartToTutorialLearnEnd();
    }

    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        Bundle bundle = getArguments();
        if (bundle != null && bundle.getString(BUNDLE_LANGUAGE_CODE) != null){
            languageCode = bundle.getString(BUNDLE_LANGUAGE_CODE);
            getAudioFile();
        }

        //makes sure the user can change the audio
        getActivity().setVolumeControlStream(AudioManager.STREAM_MUSIC);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        //can be the same as the original
        View view = inflater.inflate(R.layout.fragment_wrapper, container, false);
        ProgressBar progressBar = (ProgressBar)view.findViewById(R.id.fragment_progress_bar);
        progressBar.setVisibility(View.GONE);
        addVoicePlayerFragment(audioPath);
        return view;
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
            listener = (TutorialLearn_StartListener) context;
        } catch (Exception e){
            throw new ClassCastException(context.toString() + " must implement listener");
        }
    }

    private void getAudioFile(){
        //we want the second teach audio file
        audioPath = FirebaseDBHeaders.STORAGE_TUTORIAL + "/hello2_" + languageCode + ".wav";
    }

    private void addVoicePlayerFragment(String audioPath){
        String voicePlayerTag = "voicePlayer";
        FragmentManager fragmentManager = getChildFragmentManager();
        if (fragmentManager.findFragmentByTag(voicePlayerTag) == null) {
            FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
            Fragment voicePlayer = new VoicePlayer();
            Bundle bundle = new Bundle();
            bundle.putString(VoicePlayer.BUNDLE_AUDIO_FILE_PATH_KEY, audioPath);
            voicePlayer.setArguments(bundle);
            fragmentTransaction.add(R.id.fragment_layout, voicePlayer, voicePlayerTag);
            fragmentTransaction.commit();
        }
    }

    @Override
    public void continueToEnd(){
        listener.tutorialLearnStartToTutorialLearnEnd();
    }

}
