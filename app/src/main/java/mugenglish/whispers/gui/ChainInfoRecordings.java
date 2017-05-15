package mugenglish.whispers.gui;

import android.media.MediaPlayer;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import data.ChainManager;
import data.FirebaseDBHeaders;
import data.datawrappers.ChainLink;
import data.datawrappers.MinimalChain;
import mugenglish.whispers.R;
import mugenglish.whispers.gui.widgets.AudioSettings;

public class ChainInfoRecordings extends Fragment {
    public static String CHAIN_INFO_RECORDINGS_MINIMAL_CHAIN;
    private MinimalChain minimalChain;
    private long myLinkNumber;
    private FirebaseDatabase database;
    private FirebaseStorage storage;
    private DatabaseReference chainInfoRef;
    private ValueEventListener listener;
    private class ChainLinkUI {
        private ImageButton imageButton;
        private TextView textView;

        ChainLinkUI(ImageButton imageButton, TextView textView){
            this.imageButton = imageButton;
            this.textView = textView;
        }
    }
    private List<ChainLinkUI> chainLinkUIs = new ArrayList<>(5);
    private TextView link1TextView;
    private TextView link5TextView;

    private MediaPlayer mediaPlayer = new MediaPlayer();

    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        database = FirebaseDatabase.getInstance();
        storage = FirebaseStorage.getInstance();
        Bundle bundle = getArguments();
        minimalChain = (MinimalChain)bundle.getSerializable(CHAIN_INFO_RECORDINGS_MINIMAL_CHAIN);
        if (minimalChain == null)
            return;
        myLinkNumber = minimalChain.getLinkedLinkNumber();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState){
        View view = inflater.inflate(R.layout.fragment_chain_info_recordings ,container, false);
        ImageButton link1ImageButton = (ImageButton)view.findViewById(R.id.chain_info_recordings_link1_image);
        link1TextView = (TextView)view.findViewById(R.id.chain_info_recordings_link1_text);
        chainLinkUIs.add(new ChainLinkUI(link1ImageButton, link1TextView));
        ImageButton link2ImageButton = (ImageButton)view.findViewById(R.id.chain_info_recordings_link2_image);
        TextView link2TextView = (TextView)view.findViewById(R.id.chain_info_recordings_link2_text);
        chainLinkUIs.add(new ChainLinkUI(link2ImageButton, link2TextView));
        ImageButton link3ImageButton = (ImageButton)view.findViewById(R.id.chain_info_recordings_link3_image);
        TextView link3TextView = (TextView)view.findViewById(R.id.chain_info_recordings_link3_text);
        chainLinkUIs.add(new ChainLinkUI(link3ImageButton, link3TextView));
        ImageButton link4ImageButton = (ImageButton)view.findViewById(R.id.chain_info_recordings_link4_image);
        TextView link4TextView = (TextView)view.findViewById(R.id.chain_info_recordings_link4_text);
        chainLinkUIs.add(new ChainLinkUI(link4ImageButton, link4TextView));
        ImageButton link5ImageButton = (ImageButton)view.findViewById(R.id.chain_info_recordings_link5_image);
        link5TextView = (TextView)view.findViewById(R.id.chain_info_recordings_link5_text);
        chainLinkUIs.add(new ChainLinkUI(link5ImageButton, link5TextView));

        populateData();

        return view;
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

                //if not completed, change ui so the first recording is covered up
                if (linkSnapshot.getChildrenCount() != 5){
                    link1TextView.setText("???");
                } else {
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
                    //this also changes layout based on result
                    checkAnswer(phraseID, situationID, languageCode, answer);
                }
                //no need to reset the layout because
                // a chain can never go back
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        };
        chainInfoRef.addValueEventListener(listener);
    }

    private void checkAnswer(String phraseID, String situationID, String languageCode, final String answer){
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
                String answerToCheck = dataSnapshot.getValue(String.class);
                link1TextView.setText(answerToCheck);
                if (ChainManager.checkAnswers(answerToCheck, answer)){
                    //green
                    link5TextView.setTextColor(
                            ContextCompat.getColor(
                                    ChainInfoRecordings.this.getContext(),R.color.colorAccent
                            )
                    );
                } else {
                    link5TextView.setTextColor(
                            ContextCompat.getColor(
                                    ChainInfoRecordings.this.getContext(),R.color.errorRed
                            )
                    );
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    private void updateLinks(List<ChainLink> links){
        StorageReference audioRef = storage.getReference(
                FirebaseDBHeaders.STORAGE_RECORDINGS
        );
        Log.d(getClass().getSimpleName(), "list size:"+chainLinkUIs.size());
        for (int i=0; i<links.size(); i++){
            ChainLink link = links.get(i);
            String answer = link.getAnswer();
            TextView linkText = chainLinkUIs.get(i).textView;
            linkText.setText(answer);

            final ImageButton linkButton = chainLinkUIs.get(i).imageButton;
            if (i == myLinkNumber){
                linkButton.setImageResource(R.drawable.ic_play);
                linkButton.setBackgroundResource(R.drawable.round_button);
            } else {
                linkButton.setImageResource(R.drawable.ic_play);
                linkButton.setBackgroundResource(R.drawable.round_button);
            }

            //download associated audio file
            String audioFileName = link.getAudioFileName();
            //there does not need to be an audio file (last link)
            if (audioFileName == null)
                continue;

            try {
                File tempFile = File.createTempFile(audioFileName, AudioSettings.EXTENSION);
                final String tempFilePath = tempFile.getAbsolutePath();
                audioRef.child(audioFileName).getFile(tempFile).addOnSuccessListener(
                        new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
                            @Override
                            public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot) {
                                linkButton.setOnClickListener(linkListener(tempFilePath));
                            }
                        }
                );
            } catch (IOException ioe){
                ioe.printStackTrace();
            }
        }
    }

    private View.OnClickListener linkListener(final String audioFileName){
        return new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mediaPlayer = new MediaPlayer();
                mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                    @Override
                    public void onCompletion(MediaPlayer mediaPlayer) {
                        stopPlaying();
                    }
                });
                try {
                    mediaPlayer.setDataSource(audioFileName);
                    mediaPlayer.prepare();
                    mediaPlayer.start();
                } catch (IOException ioe){
                    ioe.printStackTrace();
                }
            }
        };
    }

    private void stopPlaying(){
        mediaPlayer.release();
        mediaPlayer = null;
    }

    @Override
    public void onDestroy(){
        if (chainInfoRef != null && listener != null){
            chainInfoRef.removeEventListener(listener);
        }

        super.onDestroy();
    }
}
