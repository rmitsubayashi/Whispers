package mugenglish.whispers.gui;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.joda.time.DateTime;

import data.ChainManager;
import data.FirebaseDBHeaders;
import data.UserManager;
import data.datawrappers.ChainLink;
import data.datawrappers.ChainQueue;
import mugenglish.whispers.R;
import mugenglish.whispers.gui.widgets.ActionAfterEndListener;

public class Teach_End extends Fragment implements ActionAfterEndListener {
    public static String BUNDLE_CHAIN_ID = "chainID";
    public static String BUNDLE_CHAIN_QUEUE = "chainQueue";
    public static String BUNDLE_LANGUAGE_CODE = "languageCode";
    private String chainID;
    //if the user exists without linking hte chain,
    //put this back into the queue
    private ChainQueue chainQueue = null;
    private String languageCode;
    private boolean cleanlyFinished = false;

    private FirebaseDatabase database;

    //for deciding the right layout to show
    private boolean firstLink;
    private boolean finalLink;

    private Teach_EndListener listener;

    public interface Teach_EndListener{
        void teachEndToTeachStart();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle bundle = getArguments();
        chainID = bundle.getString(BUNDLE_CHAIN_ID);

        //now this activity is responsible for putting the chain back into the queue
        // if the user never finishes.
        //this can be empty
        if(bundle.containsKey(BUNDLE_CHAIN_QUEUE)){
            chainQueue = (ChainQueue)bundle.getSerializable(BUNDLE_CHAIN_QUEUE);
            languageCode = bundle.getString(BUNDLE_LANGUAGE_CODE);
        }

        database = FirebaseDatabase.getInstance();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState){
        return inflater.inflate(R.layout.fragment_teach_end, container, false);
    }

    @Override
    public void onStart(){
        super.onStart();
        setLayoutBasedOnChain();
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
            listener = (Teach_EndListener) context;
        } catch (Exception e){
            throw new ClassCastException(context.toString() + " must implement listener");
        }
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        //this means the user created a chain but never
        //put anything into the chain queue
        if (chainQueue == null && !cleanlyFinished){
            ChainManager.removeChain(chainID);
            return;
        }

        if (chainQueue == null){ //&& cleanlyFinished)
            return;
        }
        //put the chain back into the queue if
        // the user exits the application without submitting
        String currentDateTime = DateTime.now().toString();
        chainQueue.setDateTimeAdded(currentDateTime);
        ChainManager.enqueue(chainQueue, languageCode,"",true);
    }

    //stores the chain info and rewards user
    @Override
    public void saveData(final String recordingPath, final String answer){
        DatabaseReference linkNumberRef = database.getReference(
                FirebaseDBHeaders.CHAINS + "/" +
                chainID + "/" +
                FirebaseDBHeaders.CHAINS_ID_NEXT_LINK_NUMBER
        );
        linkNumberRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                long linkNumber = (long)dataSnapshot.getValue();
                String userID = FirebaseAuth.getInstance().getCurrentUser().getUid();
                ChainLink chainLink = new ChainLink(userID, linkNumber, recordingPath, answer);
                ChainManager.updateChain(chainID, chainLink);
                chainQueue = null;
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
        rewardUser();
    }

    private void rewardUser(){
        String userID = FirebaseAuth.getInstance().getCurrentUser().getUid();
        UserManager.addCredit(userID, UserManager.CREDIT_REWARD_FOR_TEACHING);
    }

    @Override
    public void redirectUser(){
        listener.teachEndToTeachStart();
        chainQueue = null;
        cleanlyFinished = true;
    }

    private void setLayoutBasedOnChain(){
        DatabaseReference chainRef = database.getReference(
                FirebaseDBHeaders.CHAINS + "/" +
                chainID + "/" +
                FirebaseDBHeaders.CHAINS_ID_NEXT_LINK_NUMBER
        );
        chainRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                long nextLinkNumber = (long)dataSnapshot.getValue();
                if (nextLinkNumber == 0L) {
                    firstLink = true;
                    finalLink = false;
                }
                else if (nextLinkNumber == 2L){
                    firstLink = false;
                    finalLink = false;
                } else if (nextLinkNumber == 4L){
                    firstLink = false;
                    finalLink = true;
                }
                addFragmentUI();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    private void addFragmentUI(){
        if (finalLink)
            addFinalAnswerUI();
        else
            addVoiceRecorderUI();
    }

    private void addVoiceRecorderUI(){
        FragmentManager fragmentManager = getChildFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        Fragment voiceRecorder = new VoiceRecorder();
        Bundle bundle = new Bundle();
        int userInputPrompt;
        if (firstLink)
            userInputPrompt = VoiceRecorder.USER_INPUT_PROMPT_NONE;
        else
            userInputPrompt = VoiceRecorder.USER_INPUT_PROMPT_REQUIRED;

        bundle.putInt(VoiceRecorder.USER_INPUT_PROMPT_BUNDLE, userInputPrompt);
        voiceRecorder.setArguments(bundle);
        fragmentTransaction.add(R.id.fragment_teach_end, voiceRecorder);
        fragmentTransaction.commit();
    }

    private void addFinalAnswerUI(){
        FragmentManager fragmentManager = getChildFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        Fragment finalAnswer = new FinalAnswer();
        fragmentTransaction.add(R.id.fragment_teach_end, finalAnswer);
        fragmentTransaction.commit();
    }
}
