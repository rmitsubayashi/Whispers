package mugenglish.whispers.gui;

import android.media.MediaPlayer;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.File;

import data.FirebaseDBHeaders;
import mugenglish.whispers.R;
import mugenglish.whispers.gui.widgets.ActionAfterStartListener;
import mugenglish.whispers.gui.widgets.AudioSettings;

public class VoicePlayer extends Fragment {
    public static String BUNDLE_AUDIO_FILE_NAME_KEY = "imageName";
    private FirebaseStorage storage;
    private MediaPlayer player;
    private boolean playing;
    private ImageButton playButton;
    private Button confirmButton;
    private String filePath;
    private ActionAfterStartListener actionAfterStartListener;

    public VoicePlayer(){

    }

    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);

        storage = FirebaseStorage.getInstance();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState){
        View view = inflater.inflate(R.layout.fragment_voice_player, container, false);
        playButton = (ImageButton) view.findViewById(R.id.voice_player_play_button);
        confirmButton = (Button) view.findViewById(R.id.voice_player_confirm_button);

        Bundle dataBundle = getArguments();
        String imageName = dataBundle.getString(BUNDLE_AUDIO_FILE_NAME_KEY);
        readFile(imageName);
        addListeners();
        implementListeners(getParentFragment());

        return view;
    }

    private void implementListeners(Fragment parentFragment){
        try {
            actionAfterStartListener = (ActionAfterStartListener) parentFragment;
        } catch (Exception e){
            throw new ClassCastException(parentFragment.toString() + " must implement actionAfterStart listener");
        }
    }

    private void readFile(String audioFileName){
        StorageReference audioRef = storage.getReference(
                FirebaseDBHeaders.STORAGE_RECORDINGS + "/" +
                audioFileName
        );
        try {
            File tempAudioFile = File.createTempFile("whispersAudioFile", AudioSettings.EXTENSION);
            filePath = tempAudioFile.getAbsolutePath();
            audioRef.getFile(tempAudioFile).addOnSuccessListener(new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot) {
                    unlockPlayButton();
                }
            });
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    private void unlockPlayButton(){
        playButton.setEnabled(true);
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
        player.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mediaPlayer) {
                stopPlaying();
            }
        });
        try {
            player.setDataSource(filePath);
            player.prepare();
            player.setVolume(1,1);
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
