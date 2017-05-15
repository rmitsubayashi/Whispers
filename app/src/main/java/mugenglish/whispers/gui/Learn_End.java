package mugenglish.whispers.gui;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import data.ChainManager;
import data.FirebaseDBHeaders;
import data.UserManager;
import data.datawrappers.ChainLink;
import data.datawrappers.ChainQueue;
import mugenglish.whispers.R;
import mugenglish.whispers.gui.widgets.ActionAfterEndListener;

public class Learn_End extends Fragment implements ActionAfterEndListener {
    public static String BUNDLE_CHAIN_QUEUE = "chainQueue";
    public static String BUNDLE_LANGUAGE_CODE = "languageCode";
    public static String BUNDLE_SITUATION_ID = "situationID";

    private String chainID;
    private ChainQueue chainQueue;
    private FirebaseDatabase database;
    private String userID;
    //we need these to return the chain queue back into the queue
    private String languageCode;
    private String situationID;
    private boolean cleanlyFinished = false;

    private Learn_EndListener listener;

    public interface Learn_EndListener {
        void learnEndToSituationList();
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle bundle = getArguments();
        chainQueue = (ChainQueue)bundle.getSerializable(BUNDLE_CHAIN_QUEUE);
        chainID = chainQueue.getChainID();
        //note we can also get this by looking at the user's preferences
        languageCode = bundle.getString(BUNDLE_LANGUAGE_CODE);
        situationID = bundle.getString(BUNDLE_SITUATION_ID);

        database = FirebaseDatabase.getInstance();
        userID = FirebaseAuth.getInstance().getCurrentUser().getUid();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState){
        return inflater.inflate(R.layout.fragment_learn_end, container, false);
    }

    @Override
    public void onStart(){
        super.onStart();
        addVoiceRecorderFragment();
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
            listener = (Learn_EndListener) context;
        } catch (Exception e){
            throw new ClassCastException(context.toString() + " must implement listener");
        }
    }

    @Override
    public void saveData(final String audioFileName, final String answer){
        DatabaseReference linkNumberRef = database.getReference(
                FirebaseDBHeaders.CHAINS + "/" +
                        chainID + "/" +
                        FirebaseDBHeaders.CHAINS_ID_NEXT_LINK_NUMBER
        );
        linkNumberRef.addListenerForSingleValueEvent(new ValueEventListener() {
             @Override
             public void onDataChange(DataSnapshot dataSnapshot) {
                 long nextLinkNumber = (long)dataSnapshot.getValue();
                 String userID = FirebaseAuth.getInstance().getCurrentUser().getUid();
                 ChainLink chainLink = new ChainLink(userID, nextLinkNumber, audioFileName, answer);
                 ChainManager.updateChain(chainID, chainLink);
                 chainQueue = null;
             }

             @Override
             public void onCancelled(DatabaseError databaseError) {

             }
         });

    }

    @Override
    public void redirectUser(){
        cleanlyFinished = true;
        listener.learnEndToSituationList();
    }

    @Override
    public void onDestroy(){
        //put the chain back into the queue
        // if the user exits without going to the next activity
        putChainBackToQueue();
        super.onDestroy();
    }

    private void putChainBackToQueue(){
        if (!cleanlyFinished) {
            //put the chain back into queue
            ChainManager.enqueue(chainQueue, languageCode, situationID, false);
            //give back the credits
            UserManager.addCredit(userID, UserManager.CREDIT_NEEDED_FOR_LEARNING);
        }
    }

    private void addVoiceRecorderFragment(){
        FragmentManager fragmentManager = getChildFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        Fragment voiceRecorder = new VoiceRecorder();
        Bundle bundle = new Bundle();
        bundle.putInt(VoiceRecorder.USER_INPUT_PROMPT_BUNDLE, VoiceRecorder.USER_INPUT_PROMPT_OPTIONAL);
        voiceRecorder.setArguments(bundle);
        fragmentTransaction.add(R.id.fragment_learn_end, voiceRecorder);
        fragmentTransaction.commit();
    }
}
