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

import com.linnca.whispers.R;
import com.linnca.whispers.gui.VoiceRecorder;
import com.linnca.whispers.gui.widgets.ActionAfterEndListener;

public class TutorialLearn_End extends Fragment implements ActionAfterEndListener {
    private TutorialLearn_EndListener listener;
    private String audioPath;
    private String answer;

    public interface TutorialLearn_EndListener {
        void tutorialLearnEndToStart(String audioPath, String answer);
    }

    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        //makes sure the user can change the audio
        getActivity().setVolumeControlStream(AudioManager.STREAM_MUSIC);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState){
        View view = inflater.inflate(R.layout.fragment_wrapper, container, false);
        addVoiceRecorderFragment();
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
            listener = (TutorialLearn_EndListener) context;
        } catch (Exception e){
            throw new ClassCastException(context.toString() + " must implement listener");
        }
    }

    @Override
    public void saveData(final String audioFileName, final String answer){
        //since we are reading the audio file diectly from cache,
        //add the cache path to the audio path
        audioPath = getActivity().getExternalCacheDir().getAbsolutePath()
        + audioFileName;
        this.answer = answer;
    }

    @Override
    public void redirectUser(){
        listener.tutorialLearnEndToStart(audioPath, answer);
    }

    private void addVoiceRecorderFragment(){
        String fragmentTag = "fragmentTag";
        FragmentManager fragmentManager = getChildFragmentManager();
        //if the fragment is already added (orientation change),
        //don't re-add the fragment
        if (fragmentManager.findFragmentByTag(fragmentTag) != null)
            return;
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        Fragment voiceRecorder = new VoiceRecorder();
        Bundle bundle = new Bundle();
        bundle.putString(VoiceRecorder.USER_INPUT_PROMPT_BUNDLE,
                getResources().getString(R.string.voice_recorder_prompt_optional));
        bundle.putInt(VoiceRecorder.USER_INPUT_BUNDLE, VoiceRecorder.USER_INPUT_OPTIONAL);
        voiceRecorder.setArguments(bundle);
        fragmentTransaction.replace(R.id.fragment_layout, voiceRecorder, fragmentTag);
        fragmentTransaction.commit();
    }

}
