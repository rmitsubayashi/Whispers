package com.linnca.whispers.gui;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
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

import java.util.ArrayList;
import java.util.List;

import com.linnca.whispers.data.FirebaseDBHeaders;
import com.linnca.whispers.R;
import com.linnca.whispers.gui.widgets.SituationListAdapter;
import com.linnca.whispers.gui.widgets.SituationListItemWrapper;

public class SituationListList extends Fragment {
    private String displayLanguage;
    private FirebaseDatabase database;
    private RecyclerView listView;
    private SituationListAdapter listAdapter;
    private List<SituationListItemWrapper> listItems;

    private SituationListListListener listener;

    public static String BUNDLE_DISPLAY_LANGUAGE = "displayLanguage";

    public interface SituationListListListener {
        void situationListToLearnStart(String situationID);
        void situationListToNoLinks();
        void showProgressBar();
        void hideProgressBar();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        database = FirebaseDatabase.getInstance();

        Bundle bundle = getArguments();
        displayLanguage = bundle.getString(BUNDLE_DISPLAY_LANGUAGE);
    }

    @Override
    public void onStart(){
        super.onStart();
        getActivity().setTitle(R.string.toolbar_title_learn);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState){
        View view = inflater.inflate(R.layout.fragment_situation_list, container, false);
        if (displayLanguage == null){
            Log.d("SituationList","Language code is empty");
            return view;
        }
        implementListeners(getParentFragment());
        listView = (RecyclerView)view.findViewById(R.id.situation_list_list);
        listView.setLayoutManager(new LinearLayoutManager(getContext()));
        listItems = new ArrayList<>();
        String userID = FirebaseAuth.getInstance().getCurrentUser().getUid();
        listAdapter = new SituationListAdapter(listItems, listener, userID);
        listView.setAdapter(listAdapter);
        populateList();
        return view;
    }

    private void implementListeners(Fragment parentFragment){
        try {
            listener = (SituationListListListener) parentFragment;
        } catch (Exception e){
            throw new ClassCastException(parentFragment.toString() + " must implement listener");
        }
    }

    //this is expensive now.
    //flatten out/make it local later
    private void populateList(){
        showLoadingUI();
        DatabaseReference situationsReference = database.getReference(
                FirebaseDBHeaders.SITUATIONS
        );

        situationsReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (DataSnapshot child : dataSnapshot.getChildren()){
                    String situationID = child.getKey();
                    //we want the list to be in the user's native language
                    String title = (String)child.child(FirebaseDBHeaders.SITUATIONS_ID_TITLE)
                            .child(displayLanguage).getValue();
                    SituationListItemWrapper wrapper = new SituationListItemWrapper(
                            title, situationID
                    );
                    listItems.add(wrapper);
                }
                stopLoadingUI();
                listAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    private void showLoadingUI(){
        listener.showProgressBar();
    }

    private void stopLoadingUI(){
        listener.hideProgressBar();
    }
}
