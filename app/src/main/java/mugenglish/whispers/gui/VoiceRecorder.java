package mugenglish.whispers.gui;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.PorterDuff;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.io.File;

import data.FirebaseDBHeaders;
import mugenglish.whispers.R;
import mugenglish.whispers.gui.widgets.ActionAfterEndListener;
import mugenglish.whispers.gui.widgets.AudioSettings;

public class VoiceRecorder extends Fragment {
    public static String USER_INPUT_PROMPT_BUNDLE = "user input prompt";
    public static int USER_INPUT_PROMPT_NONE = 0;
    public static int USER_INPUT_PROMPT_REQUIRED = 1;
    public static int USER_INPUT_PROMPT_OPTIONAL = 2;

    private ImageButton recordButton;
    private ImageButton playButton;
    private MediaRecorder recorder;
    private boolean recording = false;
    private MediaPlayer player;
    private boolean playing = false;
    private String currentFileName = null;
    private String filePath;

    private ActionAfterEndListener actionAfterEndListener;
    private Button confirmButton;
    private boolean showUserInputPrompt;
    private boolean userInputPromptOptional;
    private TextView userInputPrompt;
    private EditText userInputEditText;
    private TextView recordingErrorMessage;
    private FirebaseStorage storage;
    private String userID;

    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 200;
    private boolean recordPermissionGranted = false;

    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        Activity activity = getActivity();
        //we need to get permission from the user to use the microphone
        int permissionCheck = ActivityCompat.checkSelfPermission(activity,
                Manifest.permission.RECORD_AUDIO);
        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
            String[] permissions = new String[]{Manifest.permission.RECORD_AUDIO};
            ActivityCompat.requestPermissions(activity, permissions, REQUEST_RECORD_AUDIO_PERMISSION);
        }
        if (activity.getExternalCacheDir() != null) {
            filePath = activity.getExternalCacheDir().getAbsolutePath();
        } else {
            Log.d(getClass().getCanonicalName(),"External cache returned null");
        }

        if (FirebaseAuth.getInstance().getCurrentUser() == null){
            FirebaseAuth.getInstance().signInAnonymously();
        }

        userID = FirebaseAuth.getInstance().getCurrentUser().getUid();
        storage = FirebaseStorage.getInstance();

        int userPromptBundle = getArguments().getInt(USER_INPUT_PROMPT_BUNDLE);
        if (userPromptBundle == USER_INPUT_PROMPT_NONE) {
            showUserInputPrompt = false;
        }
        else if (userPromptBundle == USER_INPUT_PROMPT_OPTIONAL){
            showUserInputPrompt = true;
            userInputPromptOptional = true;
        } else if (userPromptBundle == USER_INPUT_PROMPT_REQUIRED){
            showUserInputPrompt = true;
            userInputPromptOptional = false;
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_voice_recorder, container, false);
        recordButton = (ImageButton) view.findViewById(R.id.voice_recorder_record_button);
        playButton = (ImageButton) view.findViewById(R.id.voice_recorder_play_button);
        confirmButton = (Button) view.findViewById(R.id.voice_recorder_confirm_button);
        userInputEditText = (EditText) view.findViewById(R.id.voice_recorder_user_input_edit_text);
        userInputPrompt = (TextView) view.findViewById(R.id.voice_recorder_user_input_prompt);
        recordingErrorMessage = (TextView) view.findViewById(R.id.voice_recorder_recording_error_message);

        setUserInputPrompt();

        addListeners();

        return view;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode){
            case REQUEST_RECORD_AUDIO_PERMISSION:
                recordPermissionGranted  = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                break;
        }
        if (!recordPermissionGranted ) getActivity().finish();

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

    private void removeFile(){
        if (currentFileName == null)
            return;
        File file = new File(currentFileName);
        if (!file.delete()){
            Log.d(getClass().getCanonicalName(), "Could not remove file: " + currentFileName);
        }
        currentFileName = null;
    }

    private void refreshCurrentFileName(){
        DateTime dateTime = DateTime.now();
        DateTimeFormatter dtf = DateTimeFormat.forPattern("MMddyyyy_HHmmss");
        String dateTimeIdentifier = dtf.print(dateTime);
        currentFileName = filePath + "/" + userID + dateTimeIdentifier + AudioSettings.EXTENSION;
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

    private void onRecord(boolean recording){
        if (!recording) {
            Log.d(getClass().getCanonicalName(), "Starting recorder");
            startRecording();
        } else {
            Log.d(getClass().getCanonicalName(), "Stopping recorder");
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
            player.setDataSource(currentFileName);
            player.prepare();
            player.start();

            playButton.setEnabled(false);
            playing = true;
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    private void stopPlaying(){
        player.release();
        player = null;
        playButton.setEnabled(true);
        playing = false;
    }

    private void startRecording(){
        removeFile();
        refreshCurrentFileName();
        recorder = new MediaRecorder();
        recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        recorder.setOutputFormat(AudioSettings.OUTPUT_FORMAT);
        recorder.setOutputFile(currentFileName);
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
        recorder.stop();
        recorder.release();
        recorder = null;
        recordButton.setImageResource(R.drawable.ic_record);
        recording = false;
    }

    private String getUploadPath(){
        String fileName = currentFileName.replace(filePath,"");
        return FirebaseDBHeaders.STORAGE_RECORDINGS + fileName;
    }

    private String getUploadFileName(){
        return currentFileName.replace(filePath,"");
    }

    private void uploadRecording(){
        StorageReference ref = storage.getReference(getUploadPath());
        final File recordingFile = new File(currentFileName);
        Uri recordingUri = Uri.fromFile(recordingFile);
        UploadTask task = ref.putFile(recordingUri);
        task.addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                removeFile();
            }
        });
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
        String audioFileName = getUploadFileName();
        uploadRecording();

        actionAfterEndListener.saveData(audioFileName, answer);

        actionAfterEndListener.redirectUser();
    }

    private void addListeners(){
        recordButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onRecord(recording);

            }
        });

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
                    userInputPrompt.setTextColor(color);
                } else {
                    color = ContextCompat.getColor(VoiceRecorder.this.getContext(), R.color.gray);
                    userInputPrompt.setTextColor(color);
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
                    userInputPrompt.setText("");
                } else {
                    if (userInputPrompt.getText().length() == 0){
                        CharSequence hint = userInputEditText.getHint();
                        userInputPrompt.setText(hint);
                    }
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });
    }

    private void setUserInputPrompt(){
        if (!showUserInputPrompt){
            userInputEditText.setVisibility(View.GONE);
        } else {
            if (userInputPromptOptional){
                userInputEditText.setHint(R.string.voice_recorder_prompt_optional);
            } else {
                userInputEditText.setHint(R.string.voice_recorder_prompt_required);
            }
        }
    }

}
