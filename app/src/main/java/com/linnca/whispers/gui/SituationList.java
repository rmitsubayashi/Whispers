package com.linnca.whispers.gui;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import com.linnca.whispers.R;

public class SituationList extends Fragment implements SituationListList.SituationListListListener,
        YouAreOffline.YouAreOfflineListener{
    private String displayLanguage;
    private ProgressBar progressBar;

    private DatabaseReference onlineRef;
    private ValueEventListener onlineListener;

    private SituationListListener listener;

    //so the info/connected listener reads false the first time you initialize the app.
    //we wait longer so we have time for FireBase to update the reference.
    //if not, no need to wait
    private boolean initialRun = true;
    private String SAVED_INSTANCE_STATE_CONNECTED_ONCE = "savedConnectedOnce";
    private boolean connectedOnce = false;

    public static String BUNDLE_DISPLAY_LANGUAGE = "displayLanguage";
    public static String BUNDLE_INITIAL_RUN = "initialRun";

    private final String FRAGMENT_SITUATION_LIST_LIST ="situationListList";
    private final String FRAGMENT_OFFLINE = "offline";

    public interface SituationListListener {
        void situationListToLearnStart(String situationID);
        void situationListToNoLinks();
        void toOfflineMode();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle bundle = getArguments();
        displayLanguage = bundle.getString(BUNDLE_DISPLAY_LANGUAGE);
        initialRun = bundle.getBoolean(BUNDLE_INITIAL_RUN, false);
        if (savedInstanceState != null){
            connectedOnce = savedInstanceState.getBoolean(SAVED_INSTANCE_STATE_CONNECTED_ONCE);
        }
    }

    @Override
    public void onStart(){
        super.onStart();
        getActivity().setTitle(R.string.toolbar_title_learn);
        setLayout();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState){
        View view = inflater.inflate(R.layout.fragment_wrapper, container, false);
        progressBar = (ProgressBar)view.findViewById(R.id.fragment_progress_bar);
        return view;
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
            listener = (SituationListListener) context;
        } catch (Exception e){
            throw new ClassCastException(context.toString() + " must implement listener");
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState){
        super.onSaveInstanceState(outState);
        outState.putBoolean(SAVED_INSTANCE_STATE_CONNECTED_ONCE, connectedOnce);
    }

    @Override
    public void showProgressBar(){
        progressBar.setVisibility(View.VISIBLE);
    }

    @Override
    public void hideProgressBar(){
        progressBar.setVisibility(View.GONE);
    }

    @Override
    public void situationListToLearnStart(String situationID){
        listener.situationListToLearnStart(situationID);
    }

    @Override
    public void situationListToNoLinks(){
        listener.situationListToNoLinks();
    }

    @Override
    public void toOfflineMode(){listener.toOfflineMode();}

    private void setLayout(){
        onlineRef = FirebaseDatabase.getInstance().getReference(".info/connected");
        onlineListener = new ValueEventListener(){
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                boolean connected = dataSnapshot.getValue(Boolean.class);
                FragmentManager fragmentManager = getChildFragmentManager();
                if (connected) {
                    if (fragmentManager.findFragmentByTag(FRAGMENT_SITUATION_LIST_LIST) == null &&
                            fragmentManager.findFragmentByTag(FRAGMENT_OFFLINE) == null) {
                        connectedOnce = true;
                        hideProgressBar();
                        addListLayout();
                    }
                } else {
                    if (initialRun && !connectedOnce) {
                        //handle when user changes orientation
                        if (fragmentManager.findFragmentByTag(FRAGMENT_OFFLINE) == null) {
                            showProgressBar();
                            //we wait a bit to let FireBase update the online listener the first time.
                            //if not, the listener will always return false on initial run.
                            Handler handler = new Handler();
                            handler.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    //make sure the listener hasn't updated while waiting or
                                    //the user leaves the screen
                                    if (!connectedOnce) {
                                        if (isAdded()) {
                                            hideProgressBar();
                                            addNoConnectionFragment();
                                        }
                                        //shouldn't remove listener if the user is connected
                                        //because he might disconnect again
                                        onlineRef.removeEventListener(onlineListener);
                                    }
                                }
                            }, 5000);
                        }
                    } else {
                        addNoConnectionFragment();
                        onlineRef.removeEventListener(onlineListener);
                    }
                    //onlineRef.removeEventListener(onlineListener);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        };

        onlineRef.addValueEventListener(onlineListener);
    }

    private void addListLayout(){
        FragmentManager fragmentManager = getChildFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        Fragment situationListList = new SituationListList();
        Bundle bundle = new Bundle();
        bundle.putString(SituationListList.BUNDLE_DISPLAY_LANGUAGE, displayLanguage);
        situationListList.setArguments(bundle);
        fragmentTransaction.replace(R.id.fragment_layout, situationListList, FRAGMENT_SITUATION_LIST_LIST);
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
    public void onStop(){
        super.onStop();
        if (onlineRef != null && onlineListener != null){
            onlineRef.removeEventListener(onlineListener);
        }
    }
}
