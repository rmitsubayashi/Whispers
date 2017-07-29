package com.linnca.whispers.gui;

import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ProgressBar;

import com.linnca.whispers.R;
import com.linnca.whispers.gui.widgets.ActionAfterStartListener;
import com.linnca.whispers.gui.widgets.ProgressBarAnimation;

public class VoicePlayer extends Fragment {
    private final String TAG = "VoicePlayer";
    public static String BUNDLE_AUDIO_FILE_PATH_KEY = "filePath";
    private final String SAVED_STATE_FILE_PATH = "savedFilePath";
    private MediaPlayer player;
    private boolean playing = false;
    private ImageButton playButton;
    private ProgressBar playProgressBar;
    private Button confirmButton;
    private String filePath;
    private ActionAfterStartListener actionAfterStartListener;

    public VoicePlayer(){

    }

    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        //makes sure the user can change the volume
        getActivity().setVolumeControlStream(AudioManager.STREAM_MUSIC);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState){
        View view = inflater.inflate(R.layout.fragment_voice_player, container, false);
        playButton = (ImageButton) view.findViewById(R.id.voice_player_play_button);
        playProgressBar = (ProgressBar) view.findViewById(R.id.voice_player_play_progress_bar);
        confirmButton = (Button) view.findViewById(R.id.voice_player_confirm_button);

        if (savedInstanceState != null &&
                savedInstanceState.getString(SAVED_STATE_FILE_PATH) != null){
            filePath = savedInstanceState.getString(SAVED_STATE_FILE_PATH);
        } else {
            Bundle dataBundle = getArguments();
            filePath = dataBundle.getString(BUNDLE_AUDIO_FILE_PATH_KEY);
        }
        addListeners();
        implementListeners(getParentFragment());

        return view;
    }

    @Override
    public void onPause(){
        super.onPause();
        stopPlaying();
    }

    @Override
    public void onSaveInstanceState(Bundle outState){
        super.onSaveInstanceState(outState);
        if (getActivity().isChangingConfigurations()){
            outState.putString(SAVED_STATE_FILE_PATH, filePath);
        }
    }

    private void implementListeners(Fragment parentFragment){
        try {
            actionAfterStartListener = (ActionAfterStartListener) parentFragment;
        } catch (Exception e){
            throw new ClassCastException(parentFragment.toString() + " must implement actionAfterStart listener");
        }
    }

    private void onPlay(boolean playing){
        if (!playing){
            Log.d(getClass().getCanonicalName(), "Starting player");
            startPlaying();
        } else {
            Log.d(getClass().getCanonicalName(), "Stopping player");
            stopPlaying();
        }
    }

    private void startPlaying(){
        player = new MediaPlayer();
        //when the audio is done playing,
        //we should stop the player
        player.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mediaPlayer) {
                stopPlaying();
            }
        });
        //prepare the media player asynchronously so it doesn't block the UI thread
        player.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mediaPlayer) {
                player.start();
                playButton.setEnabled(false);
                playing = true;
                int duration = player.getDuration();
                startProgressBar(duration);
            }
        });
        try {
            player.setDataSource(filePath);
            player.prepareAsync();
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    private void startProgressBar(final int duration){
        playProgressBar.setMax(duration);
        playProgressBar.setProgress(0);

        ProgressBarAnimation anim = new ProgressBarAnimation(playProgressBar, 0, duration);
        anim.setDuration(duration);
        anim.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                retractProgress();
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
        playProgressBar.startAnimation(anim);
    }

    private void retractProgress(){
        ProgressBarAnimation anim = new ProgressBarAnimation(playProgressBar, playProgressBar.getProgress(), 0);
        anim.setDuration(500);
        playProgressBar.startAnimation(anim);
    }

    private void stopPlaying(){
        if (playing) {
            player.release();
            player = null;
            playButton.setEnabled(true);
            playing = false;
        }
    }

    private void addListeners(){
        playButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onPlay(playing);
            }
        });
        confirmButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                actionAfterStartListener.continueToEnd();
            }
        });
    }
}
