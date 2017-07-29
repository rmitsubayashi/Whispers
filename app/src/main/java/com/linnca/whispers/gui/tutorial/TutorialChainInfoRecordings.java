package com.linnca.whispers.gui.tutorial;

import android.graphics.Typeface;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.linnca.whispers.data.FirebaseDBHeaders;
import com.linnca.whispers.data.LanguageIDs;
import com.linnca.whispers.R;
import com.linnca.whispers.gui.widgets.AudioSettings;
import com.linnca.whispers.gui.widgets.ProgressBarAnimation;

public class TutorialChainInfoRecordings extends Fragment {
    public static final String BUNDLE_AUDIO_PATH = "audioPath";
    public static final String BUNDLE_ANSWER = "answer";
    public static final String BUNDLE_LANGUAGE_CODE = "languageCode";
    private String audioPath;
    private String answer;
    private String languageCode;

    private List<Button> chainLinkButtons = new ArrayList<>(5);
    private int selectedButtonIndex = -1;
    private MediaPlayer player;
    private ImageButton playButton;
    private String currentFileName;
    private boolean playing = false;
    private ProgressBar playProgressBar;
    private TextView guessTextView;
    private ViewGroup successfulLayout;
    private TextView successfulTextView;
    private Button successfulCloseButton;

    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        Bundle bundle = getArguments();
        audioPath = bundle.getString(BUNDLE_AUDIO_PATH);
        answer = bundle.getString(BUNDLE_ANSWER);
        languageCode = bundle.getString(BUNDLE_LANGUAGE_CODE);

        //makes sure the user can change the audio
        getActivity().setVolumeControlStream(AudioManager.STREAM_MUSIC);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState){
        View view = inflater.inflate(R.layout.fragment_chain_info_recordings ,container, false);
        Button link1Button = (Button)view.findViewById(R.id.chain_info_recordings_link1_button);
        chainLinkButtons.add(link1Button);
        Button link2Button = (Button)view.findViewById(R.id.chain_info_recordings_link2_button);
        chainLinkButtons.add(link2Button);
        Button link3Button = (Button)view.findViewById(R.id.chain_info_recordings_link3_button);
        chainLinkButtons.add(link3Button);
        Button link4Button = (Button)view.findViewById(R.id.chain_info_recordings_link4_button);
        chainLinkButtons.add(link4Button);
        Button link5Button = (Button)view.findViewById(R.id.chain_info_recordings_link5_button);
        chainLinkButtons.add(link5Button);

        guessTextView = (TextView)view.findViewById(R.id.chain_info_recordings_guess);
        playButton = (ImageButton)view.findViewById(R.id.chain_info_recordings_play_button);
        disablePlayButton();
        playProgressBar = (ProgressBar)view.findViewById(R.id.chain_info_recordings_play_progress_bar);

        successfulLayout = (ViewGroup)view.findViewById(R.id.chain_info_recordings_successful_layout);
        successfulTextView = (TextView)view.findViewById(R.id.chain_info_recordings_successful_textview);
        successfulCloseButton = (Button)view.findViewById(R.id.chain_info_recordings_successful_close);

        populateData();
        alertSuccess();

        playButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onPlay(playing);
            }
        });

        return view;
    }

    private void populateData(){
        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference audioRef = storage.getReference(
                FirebaseDBHeaders.STORAGE_TUTORIAL
        );

        //manually input all 5 links
        final String correctAnswer;
        final String incorrectAnswer;
        switch (languageCode){
            case LanguageIDs.ENGLISH :
                correctAnswer = getString(R.string.tutorial_chain_info_recordings_phrase_en);
                incorrectAnswer =getString(R.string.tutorial_chain_info_recordings_phrase_incorrect_en);
                break;
            case LanguageIDs.JAPANESE :
                correctAnswer = getString(R.string.tutorial_chain_info_recordings_phrase_jp);
                incorrectAnswer = getString(R.string.tutorial_chain_info_recordings_phrase_incorrect_jp);
                break;
            default :
                correctAnswer = "";
                incorrectAnswer = "";
        }
        final Button firstLinkButton = chainLinkButtons.get(0);
        enableButton(firstLinkButton);
        String firstLinkFile = "hello1_" + languageCode + ".wav";
        try {
            File tempFile = File.createTempFile(firstLinkFile, AudioSettings.EXTENSION);
            final String tempFilePath = tempFile.getAbsolutePath();
            //set the first chain link as the default selected button
            selectButton(0, tempFilePath);
            //no need to set the guess text view because
            //the first one does not guess?
            guessTextView.setText(answer);

            audioRef.child(firstLinkFile).getFile(tempFile).addOnSuccessListener(
                    new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
                        @Override
                        public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot) {
                            firstLinkButton.setOnClickListener(linkListener(0, tempFilePath, "?"));
                            enablePlayButton();
                        }
                    }
            );
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }

        final Button secondLinkButton = chainLinkButtons.get(1);
        enableButton(secondLinkButton);
        String secondLinkFile = "hello1_" + languageCode + ".wav";
        try {
            File tempFile = File.createTempFile(secondLinkFile, AudioSettings.EXTENSION);
            final String tempFilePath = tempFile.getAbsolutePath();

            audioRef.child(secondLinkFile).getFile(tempFile).addOnSuccessListener(
                    new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
                        @Override
                        public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot) {
                            secondLinkButton.setOnClickListener(linkListener(1, tempFilePath, incorrectAnswer));
                            enablePlayButton();
                        }
                    }
            );
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }

        final Button thirdLinkButton = chainLinkButtons.get(2);
        enableButton(thirdLinkButton);
        String thirdLinkFile = "hello2_" + languageCode + ".wav";
        try {
            File tempFile = File.createTempFile(thirdLinkFile, AudioSettings.EXTENSION);
            final String tempFilePath = tempFile.getAbsolutePath();

            audioRef.child(thirdLinkFile).getFile(tempFile).addOnSuccessListener(
                    new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
                        @Override
                        public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot) {
                            thirdLinkButton.setOnClickListener(linkListener(2, tempFilePath, correctAnswer));
                            enablePlayButton();
                        }
                    }
            );
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }

        Button fourthLinkButton = chainLinkButtons.get(3);
        enableButton(fourthLinkButton);
        fourthLinkButton.setOnClickListener(linkListener(3, audioPath, answer));

        Button fifthLinkButton = chainLinkButtons.get(4);
        enableButton(fifthLinkButton);
        fifthLinkButton.setOnClickListener(linkListener(4, null, correctAnswer));
    }

    private void alertSuccess(){
        //green
        successfulLayout.setBackgroundColor(
                ContextCompat.getColor(getContext(),R.color.colorAccent)
        );

        successfulTextView.setText(R.string.chain_info_recordings_successful);
        successfulLayout.setVisibility(View.VISIBLE);
    }

    private void enableButton(Button button){
        button.setEnabled(true);
        button.setTextColor(
                ContextCompat.getColor(getContext(), R.color.colorPrimary)
        );
    }

    private View.OnClickListener linkListener(final int buttonIndex, final String audioFileName, final String answer){
        return new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                selectButton(buttonIndex, audioFileName);
                guessTextView.setText(answer);
            }
        };
    }

    private void selectButton(int index, String audioFileName){
        //same button as the one selected, so do nothing
        if (index == selectedButtonIndex)
            return;

        Button selectButton = chainLinkButtons.get(index);
        if (selectedButtonIndex != -1){
            Button previousButton = chainLinkButtons.get(selectedButtonIndex);
            previousButton.setTextColor(
                    ContextCompat.getColor(getContext(), R.color.colorPrimary)
            );
            previousButton.setTypeface(null, Typeface.NORMAL);
        }

        selectButton.setTextColor(
                ContextCompat.getColor(getContext(), R.color.colorPrimaryDark)
        );
        selectButton.setTypeface(null, Typeface.BOLD);

        if (playing){
            stopPlaying();
            playProgressBar.clearAnimation();
        }

        selectedButtonIndex = index;
        currentFileName = audioFileName;

    }

    private void disablePlayButton(){
        playButton.setEnabled(false);
        if (Build.VERSION.SDK_INT < 16)
            playButton.setAlpha(0.5f);
        else
            playButton.setImageAlpha(140);

        if (Build.VERSION.SDK_INT > 20)
            playButton.setElevation(1);
    }

    private void enablePlayButton(){
        playButton.setEnabled(true);
        if (Build.VERSION.SDK_INT < 16)
            playButton.setAlpha(1);
        else
            playButton.setImageAlpha(255);

        if (Build.VERSION.SDK_INT > 20)
            playButton.setElevation(8);
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
        //user clicks on last link
        if (currentFileName == null)
            return;

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
            player.setDataSource(currentFileName);
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

    @Override
    public void onPause(){
        super.onPause();
        stopPlaying();
    }

}
