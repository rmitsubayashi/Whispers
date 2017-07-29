package com.linnca.whispers.gui;

import android.Manifest;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.PorterDuff;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.speech.RecognizerIntent;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import com.linnca.whispers.data.RecordingManager;
import com.linnca.whispers.R;
import com.linnca.whispers.gui.widgets.ActionAfterEndListener;
import com.linnca.whispers.gui.widgets.AudioSettings;
import com.linnca.whispers.gui.widgets.ProgressBarAnimation;

import static android.app.Activity.RESULT_OK;

public class VoiceRecorder extends Fragment {
    private final String TAG = "VoiceRecorder";
    public static String USER_INPUT_PROMPT_BUNDLE = "userInputPrompt";
    public static String USER_INPUT_BUNDLE = "userInput";
    public static int USER_INPUT_REQUIRED = 0;
    public static int USER_INPUT_OPTIONAL = 1;
    public static int USER_INPUT_GONE = 2;

    private final int REQUEST_VOICE_RECOGNITION = 640;


    private final String SAVED_STATE_CURRENT_FILE_NAME = "savedCurrentFileName";
    private final String SAVED_STATE_USER_INPUT = "savedUserInput";
    private final String SAVED_STATE_USER_PROMPT = "savedUserPrompt";
    private final String SAVED_STATE_USER_INPUT_CONFIGURATIONS = "savedUserInputConfigurations";

    private ViewGroup layout;
    private ImageButton recordButton;
    private ImageButton playButton;
    private ProgressBar playProgressBar;
    private MediaRecorder recorder;
    private boolean recording = false;
    private MediaPlayer player;
    private boolean playing = false;
    private String currentFileName = null;

    private boolean shouldUpload = false;

    private ActionAfterEndListener actionAfterEndListener;
    private Button confirmButton;
    private int userInputConfigurations;
    private boolean showUserInputPrompt = false;
    private boolean userInputPromptOptional = true;
    private TextView userInputPromptTextView;
    private String userInputPrompt;
    private EditText userInputEditText;
    private TextView recordingErrorMessage;
    private String uniqueID;

    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 200;
    private boolean recordPermissionGranted = false;


    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        Activity activity = getActivity();
        //we need to get permission from the user to use the microphone
        recordPermissionGranted = checkAudioPermission();
        if (!recordPermissionGranted) {
            String[] permissions = new String[]{Manifest.permission.RECORD_AUDIO};
            requestPermissions(permissions, REQUEST_RECORD_AUDIO_PERMISSION);
        }

        //if the user doesn't exist, that means this is part of the tutorial
        if (FirebaseAuth.getInstance().getCurrentUser() != null)
            uniqueID = FirebaseAuth.getInstance().getCurrentUser().getUid();
        else
            //this is saved locally (not on database) so it really doesn't matter
            uniqueID = "tutorial";

        userInputPrompt = getArguments().getString(USER_INPUT_PROMPT_BUNDLE);
        userInputConfigurations = getArguments().getInt(USER_INPUT_BUNDLE);
        configureUserInputPrompt(userInputConfigurations);

        //makes sure the user can change the audio (not the notification audio)
        getActivity().setVolumeControlStream(AudioManager.STREAM_MUSIC);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_voice_recorder, container, false);
        layout = (ViewGroup) view.findViewById(R.id.voice_recorder_layout);
        recordButton = (ImageButton) view.findViewById(R.id.voice_recorder_record_button);
        playButton = (ImageButton) view.findViewById(R.id.voice_recorder_play_button);
        playProgressBar = (ProgressBar) view.findViewById(R.id.voice_recorder_play_progress_bar);
        disablePlayButton();
        confirmButton = (Button) view.findViewById(R.id.voice_recorder_confirm_button);
        userInputEditText = (EditText) view.findViewById(R.id.voice_recorder_user_input_edit_text);
        userInputPromptTextView = (TextView) view.findViewById(R.id.voice_recorder_user_input_prompt);
        recordingErrorMessage = (TextView) view.findViewById(R.id.voice_recorder_recording_error_message);

        addListeners();

        //re-populate fields if the user restores the fragment
        if (savedInstanceState != null){
            userInputPrompt = savedInstanceState.getString(SAVED_STATE_USER_PROMPT);
            userInputConfigurations = savedInstanceState.getInt(SAVED_STATE_USER_INPUT_CONFIGURATIONS);
            configureUserInputPrompt(userInputConfigurations);
            
            if (savedInstanceState.getString(SAVED_STATE_CURRENT_FILE_NAME) != null){
                currentFileName = savedInstanceState.getString(SAVED_STATE_CURRENT_FILE_NAME);
                //before the user has a recording, the play button is disabled.
                //since we are restoring the recording, enable the play button
                enablePlayButton();
            }

            if (savedInstanceState.getString(SAVED_STATE_USER_INPUT) != null){
                String input = savedInstanceState.getString(SAVED_STATE_USER_INPUT);
                userInputEditText.setText(input);
            }
        } else {
            setUserInputPrompt();
        }

        return view;
    }

    //we need to ask for permission to use the recorder
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode){
            case REQUEST_RECORD_AUDIO_PERMISSION:
                recordPermissionGranted  = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                break;
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data){
        unlockScreenOrientation();
        if (requestCode == REQUEST_VOICE_RECOGNITION) {
            //the user doesn't record anything
            if (resultCode != RESULT_OK){
                return;
            }

            // the resulting text is in the getExtras:
            Bundle bundle = data.getExtras();
            ArrayList<String> matches = bundle.getStringArrayList(RecognizerIntent.EXTRA_RESULTS);
            // the recording url is in getData:
            Uri audioUri = data.getData();
            ContentResolver contentResolver = getActivity().getContentResolver();
            try {
                InputStream fileStream = contentResolver.openInputStream(audioUri);
                byte[] buffer = new byte[fileStream.available()];
                fileStream.read(buffer);
                //we should remove the previous file before creating a new one
                RecordingManager.removeRecordingFromInternalStorage(getContext(), currentFileName);
                refreshCurrentFileName();
                RecordingManager.OnSaveRecordingListener listener = new RecordingManager.OnSaveRecordingListener() {
                    @Override
                    public void onSaveRecording() {
                        enablePlayButton();
                    }
                };
                RecordingManager.writeToInternalStorage(getContext(), buffer, currentFileName, listener);
                fileStream.close();

            } catch (Exception exception) {
                exception.printStackTrace();
            }
        }
    }

    @Override
    public void onStart(){
        super.onStart();
        setUserInputPrompt();
    }

    @Override
    public void onStop(){
        super.onStop();
        if (recorder != null){
            stopRecording();
        }

        if (player != null){
            stopPlaying();
        }

        //don't remove anything on orientation change.
        //if the file is to be uploaded, the cached file will be removed
        //after the upload.
        if (!getActivity().isChangingConfigurations() && !shouldUpload) {
            if (!uniqueID.equals("tutorial")){
                //if this is a normal run,
                //we should remove the file
                //if the user exits the app
                //without finishing the activity
                RecordingManager.removeRecordingFromInternalStorage(getContext(), currentFileName);
            }
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        Fragment parentFragment = getParentFragment();
        implementListeners(parentFragment);
    }

    //must implement to account for lower APIs
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        Fragment parentFragment = getParentFragment();
        implementListeners(parentFragment);
    }

    private void implementListeners(Fragment parentFragment){
        try {
            actionAfterEndListener = (ActionAfterEndListener) parentFragment;
        } catch (Exception e){
            throw new ClassCastException(parentFragment.toString() + " must implement listener");
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState){
        super.onSaveInstanceState(outState);

        if (getActivity().isChangingConfigurations()){
            outState.putString(SAVED_STATE_USER_PROMPT, userInputPrompt);
            outState.putInt(SAVED_STATE_USER_INPUT_CONFIGURATIONS, userInputConfigurations);
            if (userInputEditText.getText().length() != 0){
                outState.putString(SAVED_STATE_USER_INPUT, userInputEditText.getText().toString());
            }

            if (currentFileName != null){
                outState.putString(SAVED_STATE_CURRENT_FILE_NAME, currentFileName);
            }
        }
    }

    private boolean checkAudioPermission(){
        int permissionCheck = ActivityCompat.checkSelfPermission(getContext(),
                Manifest.permission.RECORD_AUDIO);
        return permissionCheck == PackageManager.PERMISSION_GRANTED;
    }

    private void refreshCurrentFileName(){
        DateTime dateTime = DateTime.now();
        DateTimeFormatter dtf = DateTimeFormat.forPattern("MMdd_HHmmss");
        String dateTimeIdentifier = dtf.print(dateTime);
        currentFileName = uniqueID + dateTimeIdentifier + AudioSettings.EXTENSION;
    }

    private void onPlay(boolean playing){
        if (!playing){
            startPlaying();
        } else {
            stopPlaying();
        }
    }

    private void onRecord(boolean recording){
        if (!recording) {
            startRecording();
        } else {
            stopRecording();
        }
    }

    private void startPlaying(){
        player = new MediaPlayer();
        player.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mediaPlayer) {
                stopPlaying();
            }
        });
        try {
            player.setDataSource(RecordingManager.getInternalStorageFilePath(getContext(),currentFileName));
            player.prepare();
            player.start();

            playButton.setEnabled(false);
            playing = true;
            int duration = player.getDuration();
            startProgressBar(duration);
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
        player.release();
        player = null;
        playButton.setEnabled(true);
        playing = false;
    }

    private void startRecording(){
        RecordingManager.removeRecordingFromInternalStorage(getContext(), currentFileName);
        refreshCurrentFileName();
        //we don't want the user to try to finish without stopping the recording
        confirmButton.setEnabled(false);
        disablePlayButton();
        recorder = new MediaRecorder();
        recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        recorder.setOutputFormat(AudioSettings.OUTPUT_FORMAT);
        recorder.setOutputFile(RecordingManager.getInternalStorageFilePath(getContext(), currentFileName));
        recorder.setAudioEncoder(AudioSettings.AUDIO_ENCODER);
        try {
            recorder.prepare();
            recorder.start();
            recordButton.setImageResource(R.drawable.ic_stop);
            recording = true;
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    private void stopRecording(){
        if (recorder == null)
            return;
        recorder.stop();
        recorder.release();
        recorder = null;
        recordButton.setImageResource(R.drawable.ic_record);
        recording = false;

        //now the user should be able to click the buttons
        confirmButton.setEnabled(true);
        enablePlayButton();
    }

    private void disablePlayButton(){
        playButton.setEnabled(false);
        playButton.setAlpha(0.5f);

        if (Build.VERSION.SDK_INT > 20)
            playButton.setElevation(0);
    }

    private void enablePlayButton(){
        playButton.setEnabled(true);
        playButton.setAlpha(1f);

        if (Build.VERSION.SDK_INT > 20)
            playButton.setElevation(8);
    }

    private void onConfirm(){
        //no recording
        if (currentFileName == null){
            recordingErrorMessage.setText(R.string.voice_recorder_recording_error_message);
            return;
        }
        String answer = userInputEditText.getText().toString();
        if (showUserInputPrompt && !userInputPromptOptional){
            //if required, this can't be empty
            if (answer.equals("")){
                //show error
                userInputEditText.getBackground().setColorFilter(
                        ContextCompat.getColor(getContext(), R.color.errorRed), PorterDuff.Mode.SRC_IN
                );
                return;
            }
        }
        //make sure we call this before uploading because
        // the file is removed after uploading
        shouldUpload = true;

        actionAfterEndListener.saveData(currentFileName, answer);

        actionAfterEndListener.redirectUser();
    }

    private void addListeners(){
        //check to see if we can use the google api to record
        PackageManager packageManager = getContext().getPackageManager();
        List activities = packageManager.queryIntentActivities(
                new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH), 0
        );
        //google api exists
        if (activities.size() > 0) {
            recordButton.setOnClickListener(googleVoiceRecorder());
        }
        //google api doesn't exist
        else {
            recordButton.setOnClickListener(nativeVoiceRecorder());
        }

        playButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onPlay(playing);
            }
        });

        confirmButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onConfirm();
            }
        });

        userInputEditText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean focused) {
                int color;
                if (focused){
                    //if there was an error, we should clear the red underline
                    userInputEditText.getBackground().clearColorFilter();

                    color = ContextCompat.getColor(VoiceRecorder.this.getContext(), R.color.colorAccent);
                    userInputPromptTextView.setTextColor(color);
                } else {
                    color = ContextCompat.getColor(VoiceRecorder.this.getContext(), android.R.color.primary_text_dark);
                    userInputPromptTextView.setTextColor(color);
                }
            }
        });

        //this makes it so that when the hint is no longer visible (the user has typed something),
        // the label above the text field will show the hint
        userInputEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                if (charSequence.length() == 0){
                    //the hint will be shown so hide the label above the line
                    userInputPromptTextView.setText("");
                } else {
                    if (userInputPromptTextView.getText().length() == 0){
                        CharSequence hint = userInputEditText.getHint();
                        userInputPromptTextView.setText(hint);
                    }
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });
    }

    //both sets of recorder listeners should check for the recording permission
    //and ask the user to allow permissions

    //use when google voice recognition when available
    //because it comes with better detection capabilities
    // (auto-stop, silence detection, etc)
    private View.OnClickListener googleVoiceRecorder(){
        return new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!recordPermissionGranted){
                    askForRecordingPermission();
                    return;
                }
                //we can either use an intent to make a new activity
                // or a service that runs on a thread.
                //there is no way? of getting the resulting audio from
                //the threaded version so we are using an intent with an activity.
                //this prevents control of this fragment while the api activity is up
                //so an orientation change will not save any com.linnca.whispers.data in the background activity
                Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
                // secret parameters that when added provide audio url in the result
                intent.putExtra("android.speech.extra.GET_AUDIO_FORMAT", "audio/AMR");
                intent.putExtra("android.speech.extra.GET_AUDIO", true);

                //we lock the screen orientation here because when the user rotates the screen
                //during a recording, nothing will be saved in the background
                lockScreenOrientation();
                startActivityForResult(intent, REQUEST_VOICE_RECOGNITION);
            }
        };
    }

    //if the user has something like a amazon fire which doesn't have google play,
    // use this instead.
    //TODO: silence detection for native recorder
    private View.OnClickListener nativeVoiceRecorder(){
        return new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!recordPermissionGranted){
                    askForRecordingPermission();
                    return;
                }
                onRecord(recording);

            }
        };
    }

    //the first time the user enters this page,
    //we should ask for permissions normally (with a popup).
    //if the user denies the permission initially and clicks on thee record button,
    //we should let him go to the app settings to change permissions
    private void askForRecordingPermission(){
        Snackbar snackbar = Snackbar.make(layout, R.string.voice_recorder_permission_required_title,
                Snackbar.LENGTH_INDEFINITE);
        snackbar.setAction(R.string.voice_recorder_permission_required_button,
                new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                Uri.fromParts("package", getContext().getPackageName(), null));
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                    }
                });
        snackbar.show();
    }

    private void lockScreenOrientation(){
        int currentOrientation = getResources().getConfiguration().orientation;
        if (currentOrientation == Configuration.ORIENTATION_LANDSCAPE) {
            getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
        }
        else {
            getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT);
        }
    }

    private void unlockScreenOrientation(){
        getActivity().setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_USER);
    }

    private void setUserInputPrompt(){
        if (!showUserInputPrompt){
            userInputEditText.setVisibility(View.GONE);
        }
        userInputPromptTextView.setText(userInputPrompt);
    }
    
    private void configureUserInputPrompt(int config){
        if (config == USER_INPUT_REQUIRED || config == USER_INPUT_OPTIONAL)
            showUserInputPrompt = true;
        if (config == USER_INPUT_GONE)
            showUserInputPrompt = false;
        if (config == USER_INPUT_OPTIONAL)
            userInputPromptOptional =true;
        if (config == USER_INPUT_REQUIRED)
            userInputPromptOptional = false;
    }

}
