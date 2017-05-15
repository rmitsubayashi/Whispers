package mugenglish.whispers.gui;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Transaction;

import org.joda.time.DateTime;

import data.ChainManager;
import data.FirebaseDBHeaders;
import data.UserManager;
import data.datawrappers.ChainQueue;
import mugenglish.whispers.R;
import mugenglish.whispers.gui.widgets.ActionAfterStartListener;
import mugenglish.whispers.gui.widgets.GUIUtils;

public class Learn_Start extends Fragment implements ActionAfterStartListener {
    public static String BUNDLE_SITUATION_ID = "situationID";
    public static String BUNDLE_LANGUAGE_CODE = "languageCode";
    private FirebaseDatabase database;
    private String situationID;
    private String languageCode;
    private ChainQueue chainQueue = null;

    private String userID;

    private Learn_StartListener listener;

    interface Learn_StartListener {
        void learnStartToLearnEnd(ChainQueue chainQueue, String situationID);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle bundle = getArguments();
        database = FirebaseDatabase.getInstance();
        situationID = bundle.getString(BUNDLE_SITUATION_ID);
        languageCode = bundle.getString(BUNDLE_LANGUAGE_CODE);
        userID = FirebaseAuth.getInstance().getCurrentUser().getUid();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState){
        return inflater.inflate(R.layout.fragment_learn_start, container, false);
    }

    @Override
    public void onStart(){
        super.onStart();
        getChainFromQueue();
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
            listener = (Learn_StartListener) context;
        } catch (Exception e){
            throw new ClassCastException(context.toString() + " must implement listener");
        }
    }

    private void getChainFromQueue(){
        DatabaseReference chainQueueRef = database.getReference(
                FirebaseDBHeaders.TO_LEARN_CHAIN_QUEUE + "/" +
                languageCode + "/" +
                situationID
        );

        Log.d(getClass().getSimpleName(), FirebaseDBHeaders.TO_LEARN_CHAIN_QUEUE + "/" +
                languageCode + "/" +
                situationID);

        /*chainQueueRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                Log.d(getClass().getSimpleName(), "Data:" + dataSnapshot.toString());
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });*/

        chainQueueRef.runTransaction(new Transaction.Handler() {
            ChainQueue chain = null;
            @Override
            public Transaction.Result doTransaction(MutableData mutableData) {
                if (mutableData.getValue() == null)
                    return Transaction.success(mutableData);
                MutableData mostRecentChainRef = null;
                ChainQueue mostRecentChain = null;
                DateTime mostRecentDateTime = null;
                for (MutableData childRef : mutableData.getChildren()){
                    ChainQueue childClass = childRef.getValue(ChainQueue.class);
                    if (childClass == null) {
                        continue;
                    }
                    DateTime childDateTime = DateTime.parse(childClass.getDateTimeAdded());
                    if (mostRecentChainRef == null){
                        mostRecentChainRef = childRef;
                        mostRecentChain = childClass;
                        mostRecentDateTime = childDateTime;
                    }

                    if (childDateTime.isAfter(mostRecentDateTime)){
                        mostRecentChainRef = childRef;
                        mostRecentChain = childClass;
                        mostRecentDateTime = childDateTime;
                    }
                }

                if (mostRecentChainRef != null) {
                    chain = mostRecentChain;
                    //remove
                    mostRecentChainRef.setValue(null);
                }

                return Transaction.success(mutableData);
            }

            @Override
            public void onComplete(DatabaseError databaseError, boolean b, DataSnapshot dataSnapshot) {
                if (chain == null){
                    notifyNoChain();
                } else {
                    //do this here makes sense?
                    //but we will need to account for whn the user doesn't complete the chain.
                    //we should return the credits then
                    UserManager.removeCredit(userID, UserManager.CREDIT_NEEDED_FOR_LEARNING);
                    linkChain(chain);
                }
                Log.d("Learn_Start","completed transaction");
            }
        });
    }

    private void notifyNoChain(){
        if (getView() == null)
            return;

        TextView errorTextView = new TextView(getContext());
        errorTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        errorTextView.setText(R.string.learn_start_no_chain);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT);
        params.gravity = Gravity.CENTER;
        int margin = GUIUtils.getDp(16,getContext());
        errorTextView.setLayoutParams(params);

        errorTextView.setPadding(margin,margin,margin,margin);

        ViewGroup rootView = (ViewGroup)getView();
        rootView.addView(
                errorTextView
        );
    }

    private void linkChain(ChainQueue chainQueue){
        this.chainQueue = chainQueue;
        String audioPath = chainQueue.getAudioPath();
        addVoicePlayerFragment(audioPath);
    }

    private void addVoicePlayerFragment(String audioPath){
        FragmentManager fragmentManager = getChildFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        Fragment voicePlayer = new VoicePlayer();
        Bundle bundle = new Bundle();
        bundle.putString(VoicePlayer.BUNDLE_AUDIO_FILE_NAME_KEY, audioPath);
        voicePlayer.setArguments(bundle);
        fragmentTransaction.add(R.id.fragment_learn_start, voicePlayer);
        fragmentTransaction.commit();
    }

    @Override
    public void continueToEnd(){
        //we want to pass the chain id to the next page
        listener.learnStartToLearnEnd(chainQueue, situationID);
        chainQueue = null;
    }

    @Override
    public void onDestroy(){
        //put the chain back into the queue
        // if the user exits without going to the next activity
        putChainBackToQueue();
        super.onDestroy();
    }

    private void putChainBackToQueue(){
        //cleanly finished activity
        if (chainQueue == null)
            return;

        Log.d(getClass().getSimpleName(), "Called enqueue");
        ChainManager.enqueue(chainQueue, languageCode, situationID, false);
        //also give back the credits
        UserManager.addCredit(userID,UserManager.CREDIT_NEEDED_FOR_LEARNING);
    }
}
