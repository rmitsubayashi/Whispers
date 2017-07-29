package com.linnca.whispers.gui;

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
import android.widget.ProgressBar;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Query;
import com.google.firebase.database.Transaction;
import com.google.firebase.database.ValueEventListener;

import com.linnca.whispers.data.ChainManager;
import com.linnca.whispers.data.FirebaseDBHeaders;
import com.linnca.whispers.data.RecordingManager;
import com.linnca.whispers.data.datawrappers.ChainQueue;
import com.linnca.whispers.R;
import com.linnca.whispers.gui.widgets.ActionAfterStartListener;

public class Learn_Start extends Fragment implements ActionAfterStartListener,
        TimeExpired.TimeExpiredListener,
        YouAreOffline.YouAreOfflineListener,
        Learn_StartNoChain.LearnStartNoChainListener
{
    private final String TAG = "Learn_Start";
    public static String BUNDLE_SITUATION_ID = "situationID";
    public static String BUNDLE_TO_LEARN_LANGUAGE_CODE = "toLearnLanguageCode";
    public static String BUNDLE_TO_DISPLAY_LANGUAGE_CODE = "toDisplayLanguageCode";

    private final String SAVED_STATE_SITUATION_ID = "savedSituationID";
    private final String SAVED_STATE_TO_LEARN_LANGUAGE_CODE = "savedToLearnLanguageCode";
    private final String SAVED_STATE_TO_DISPLAY_LANGUAGE_CODE = "savedToDisplayLeanguageCode";
    private final String SAVED_STATE_CHAIN_QUEUE_KEY = "savedChainQueueKey";
    private final String SAVED_STATE_CHAIN_ID = "savedChainID";
    private final String SAVED_STATE_FILE_NAME = "savedAudioPath";
    //we have a local variable for when the activity is stopped (stopped variable is persisted),
    //and we have the saved instance variable for when the user changes orientation
    //outside of the app and come back (stopped state will not be saved)
    public static final String SAVED_STATE_STOPPED = "stopped";

    private final String FRAGMENT_VOICE_PLAYER = "voicePlayer";
    private final String FRAGMENT_TIME_EXPIRED = "timeExpired";
    private final String FRAGMENT_NO_CHAIN = "noChain";
    private final String FRAGMENT_OFFLINE = "offline";

    private FirebaseDatabase database;
    private String situationID;
    private String toLearnLanguageCode;
    private String toDisplayLanguageCode;
    private String chainQueueKey;
    private String chainID;
    private String fileName;
    private boolean stopped = false;
    private DatabaseReference onlineRef;
    private ValueEventListener onlineListener;

    private ProgressBar progressBar;

    private String userID;

    private Learn_StartListener listener;

    interface Learn_StartListener {
        void learnStartToLearnEnd(String chainQueueKey, String situationID, String chainID);
        void learnStartToSituationList();
        //different learn start
        //for when there is no chain is available and
        //we want to redirect the user to another situation
        void learnStartToLearnStart(String situationID);
        void toOfflineMode();
        void setSituationID(String situationID);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        database = FirebaseDatabase.getInstance();
        userID = FirebaseAuth.getInstance().getCurrentUser().getUid();
        //restored fragment
        if (savedInstanceState != null){
            stopped = savedInstanceState.getBoolean(SAVED_STATE_STOPPED);
            //we have to check this to prevent referencing an already-released chain
            // in putChainBackIntoQueue
            situationID = savedInstanceState.getString(SAVED_STATE_SITUATION_ID);
            toLearnLanguageCode = savedInstanceState.getString(SAVED_STATE_TO_LEARN_LANGUAGE_CODE);
            toDisplayLanguageCode = savedInstanceState.getString(SAVED_STATE_TO_DISPLAY_LANGUAGE_CODE);
            chainID = savedInstanceState.getString(SAVED_STATE_CHAIN_ID);
            chainQueueKey = savedInstanceState.getString(SAVED_STATE_CHAIN_QUEUE_KEY);
            fileName = savedInstanceState.getString(SAVED_STATE_FILE_NAME);

        } else {//first instantiation
            Bundle bundle = getArguments();
            if (bundle.getString(BUNDLE_SITUATION_ID) != null) {
                situationID = bundle.getString(BUNDLE_SITUATION_ID);
                toLearnLanguageCode = bundle.getString(BUNDLE_TO_LEARN_LANGUAGE_CODE);
                toDisplayLanguageCode =bundle.getString(BUNDLE_TO_DISPLAY_LANGUAGE_CODE);
            }
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState){
        View view = inflater.inflate(R.layout.fragment_wrapper, container, false);
        progressBar = (ProgressBar)view.findViewById(R.id.fragment_progress_bar);
        return view;
    }

    @Override
    public void onStart(){
        super.onStart();
        getActivity().setTitle(R.string.toolbar_title_learn);
        FragmentManager fragmentManager = getChildFragmentManager();
        if (stopped && fragmentManager.findFragmentByTag(FRAGMENT_VOICE_PLAYER) != null){
            addTimeExpiredFragment();
        }
        else {
            onlineRef = database.getReference(".info/connected");
            onlineListener = new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    boolean connected = dataSnapshot.getValue(Boolean.class);
                    if (connected){
                        FragmentManager fragmentManager = getChildFragmentManager();
                        if (fragmentManager.findFragmentByTag(FRAGMENT_VOICE_PLAYER) == null &&
                                fragmentManager.findFragmentByTag(FRAGMENT_NO_CHAIN) == null &&
                                fragmentManager.findFragmentByTag(FRAGMENT_TIME_EXPIRED) == null &&
                                fragmentManager.findFragmentByTag(FRAGMENT_OFFLINE) == null
                                ) {
                            getChainFromQueue();
                        }
                    } else {
                        //!stopped prevents attempting to add the fragment when we press the
                        //home button
                        if (!stopped)
                            addNoConnectionFragment();
                        onlineRef.removeEventListener(onlineListener);
                    }
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            };
            onlineRef.addValueEventListener(onlineListener);
        }
        //no need to create the child fragment here
        //because the child fragment manager handles the destruction of
        //the child fragment and restores it here
    }

    @Override
    public void onSaveInstanceState(Bundle outState){
        //if there is an orientation change,
        //use the same com.linnca.whispers.data when the fragment is re-created
        if (!getActivity().isChangingConfigurations() || stopped) {
            outState.putBoolean(SAVED_STATE_STOPPED, true);
        } else {
            //needed to continue to learn_end
            outState.putString(SAVED_STATE_SITUATION_ID, situationID);
            outState.putString(SAVED_STATE_CHAIN_ID, chainID);
            outState.putString(SAVED_STATE_CHAIN_QUEUE_KEY, chainQueueKey);
            outState.putString(SAVED_STATE_TO_LEARN_LANGUAGE_CODE, toLearnLanguageCode);
            outState.putString(SAVED_STATE_TO_DISPLAY_LANGUAGE_CODE, toDisplayLanguageCode);
            //needed to update the audio path
            outState.putString(SAVED_STATE_FILE_NAME, fileName);
        }

        super.onSaveInstanceState(outState);
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
        progressBar.setVisibility(View.VISIBLE);
        DatabaseReference chainQueueRef = database.getReference(
                FirebaseDBHeaders.TO_LEARN_CHAIN_QUEUE + "/" +
                toLearnLanguageCode + "/" +
                situationID
        );

        //we want to order them by date time added and also
        //filter by whether it is available.
        //since FireBase doesn't allow multiple query filters,
        //we combine them into one filter
        Query chainQueueQuery = chainQueueRef.
                orderByChild(FirebaseDBHeaders.CHAIN_QUEUE_IN_QUEUE)
                .equalTo(ChainQueue.IN_QUEUE)
                .limitToFirst(1);
        dequeQuery(chainQueueQuery, 0);
    }

    //loops until we find a suitable chain
    private void dequeQuery(final Query query, final int loopCt){
        if (stopped)
            return;
        Log.d(TAG, "Loop "+loopCt);
        //max loop count
        if (loopCt == 10){
            Log.d("Learn_Start","too many loops");
            notifyNoChain();
            return;
        }

        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (dataSnapshot.getChildrenCount() == 0){
                    Log.d(TAG, "No children");
                    notifyNoChain();
                    return;
                }

                //only one item since we are limiting to one
                for (DataSnapshot snapshot : dataSnapshot.getChildren()){
                    //get key
                    final String key = snapshot.getKey();
                    final DatabaseReference transactionRef =
                            database.getReference(
                                    FirebaseDBHeaders.TO_LEARN_CHAIN_QUEUE + "/" +
                                    toLearnLanguageCode + "/" +
                                    situationID + "/" +
                                    key + "/" +
                                    FirebaseDBHeaders.CHAIN_QUEUE_IN_QUEUE
                            );

                    transactionRef.runTransaction(new Transaction.Handler() {
                        @Override
                        public Transaction.Result doTransaction(MutableData mutableData) {
                            long inQueue = mutableData.getValue(long.class);
                            if (inQueue == ChainQueue.WITH_USER){
                                //exit transaction and search again
                                return Transaction.abort();
                            } else if (inQueue == ChainQueue.IN_QUEUE){
                                //mark the chain queue indicating that the user has it
                                mutableData.setValue(ChainQueue.WITH_USER);
                                //save the key so if we end up returning the queue,
                                //we have a reference
                                chainQueueKey = key;
                                return Transaction.success(mutableData);
                            } else {
                                //shouldn't be called
                                return Transaction.abort();
                            }
                        }

                        @Override
                        public void onComplete(DatabaseError databaseError, boolean committed, DataSnapshot dataSnapshot) {
                            if (committed && chainQueueKey != null){
                                transactionRef.onDisconnect().setValue(ChainQueue.IN_QUEUE);
                                //do this after removing the credits because
                                // putChainBackToQueue adds the removed credits
                                if (stopped) {
                                    return;
                                }
                                linkChain(chainQueueKey);
                            }  else {
                                dequeQuery(query, loopCt+1);
                            }
                        }
                    });
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    private void notifyNoChain(){
        if (stopped)
            return;
        progressBar.setVisibility(View.GONE);
        FragmentManager fragmentManager = getChildFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        Fragment noChainFragment = new Learn_StartNoChain();
        Bundle bundle = new Bundle();
        bundle.putString(Learn_StartNoChain.LANGUAGE_TO_LEARN_BUNDLE, toLearnLanguageCode);
        bundle.putString(Learn_StartNoChain.LANGUAGE_TO_DISPLAY_BUNDLE, toDisplayLanguageCode);
        noChainFragment.setArguments(bundle);
        fragmentTransaction.replace(R.id.fragment_layout, noChainFragment, FRAGMENT_NO_CHAIN);
        fragmentTransaction.commit();
    }

    private void linkChain(String chainKey){
        DatabaseReference chainQueueRef = database.getReference(
                FirebaseDBHeaders.TO_LEARN_CHAIN_QUEUE + "/" +
                toLearnLanguageCode + "/" +
                situationID + "/" +
                chainKey
        );

        chainQueueRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if (stopped){
                    return;
                }
                ChainQueue queue = dataSnapshot.getValue(ChainQueue.class);
                chainID = queue.getChainID();
                fileName = queue.getAudioPath();
                final RecordingManager.OnSaveRecordingListener onSaveRecordingListener = new RecordingManager.OnSaveRecordingListener() {
                    @Override
                    public void onSaveRecording() {
                        addVoicePlayerFragment(RecordingManager.getInternalStorageFilePath(getContext(), fileName));
                        listener.setSituationID(situationID);
                    }
                };
                RecordingManager.saveRecordingToInternalStorage(getContext(), fileName, fileName, onSaveRecordingListener);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }
    @Override
    public void newSituation(String situationID){
        listener.learnStartToLearnStart(situationID);
    }

    private void addVoicePlayerFragment(String audioPath){
        progressBar.setVisibility(View.GONE);
        FragmentManager fragmentManager = getChildFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        Fragment voicePlayer = new VoicePlayer();
        Bundle bundle = new Bundle();
        bundle.putString(VoicePlayer.BUNDLE_AUDIO_FILE_PATH_KEY, audioPath);
        voicePlayer.setArguments(bundle);
        fragmentTransaction.add(R.id.fragment_layout, voicePlayer, FRAGMENT_VOICE_PLAYER);
        fragmentTransaction.commit();
    }

    private void addTimeExpiredFragment(){
        progressBar.setVisibility(View.GONE);
        FragmentManager fragmentManager = getChildFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        Fragment timeExpired = new TimeExpired();
        fragmentTransaction.replace(R.id.fragment_layout, timeExpired, FRAGMENT_TIME_EXPIRED);
        fragmentTransaction.commit();
    }

    private void addNoConnectionFragment(){
        FragmentManager fragmentManager = getChildFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        Fragment youAreOffline = new YouAreOffline();
        fragmentTransaction.replace(R.id.fragment_layout, youAreOffline, FRAGMENT_OFFLINE);
        fragmentTransaction.commit();
    }

    @Override
    public void continueToEnd(){
        //we want to pass the chain id to the next page
        listener.learnStartToLearnEnd(chainQueueKey, situationID, chainID);
        chainQueueKey = null;
    }

    //don't want to do this in onStop() because onStop() triggers when we switch
    //apps with this app still in the recent apps?
    @Override
    public void onStop(){
        //put the chain back into the queue
        // if the user exits without going to the next activity
        if (!getActivity().isChangingConfigurations()) {
            if (chainQueueKey != null) {
                database.getReference(
                        FirebaseDBHeaders.TO_LEARN_CHAIN_QUEUE + "/" +
                        toLearnLanguageCode + "/" +
                        situationID + "/" +
                        chainQueueKey + "/" +
                        FirebaseDBHeaders.CHAIN_QUEUE_IN_QUEUE
                ).onDisconnect().cancel();
                putChainBackToQueue();
            }
            //putChainBackToQueue();
            if (fileName != null){
                RecordingManager.removeRecordingFromInternalStorage(getContext(), fileName);
            }
        }
        stopped = true;
        super.onStop();
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        if (onlineRef != null && onlineListener != null){
            onlineRef.removeEventListener(onlineListener);
        }
    }

    private void putChainBackToQueue(){
        //cleanly finished activity
        if (chainQueueKey == null)
            return;

        DatabaseReference chainQueueRef = database.getReference(
                FirebaseDBHeaders.TO_LEARN_CHAIN_QUEUE + "/" +
                toLearnLanguageCode + "/" +
                situationID + "/" +
                chainQueueKey
        );
        ChainManager.putChainBackIntoQueue(chainQueueRef);
        chainQueueKey = null;
        listener.setSituationID(null);
    }

    @Override
    public void backToStart(){
        listener.learnStartToSituationList();
    }

    @Override
    public void toOfflineMode(){
        listener.toOfflineMode();
    }

}
