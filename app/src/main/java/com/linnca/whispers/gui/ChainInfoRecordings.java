package com.linnca.whispers.gui;

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
import android.view.ViewManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.ViewFlipper;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.linnca.whispers.data.ChainManager;
import com.linnca.whispers.data.FirebaseDBHeaders;
import com.linnca.whispers.data.RecordingManager;
import com.linnca.whispers.data.datawrappers.ChainLink;
import com.linnca.whispers.data.datawrappers.MinimalChain;
import com.linnca.whispers.R;
import com.linnca.whispers.gui.widgets.ProgressBarAnimation;

public class ChainInfoRecordings extends Fragment {
    private final String TAG = "ChainInfoRecordings";
    public static String CHAIN_INFO_RECORDINGS_MINIMAL_CHAIN;
    private MinimalChain minimalChain;
    private FirebaseDatabase database;
    private DatabaseReference chainInfoRef;
    private ValueEventListener listener;
    //save so we can remove them
    private List<String> audioFileNames = new ArrayList<>(5);
    private List<Button> chainLinkButtons = new ArrayList<>(5);
    private int selectedButtonIndex = -1;
    private Set<Integer> myButtonIndexes = new HashSet<>(5);
    private String userID;
    private MediaPlayer player;
    private ImageButton playButton;
    private String currentFileName;
    private boolean playing = false;
    private ProgressBar playProgressBar;
    private ViewFlipper guessViewFlipper;
    private Animation inLeft;
    private Animation outRight;
    private Animation inRight;
    private Animation outLeft;
    private ViewGroup successfulLayout;
    private TextView successfulTextView;
    private Button successfulCloseButton;

    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        database = FirebaseDatabase.getInstance();
        userID = FirebaseAuth.getInstance().getCurrentUser().getUid();
        Bundle bundle = getArguments();
        minimalChain = (MinimalChain)bundle.getSerializable(CHAIN_INFO_RECORDINGS_MINIMAL_CHAIN);
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

        guessViewFlipper = (ViewFlipper)view.findViewById(R.id.chain_info_recordings_guess_flipper);
        loadAnimations();
        playButton = (ImageButton)view.findViewById(R.id.chain_info_recordings_play_button);
        disablePlayButton();
        playProgressBar = (ProgressBar)view.findViewById(R.id.chain_info_recordings_play_progress_bar);

        successfulLayout = (ViewGroup)view.findViewById(R.id.chain_info_recordings_successful_layout);
        successfulTextView = (TextView)view.findViewById(R.id.chain_info_recordings_successful_textview);
        successfulCloseButton = (Button)view.findViewById(R.id.chain_info_recordings_successful_close);

        playButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onPlay(playing);
            }
        });

        return view;
    }

    @Override
    public void onStart(){
        super.onStart();
        populateData();
    }

    private void populateData(){
        chainInfoRef = database.getReference(
                FirebaseDBHeaders.CHAINS + "/" +
                        minimalChain.getChainID()
        );

        //constantly listen for a new link
        listener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                DataSnapshot linkSnapshot = dataSnapshot.child(
                        FirebaseDBHeaders.CHAINS_ID_LINKS
                );
                List<ChainLink> links = new ArrayList<>(5);
                for (DataSnapshot child : linkSnapshot.getChildren()){
                    ChainLink link = child.getValue(ChainLink.class);
                    links.add(link);
                }

                updateLinks(links);

                //if completed, show the notification to show whether the chain was successful
                if (linkSnapshot.getChildrenCount() == 5) {
                    String answer = links.get(4).getAnswer();
                    //fetch the answer from the phrase and compare.
                    String phraseID = dataSnapshot.child(
                            FirebaseDBHeaders.CHAINS_ID_PHRASE_ID
                    ).getValue(String.class);
                    String situationID = dataSnapshot.child(
                            FirebaseDBHeaders.CHAINS_ID_SITUATION_ID
                    ).getValue(String.class);
                    String languageCode = dataSnapshot.child(
                            FirebaseDBHeaders.CHAINS_ID_LANGUAGE_CODE
                    ).getValue(String.class);
                    showSuccessfulDialogue(phraseID, situationID, languageCode, answer);
                }
                //no need to reset the layout because
                // a chain can never go back, only progress
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        };
        chainInfoRef.addValueEventListener(listener);
    }

    private void loadAnimations(){
        inLeft = AnimationUtils.loadAnimation(getContext(),R.anim.slide_in_left);
        outRight = AnimationUtils.loadAnimation(getContext(), R.anim.slide_out_right);
        inRight = AnimationUtils.loadAnimation(getContext(), R.anim.slide_in_right);
        outLeft = AnimationUtils.loadAnimation(getContext(), R.anim.slide_out_left);
    }

    private void setFlipperAnimation(int startIndex, int endIndex){
        if (startIndex > endIndex){
            guessViewFlipper.setInAnimation(inLeft);
            guessViewFlipper.setOutAnimation(outRight);
        } else if (startIndex < endIndex){
            guessViewFlipper.setInAnimation(inRight);
            guessViewFlipper.setOutAnimation(outLeft);
        } else {
            guessViewFlipper.setInAnimation(null);
            guessViewFlipper.setOutAnimation(null);
        }
    }

    private void showSuccessfulDialogue
            (String phraseID, String situationID, final String languageCode, final String answer){
        DatabaseReference phraseRef = FirebaseDatabase.getInstance().getReference(
                FirebaseDBHeaders.SITUATIONS + "/" +
                        situationID + "/" +
                        FirebaseDBHeaders.SITUATIONS_ID_PHRASES + "/" +
                        phraseID + "/" +
                        languageCode
        );

        phraseRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                String correctAnswer = dataSnapshot.getValue(String.class);
                if (ChainManager.checkAnswers(correctAnswer, answer, languageCode)){
                    //green
                    successfulLayout.setBackgroundColor(
                            ContextCompat.getColor(getContext(),R.color.colorAccent)
                    );

                    successfulTextView.setText(R.string.chain_info_recordings_successful);
                } else {
                    successfulLayout.setBackgroundColor(
                            ContextCompat.getColor(getContext(), R.color.red)
                    );
                    String incorrectText = getString(R.string.chain_info_recordings_failed, correctAnswer);
                    successfulTextView.setText(incorrectText);
                }

                successfulLayout.setVisibility(View.VISIBLE);
                successfulCloseButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        ((ViewManager)successfulLayout.getParent()).removeView(successfulLayout);
                    }
                });
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    private void updateLinks(List<ChainLink> links){
        for (int i=0; i<links.size(); i++){
            ChainLink link = links.get(i);
            //we want a blank if this is the first person
            final String answer = i == 0 ? "?" : link.getAnswer();
            String linkUserID = link.getUserID();
            boolean myLink = linkUserID.equals(userID);
            if (myLink)
                myButtonIndexes.add(i);
            final Button linkButton = chainLinkButtons.get(i);
            enableButton(linkButton, myLink);


            guessViewFlipper.addView(getGuessView(i, answer, myLink));

            //download associated audio file
            final String audioFileName = link.getAudioFileName();
            //there does not need to be an audio file (last link)
            if (audioFileName == null) {
                linkButton.setOnClickListener(linkListener(i, null, answer));
            } else {
                final int finalIndex = i;
                final String absoluteFilePath = RecordingManager.getInternalStorageFilePath(getContext(), audioFileName);
                //set the first chain link as the default selected button
                if (i == 0) {
                    selectButton(i, absoluteFilePath);
                }

                RecordingManager.OnSaveRecordingListener onSaveRecordingListener = new RecordingManager.OnSaveRecordingListener() {
                    @Override
                    public void onSaveRecording() {
                        linkButton.setOnClickListener(linkListener(finalIndex, absoluteFilePath, answer));
                        enablePlayButton();
                        audioFileNames.add(audioFileName);
                    }
                };

                RecordingManager.saveRecordingToInternalStorage(getContext(), audioFileName, audioFileName, onSaveRecordingListener);
            }
        }
    }

    private View getGuessView(int index, String answer, boolean isUser){
        if (index == 0){
            //empty view for the first??
            return new View(getContext());
        }
        View view = LayoutInflater.from(getContext()).inflate(
                R.layout.inflatable_chain_info_recordings_guess, guessViewFlipper, false);
        ViewGroup layout = (ViewGroup)view.findViewById(R.id.chain_info_recordings_guess_layout);
        int layoutBackground = isUser ? R.drawable.box_accent_color_border : R.drawable.box_primary_color_border;
        layout.setBackgroundResource(layoutBackground);
        TextView guessTextView = (TextView)view.findViewById(R.id.chain_info_recordings_guess);
        if (answer.length() != 0) {
            guessTextView.setText(answer);
        } else {
            guessTextView.setTextColor(
                    ContextCompat.getColor(getContext(), R.color.gray)
            );
            guessTextView.setText(R.string.chain_info_recordings_no_answer);
        }
        TextView guessTitleTextView = (TextView)view.findViewById(R.id.chain_info_recordings_guess_title);
        guessTitleTextView.setText(getString(R.string.chain_info_recordings_guess_title, index));

        return view;
    }

    private View.OnClickListener linkListener(final int buttonIndex, final String audioFileName, final String answer){
        return new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setFlipperAnimation(selectedButtonIndex, buttonIndex);
                selectButton(buttonIndex, audioFileName);
                guessViewFlipper.setDisplayedChild(buttonIndex);
            }
        };
    }

    private void enableButton(Button button, boolean myButton){
        button.setEnabled(true);
        if (myButton){
            button.setTextColor(
                    ContextCompat.getColor(getContext(), R.color.colorAccent)
            );
        } else {
            button.setTextColor(
                    ContextCompat.getColor(getContext(), R.color.colorPrimary)
            );
        }
    }

    private void selectButton(int index, String audioFileName){
        //same button as the one selected, so do nothing
        if (index == selectedButtonIndex)
            return;

        Button selectButton = chainLinkButtons.get(index);
        if (selectedButtonIndex != -1){
            Button previousButton = chainLinkButtons.get(selectedButtonIndex);
            if (myButtonIndexes.contains(selectedButtonIndex)){
                previousButton.setTextColor(
                        ContextCompat.getColor(getContext(), R.color.colorAccent)
                );
            } else {
                previousButton.setTextColor(
                        ContextCompat.getColor(getContext(), R.color.colorPrimary)
                );
            }
            previousButton.setTypeface(null, Typeface.NORMAL);
        }

        if (myButtonIndexes.contains(index)){
            selectButton.setTextColor(
                    ContextCompat.getColor(getContext(), R.color.colorAccentDark)
            );
        } else {
            selectButton.setTextColor(
                    ContextCompat.getColor(getContext(), R.color.colorPrimaryDark)
            );
        }
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

    @Override
    public void onStop(){
        super.onStop();
        if (chainInfoRef != null && listener != null){
            chainInfoRef.removeEventListener(listener);
        }

        for (String toRemove : audioFileNames){
            RecordingManager.removeRecordingFromInternalStorage(getContext(), toRemove);
        }
        audioFileNames.clear();
        chainLinkButtons.clear();
        selectedButtonIndex = -1;
    }
}
