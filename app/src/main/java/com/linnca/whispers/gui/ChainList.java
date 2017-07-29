package com.linnca.whispers.gui;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import com.linnca.whispers.data.datawrappers.MinimalChain;
import com.linnca.whispers.R;

public class ChainList extends Fragment implements ChainListList.ChainListListListener, YouAreOffline.YouAreOfflineListener{
    private final String FRAGMENT_CHAIN_LIST_LIST = "chainListList";
    private final String FRAGMENT_OFFLINE = "offline";
    private DatabaseReference onlineRef;
    private ValueEventListener onlineListener;
    private ChainListListener listener;

    public interface ChainListListener {
        void chainListToChainInfo(MinimalChain minimalChain);
        void toOfflineMode();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState){
        return inflater.inflate(R.layout.fragment_wrapper, container, false);
    }

    @Override
    public void onStart(){
        super.onStart();
        getActivity().setTitle(R.string.toolbar_title_chain_list);
        onlineRef = FirebaseDatabase.getInstance().getReference(".info/connected");
        onlineListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                boolean connected = dataSnapshot.getValue(Boolean.class);
                if (connected) {
                    FragmentManager fragmentManager = getChildFragmentManager();
                    if (fragmentManager.findFragmentByTag(FRAGMENT_CHAIN_LIST_LIST) == null &&
                            fragmentManager.findFragmentByTag(FRAGMENT_OFFLINE) == null) {
                        setLayout();
                    }
                } else {
                    //!stopped prevents attempting to add the fragment when we press the
                    //home button
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
            listener = (ChainListListener) context;
        } catch (Exception e){
            throw new ClassCastException(context.toString() + " must implement listener");
        }
    }

    private void setLayout(){
        FragmentManager fragmentManager = getChildFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        Fragment chainListList = new ChainListList();
        fragmentTransaction.replace(R.id.fragment_layout, chainListList, FRAGMENT_CHAIN_LIST_LIST);
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
    public void chainListToChainInfo(MinimalChain minimalChain){
        listener.chainListToChainInfo(minimalChain);
    }

    @Override
    public void toOfflineMode(){listener.toOfflineMode();}

    @Override
    public void onStop(){
        super.onStop();
        if (onlineRef != null && onlineListener != null){
            onlineRef.removeEventListener(onlineListener);
        }
    }
}
